package com.vibin.billy.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.vibin.billy.BillyApplication;
import com.vibin.billy.BillyItem;
import com.vibin.billy.R;
import com.vibin.billy.activity.DetailView;
import com.vibin.billy.reciever.MediaControl;

import java.io.IOException;

public class PlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener {
    private static final String TAG = PlayerService.class.getSimpleName();
    public MediaPlayer bp = new MediaPlayer();
    public String streamLink, song, album, artist, artwork;
    int songIndex;
    public int bufferPercent;
    Notification note;
    Bitmap notifIcon;
    RequestQueue req;
    BillyApplication billyapp;
    PhoneStateListener PSlistener;
    TelephonyManager telephony;
    private onBPChangedListener BPlistener;
    private final IBinder mBinder = new PlayerServiceBinder();
    private static boolean isInCall, isInCallMusicPaused;
    public static boolean isIdle = true;
    public static boolean isRunning;

    public interface onBPChangedListener {
        void onPrepared(int duration);

        void onCompletion();

        void onError(int i, int i2);

        void onMediaStop();

        void onMediaPlay();

        void onMediaPause();

        void onNotificationStopPressed();
    }

    /**
     * Returns this instance of LocalService so clients can call public methods
     */
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

        Log.d(TAG, "onCreate");
        billyapp = (BillyApplication) this.getApplication();

        isRunning = true;
        setCustomExceptionHandler();
        ((AudioManager)getSystemService(AUDIO_SERVICE)).registerMediaButtonEventReceiver(new ComponentName(this, MediaControl.class));
        req = billyapp.getRequestQueue();

        bp.setOnPreparedListener(this);
        bp.setOnBufferingUpdateListener(this);
        bp.setOnCompletionListener(this);
        bp.setOnErrorListener(this);
        bp.setOnInfoListener(this);
        bp.setOnSeekCompleteListener(this);
        bp.reset();

        notifIcon = Bitmap.createBitmap(600,600, Bitmap.Config.ARGB_8888);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getStringExtra("id") != null) {
                Log.d(TAG, "recieving media button intent");
                KeyEvent keyEvent = (KeyEvent) intent.getParcelableExtra("keyevent");
                handleKeyDown(keyEvent);
            } else {
                setupPhoneListener();
                BillyItem b = intent.getParcelableExtra("item");
                streamLink = b.getStreamLink();
                song = b.getSong();
                album = b.getAlbum();
                artist = b.getArtist();
                songIndex = b.getIndex();
                artwork = b.getArtwork();

                bp.reset();

                if (!bp.isPlaying()) {
                    try {
                        bp.setDataSource(getBaseContext(), Uri.parse(streamLink));
                        bp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        //bp.setDataSource(streamLink);

                        bp.prepareAsync();
                    } catch (IOException e) {
                        Log.d(TAG, e.toString());
                    }
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
                            try {
                                if (bp.isPlaying()) {
                                    isInCallMusicPaused = true;
                                    pauseMedia();
                                }
                            } catch (IllegalStateException e) {
                                Log.d(TAG, e.toString());
                            }
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (isInCall) {
                            isInCall = false;
                            if (isInCallMusicPaused) {
                                isInCallMusicPaused = false;
                                if (bp != null) {
                                    playMedia();
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
        try {
            isIdle = false;
            BPlistener.onPrepared(mediaPlayer.getDuration() / 1000); //ms
            playMedia();
        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
        }
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        bufferPercent = i;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onCompletion");
        if(BPlistener != null) {
            BPlistener.onCompletion();
        }
        stopForeground(true);
        try {
            unregisterReceiver(NotificationMediaControl);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.toString());
        }
        stopMedia();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        stopForeground(true);
        if(BPlistener != null) {
            BPlistener.onError(i, i2);
        }
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
        Log.d(TAG, "onDestroy");
        if (bp != null) {
            if (bp.isPlaying()) {
                bp.stop();
            }
            isIdle = true;
            bp.reset();
            bp.release();
        }
        stopForeground(true);
        try {
            unregisterReceiver(NotificationMediaControl);
            ((AudioManager)getSystemService(AUDIO_SERVICE)).unregisterMediaButtonEventReceiver(new ComponentName(this, MediaControl.class));
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.toString());
        }
        if (PSlistener != null) {
            telephony.listen(PSlistener, PhoneStateListener.LISTEN_NONE);
        }
        isRunning = false;
    }

    public boolean playMedia() {
        if (!bp.isPlaying()) {
            bp.start();
            putNotification();
            if(BPlistener != null) {
                BPlistener.onMediaPlay();
            }
            return true;
        }
        return false;
    }

    public boolean pauseMedia() {
        if (bp.isPlaying()) {
            bp.pause();
            putNotification();
            if(BPlistener != null) {
                BPlistener.onMediaPause();
            }
            return true;
        }
        return false;
    }

    private void toggleMedia() {
        if (!playMedia()) {
            pauseMedia();
        }
    }

    public void stopMedia() {
        if (bp.isPlaying()) {
            Log.d(TAG, "media has successfully stopped");
            bp.stop();
            if(BPlistener != null) {
                BPlistener.onMediaStop();
            }
        }
    }

    /**
     * Put a sticky notification for media controls
     */

    @TargetApi(Build.VERSION_CODES.L)
    private void putNotification() {
        final RemoteViews notifView = new RemoteViews(this.getPackageName(), R.layout.notification_view);
        try {
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
        } catch (NullPointerException e) {
            Log.d(TAG,e.toString());
        }

        Intent resultIntent = new Intent(this, DetailView.class);
        resultIntent.putExtra("song", song);
        resultIntent.putExtra("album", album);
        resultIntent.putExtra("artist", artist);
        resultIntent.putExtra("artwork", artwork);
        resultIntent.putExtra("index", songIndex);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        note = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setLargeIcon(notifIcon)
                .setContentIntent(resultPendingIntent)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle(song)
                .setContentText(artist).build();
        note.bigContentView = notifView;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            note.category = Notification.CATEGORY_TRANSPORT;
            note.audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            note.visibility = Notification.VISIBILITY_PUBLIC;
            notifView.setTextColor(R.id.song,getResources().getColor(R.color.nTitle));
            notifView.setTextColor(R.id.artist,getResources().getColor(R.color.nDesc));
            notifView.setTextColor(R.id.album,getResources().getColor(R.color.nDesc));
            //notifView.setInt(R.id.notifDivider,"setBackgroundColor",getResources().getColor(R.color.nTitle));
        }

        notifView.setTextViewText(R.id.song, song);
        notifView.setTextViewText(R.id.artist, artist);
        notifView.setTextViewText(R.id.album, album);

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

    /**
     * @return false if key is not handled
     */

    private boolean handleKeyDown(KeyEvent keyEvent)
    {
        if (keyEvent.getAction()!=KeyEvent.ACTION_DOWN) {
            int keyCode = keyEvent.getKeyCode();
            Log.d(TAG,"Keycode is "+keyCode);

            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    toggleMedia();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    playMedia();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    pauseMedia();
                    return true;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                    toggleMedia();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                case KeyEvent.KEYCODE_MEDIA_STOP:
            }
        }
        return false;
    }

    private final BroadcastReceiver NotificationMediaControl = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equalsIgnoreCase("com.vibin.billy.ACTION_PAUSE")) {
                pauseMedia();
            }
            if (action.equalsIgnoreCase("com.vibin.billy.ACTION_PLAY")) {
                playMedia();
            }
            if (action.equalsIgnoreCase("com.vibin.billy.ACTION_QUIT")) {
                if (bp.isPlaying()) {
                    bp.stop();
                }
                if(BPlistener != null) {
                    BPlistener.onNotificationStopPressed();
                }
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
                Log.d(TAG,"Uncaught exception handled");
                stopForeground(true);

                defaultExHandler.uncaughtException(thread, throwable);
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(customExHandler);
    }
}