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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import java.io.IOException;

public class PlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener {
    private static final String TAG = "Player".getClass().getSimpleName();
    MediaPlayer bp = new MediaPlayer();
    String streamLink, song, album, artist, artwork;
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
    private final IBinder mBinder = new PlayerServiceBinder();
    public static boolean isRunning;
    public static boolean isInCall, isInCallMusicPaused;
    public static boolean isIdle = true;

    public void doPause() {
        bp.pause();
        putNotification();
    }

    public interface onBPChangedListener {
        void onPrepared(int duration);

        void onCompletion();

        void onError(int i, int i2);

        void onStop();

        void onNotificationPausePressed();

        void onNotificationPlayPressed();

        void onNotificationStopPressed();
    }

    // Return this instance of LocalService so clients can call public methods
    public class PlayerServiceBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
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

        billyapp = (BillyApplication) this.getApplication();

        isRunning = true;
        notifView = new RemoteViews(this.getPackageName(), R.layout.notification_view);
        noteMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        req = billyapp.getRequestQueue();

        bp.setOnPreparedListener(this);
        bp.setOnBufferingUpdateListener(this);
        bp.setOnCompletionListener(this);
        bp.setOnErrorListener(this);
        bp.setOnInfoListener(this);
        bp.setOnSeekCompleteListener(this);
        bp.reset();

        Log.d(TAG, "Service's oncreate");
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            setupPhoneListener();
            streamLink = intent.getStringExtra("streamLink");
            song = intent.getStringExtra("songName");
            album = intent.getStringExtra("albumName");
            artist = intent.getStringExtra("artistName");
            songIndex = intent.getIntExtra("songIndex", 50);
            artwork = intent.getStringExtra("artwork");

            bp.reset();

            if (!bp.isPlaying()) {
                try {
                    bp.setDataSource(getBaseContext(), Uri.parse(streamLink));
                    //bp.setDataSource(streamLink);

                    bp.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
                            if(bp.isPlaying()) {
                                isInCallMusicPaused = true;
                                bp.pause();
                                putNotification();
                                BPlistener.onNotificationPausePressed();
                            }
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if(isInCall) {
                            isInCall = false;
                            if(isInCallMusicPaused) {
                                isInCallMusicPaused = false;
                                if (bp != null) {
                                    bp.start();
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

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        isIdle = false;
        BPlistener.onPrepared(mediaPlayer.getDuration() / 1000);
        playMedia();
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        bufferPercent = i;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, "Service oncompletion");
        BPlistener.onCompletion();
        stopForeground(true);
        try {
            unregisterReceiver(NotificationMediaControl);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        stopMedia();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        stopForeground(true);
        //unregisterReceiver(NotificationMediaControl);
        BPlistener.onError(i, i2);
        switch (i) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d(TAG, "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK " + i2);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d(TAG, "MEDIA_ERROR_SERVER_DIED " + i2);
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                Log.d(TAG, "MEDIA_ERROR_IO " + i2);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d(TAG, "MEDIA_ERROR_UNKNOWN " + i2);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service ondestroy");
        if (bp != null) {
            if (bp.isPlaying()) {
                bp.stop();
            }
            bp.reset();
            isIdle = true;
            bp.release();
        }
        stopForeground(true);
        try {
            unregisterReceiver(NotificationMediaControl);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        if(PSlistener != null) {
            telephony.listen(PSlistener, PhoneStateListener.LISTEN_NONE);
        }
        isRunning = false;
    }

    public void playMedia() {
        if (!bp.isPlaying()) {
            bp.start();
            putNotification();
        }
    }


    public void stopMedia() {
        if (bp.isPlaying()) {
            bp.stop();
            BPlistener.onStop();
        }
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
        filter.addAction("com.vibin.billy.ACTION_PAUSE");
        filter.addAction("com.vibin.billy.ACTION_PLAY");
        filter.addAction("com.vibin.billy.ACTION_QUIT");
        registerReceiver(NotificationMediaControl, filter);
    }

    private final BroadcastReceiver NotificationMediaControl = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equalsIgnoreCase("com.vibin.billy.ACTION_PAUSE")) {
                bp.pause();
                BPlistener.onNotificationPausePressed();
                putNotification();
            }

            if (action.equalsIgnoreCase("com.vibin.billy.ACTION_PLAY")) {
                bp.start();
                BPlistener.onNotificationPlayPressed();
                putNotification();
            }
            if (action.equalsIgnoreCase("com.vibin.billy.ACTION_QUIT")) {
                stopForeground(true);
                stopMedia();
                BPlistener.onNotificationStopPressed();
                stopSelf();
            }
        }
    };

    public void uncaughtException() {
        try {
            onDestroy();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}