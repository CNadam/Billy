package com.vibin.billy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.MediaList;


public class PPlayerService extends Service implements Handler.Callback {
    private static final String TAG = PPlayerService.class.getSimpleName();
    String streamLink, song, album, artist, artwork;
    LibVLC bp;
    MediaList list;
    int songIndex;
    int bufferPercent;
    Notification note;
    NotificationManager noteMan;
    Bitmap notifIcon;
    RequestQueue req;
    RemoteViews notifView;
    BillyApplication billyapp;
    PhoneStateListener PSlistener;
    TelephonyManager telephony;
    private onBPChangedListener BPlistener;
    private final IBinder mBinder = new PPlayerServiceBinder();
    public static boolean isRunning;
    public static boolean isInCall, isInCallMusicPaused;
    public static boolean isIdle = true;
    boolean onlyOnce = true;

    @Override
    public boolean handleMessage(Message msg) {
        int event = msg.getData().getInt("event");
        switch (event) {
            case EventHandler.MediaPlayerPlaying:
                //Log.d(TAG, "started playing" +bundle2string(msg.getData()));
                break;
            case EventHandler.MediaPlayerEndReached:
                Log.d(TAG, "end reached" +bundle2string(msg.getData()));
                onCompletion();
                break;
            case EventHandler.MediaPlayerEncounteredError:
                Log.d(TAG,"Mediaplayer error occured");
                onError();
                break;
            case EventHandler.MediaPlayerPositionChanged:
                //Log.d(TAG, "position" +bundle2string(msg.getData()));
                onPrepared();
                break;
            case EventHandler.MediaPlayerBuffering:
                onBufferChange(msg.getData().getFloat("data"));
                break;
        }
        return true;
    }

    private void onBufferChange(float percent) {
            bufferPercent = (int) percent;
    }

    public static String bundle2string(Bundle bundle) {
        String string = "Bundle{";
        for (String key : bundle.keySet()) {
            string += " " + key + " => " + bundle.get(key) + ";";
        }
        string += " }Bundle";
        return string;
    }

    public interface onBPChangedListener {
        void onPrepared(int duration);

        void onCompletion();

        void onError();

        void onStop();

        void onNotificationPausePressed();

        void onNotificationPlayPressed();

        void onNotificationStopPressed();
    }

    /**
     * Returns this instance of LocalService so clients can call public methods
     */
    public class PPlayerServiceBinder extends Binder {
        public PPlayerService getService() {
            return PPlayerService.this;
        }

        public void setListener(onBPChangedListener listener) {
            BPlistener = listener;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setCustomExceptionHandler();

        try {
            bp = LibVLC.getInstance();
            bp.init(getBaseContext());
        } catch (LibVlcException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Service's onCreate");
        billyapp = (BillyApplication) this.getApplication();

        isRunning = true;
        notifView = new RemoteViews(this.getPackageName(), R.layout.notification_view);
        noteMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        req = billyapp.getRequestQueue();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            setupPhoneListener();
            streamLink = intent.getStringExtra("streamLink");
            song = intent.getStringExtra("songName");
            album = intent.getStringExtra("albumName");
            artist = intent.getStringExtra("artistName");
            songIndex = intent.getIntExtra("songIndex", 50);
            artwork = intent.getStringExtra("artwork");

            //bp.reset();

//            if (!bp.isPlaying()) {
                //bp.setDataSource(getBaseContext(), Uri.parse(streamLink));

/*
                    URLConnection con = new URL(streamLink).openConnection();
                    con.connect();
                    InputStream is = con.getInputStream();
                    streamLink = con.getURL().toString();
                    is.close();
*/
//                  streamLink="http://ec-media.soundcloud.com/p7Uw60gtODDZ.128.mp3?f10880d39085a94a0418a7ef69b03d522cd6dfee9399eeb9a522009e6bf9b93b1c57cf78a3e4865f4b47b62a40b86e0209984a23f40484b51853d2c1e8a7dc8285658d0e8e&AWSAccessKeyId=AKIAJNIGGLK7XA7YZSNQ&Expires=1412417762&Signature=Oz5cUbgp06dzFOy3aTBm0txXSIY%3D";
                //list = bp.getPrimaryMediaList();
                //list.clear();

                //list.add(LibVLC.PathToURI(streamLink));
                Log.d(TAG, "service " + streamLink);

                EventHandler eh = EventHandler.getInstance();
                Handler handler = new Handler(this);
                eh.addHandler(handler);

                bp.setNetworkCaching(1);
                //bp.playIndex(0);
                onlyOnce = true;
                bp.playMRL(LibVLC.PathToURI(streamLink));
                //putNotification();
//            }
        }
        return START_STICKY;
    }

    private void setupPhoneListener() {
        telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        PSlistener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                switch (state) {
                    /**
                     * Lifting or cancelling a call is equivalent to pressing pause/play Notification buttons
                     */
                    case TelephonyManager.CALL_STATE_RINGING:
                        isInCall = true;
                        if (bp != null && !isIdle) {
                            try {
                                if (bp.isPlaying()) {
                                    isInCallMusicPaused = true;
                                    bp.pause();
                                    putNotification();
                                    BPlistener.onNotificationPausePressed();
                                }
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (isInCall) {
                            isInCall = false;
                            if (isInCallMusicPaused) {
                                isInCallMusicPaused = false;
                                if (bp != null) {
                                    bp.play();
                                    putNotification();
                                    BPlistener.onNotificationPlayPressed();
                                }
                            }
                        }
                }

            }
        };
        telephony.listen(PSlistener, PhoneStateListener.LISTEN_CALL_STATE);
    }


    public void onPrepared() {
        if(onlyOnce) {
            putNotification();
            Log.d(TAG,"Mediaplayer has prepared");
            isIdle = false;
            onlyOnce = false;
            BPlistener.onPrepared((int) bp.getLength() / 1000); //ms
        }
    }

    public void doPlay() {
        if (!bp.isPlaying()) {
            Log.d(TAG, "play media");
            //onlyOnce = true;
            //bp.playIndex(0);
            bp.play();
            putNotification();
        }
    }

    public void doPause() {
        if(bp.isPlaying()) {
            bp.pause();
            putNotification();
        }
    }

    public void onError() {
        stopForeground(true);
        BPlistener.onError();
    }

    public void onCompletion() {
        Log.d(TAG, "Service onCompletion");
        BPlistener.onCompletion();
        stopForeground(true);
        try {
            unregisterReceiver(NotificationMediaControl);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        stopMedia();
    }

    public void stopMedia() {
  //      if (bp.isPlaying()) {
            bp.stop();
            BPlistener.onStop();
  //      }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        if (bp != null) {
            if (bp.isPlaying()) {
                bp.stop();
            }
            isIdle = true;
            bp.destroy();
        }
        stopForeground(true);
        try {
            unregisterReceiver(NotificationMediaControl);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        if (PSlistener != null) {
            telephony.listen(PSlistener, PhoneStateListener.LISTEN_NONE);
        }
        isRunning = false;
    }

    /**
     * Put a sticky notification for media controls
     */
    private void putNotification() {
        Intent resultIntent = new Intent(this, DetailView.class);
        resultIntent.putExtra("song", song);
        resultIntent.putExtra("album", album);
        resultIntent.putExtra("artist", artist);
        resultIntent.putExtra("artwork", artwork);
        resultIntent.putExtra("index", songIndex);
        //resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        note = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setContentIntent(resultPendingIntent)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle("Billy").build();
        note.bigContentView = notifView;
        notifView.setTextViewText(R.id.song, song);
        notifView.setTextViewText(R.id.artist, artist);
        notifView.setTextViewText(R.id.album, album);

        billyapp.getImageLoader().get(artwork, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                notifIcon = imageContainer.getBitmap();
                notifView.setImageViewBitmap(R.id.artwork, notifIcon);
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d(TAG, volleyError.toString());
            }
        });

        Intent playIntent = new Intent("com.vibin.billy.ACTION_PLAY");
        Intent pauseIntent = new Intent("com.vibin.billy.ACTION_PAUSE");
        Intent quitIntent = new Intent("com.vibin.billy.ACTION_QUIT");
        PendingIntent pendingQuitIntent = PendingIntent.getBroadcast(getBaseContext(), 100, quitIntent, 0);
        notifView.setOnClickPendingIntent(R.id.dismiss, pendingQuitIntent);
        if (bp.isPlaying()) {
            notifView.setImageViewResource(R.id.control, R.drawable.notification_pause);
            PendingIntent pendingPauseIntent = PendingIntent.getBroadcast(getBaseContext(), 100, pauseIntent, 0);
            notifView.setOnClickPendingIntent(R.id.control, pendingPauseIntent);
        } else {
            notifView.setImageViewResource(R.id.control, R.drawable.notification_play);
            PendingIntent pendingPlayIntent = PendingIntent.getBroadcast(getBaseContext(), 100, playIntent, 0);
            notifView.setOnClickPendingIntent(R.id.control, pendingPlayIntent);
        }
        startForeground(1, note);

        IntentFilter filter = new IntentFilter();
        filter.addAction(playIntent.getAction());
        filter.addAction(pauseIntent.getAction());
        filter.addAction(quitIntent.getAction());
        registerReceiver(NotificationMediaControl, filter);
    }

    private final BroadcastReceiver NotificationMediaControl = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equalsIgnoreCase("com.vibin.billy.ACTION_PAUSE")) {
                BPlistener.onNotificationPausePressed();
                doPause();
            }
            if (action.equalsIgnoreCase("com.vibin.billy.ACTION_PLAY")) {
                BPlistener.onNotificationPlayPressed();
                doPlay();
            }
            if (action.equalsIgnoreCase("com.vibin.billy.ACTION_QUIT")) {
                BPlistener.onNotificationStopPressed();
                stopMedia();
                stopSelf();
            }
        }
    };

    /**
     * Register a custom UncaughtException Handler for dismissing persistent notification on foreground Service's crash
     * Do call the default Android UncaughtException Handler at last, to get the dialog
     */
    private void setCustomExceptionHandler() {
        final Thread.UncaughtExceptionHandler defaultExHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler customExHandler = new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                stopForeground(true);

                defaultExHandler.uncaughtException(thread, throwable);
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(customExHandler);
    }
}