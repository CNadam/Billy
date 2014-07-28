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
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;

import java.io.IOException;

public class PlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener,MediaPlayer.OnPreparedListener,MediaPlayer.OnInfoListener {
    private static final String TAG = "Player".getClass().getSimpleName();
    MediaPlayer bp = new MediaPlayer();
    String streamLink, song, album, artist, artwork;
    int songIndex;
    Notification note;
    NotificationManager noteMan;
    Bitmap notifIcon;
    RequestQueue req;
    RemoteViews notifView;
    BillyApplication billyapp;
    private onBPChangedListener BPlistener;
    private final IBinder mBinder = new PlayerServiceBinder();
    public static boolean isRunning;
    public static boolean isIdle = true;

    public int getCurrentPosition() {
        return bp.getCurrentPosition();
    }

    public void doPause() {
        bp.pause();
    }

    public interface onBPChangedListener{
        void onPrepared(int duration);
        void onCompletion();
        void onError(int i, int i2);
        void onStop();
        void onNotificationPausePressed();
        void onNotificationPlayPressed();
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
        bp.reset();

        Log.d(TAG,"Service's oncreate");
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        streamLink = intent.getStringExtra("streamLink");
        song = intent.getStringExtra("songName");
        album = intent.getStringExtra("albumName");
        artist = intent.getStringExtra("artistName");
        songIndex = intent.getIntExtra("songIndex",50);
        artwork = intent.getStringExtra("artwork");

        bp.reset();

        if (!bp.isPlaying()) {
            try {
                bp.setDataSource(streamLink);

                bp.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        isIdle = false;
        BPlistener.onPrepared(bp.getDuration()/1000);
        playMedia();
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        BPlistener.onCompletion();
        stopForeground(true);
        try {
            unregisterReceiver(NotificationMediaControl);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        stopMedia();
        //stopSelf();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        stopForeground(true);
        //unregisterReceiver(NotificationMediaControl);
        BPlistener.onError(i,i2);
        switch(i){
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d(TAG,"MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK "+ i2);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d(TAG,"MEDIA_ERROR_SERVER_DIED "+i2);
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                Log.d(TAG,"MEDIA_ERROR_IO "+i2);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d(TAG,"MEDIA_ERROR_UNKNOWN "+i2);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (bp != null) {
            if (bp.isPlaying()){
                bp.stop();
            }
            bp.reset();
            isIdle = true;
            bp.release();
        }
        unregisterReceiver(NotificationMediaControl);
    }

    public void playMedia() {
        if(!bp.isPlaying()){
            bp.start();
            putNotification();
        }
    }


    public void stopMedia() {
        if(bp.isPlaying())
        {
            bp.stop();
            BPlistener.onStop();
        }
    }

    //TODO

    /**
     * Put a sticky notification for media controls
     */
    private void putNotification() {
        note = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setContentTitle("Custom View").build();
        note.bigContentView = notifView;
        notifView.setTextViewText(R.id.song, song);
        notifView.setTextViewText(R.id.artist, artist);
        notifView.setTextViewText(R.id.album, album);

        //TODO check if this is actually fetching
        if (req.getCache().get(artwork) == null) {
            ImageRequest imagereq = new ImageRequest(artwork,new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap bitmap) {
                    notifIcon = bitmap;
                }
            },400,400,null,null);
            req.add(imagereq);
        } else {
            notifIcon = BitmapFactory.decodeByteArray(req.getCache().get(artwork).data, 0, req.getCache().get(artwork).data.length);
        }
        notifView.setImageViewBitmap(R.id.artwork, notifIcon);
        Intent pauseIntent = new Intent("com.vibin.billy.ACTION_PAUSE");
        Intent quitIntent = new Intent("com.vibin.billy.ACTION_QUIT");
        PendingIntent pendingPauseIntent = PendingIntent.getBroadcast(getBaseContext(),100,pauseIntent,0);
        PendingIntent pendingQuitIntent = PendingIntent.getBroadcast(getBaseContext(),100,quitIntent,0);
        notifView.setOnClickPendingIntent(R.id.control,pendingPauseIntent);
        notifView.setOnClickPendingIntent(R.id.dismiss,pendingQuitIntent);
        startForeground(1, note);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.vibin.billy.ACTION_PAUSE");
        filter.addAction("com.vibin.billy.ACTION_PLAY");
        filter.addAction("com.vibin.billy.ACTION_QUIT");
        registerReceiver(NotificationMediaControl,filter);
    }

    private final BroadcastReceiver NotificationMediaControl = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equalsIgnoreCase("com.vibin.billy.ACTION_PAUSE")){
                bp.pause();
                BPlistener.onNotificationPausePressed();
                notifView.setInt(R.id.control, "setImageResource", R.drawable.notification_play);
                Intent playIntent = new Intent("com.vibin.billy.ACTION_PLAY");
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(),100,playIntent,0);
                notifView.setOnClickPendingIntent(R.id.control,pendingIntent);
            }

            if(action.equalsIgnoreCase("com.vibin.billy.ACTION_PLAY")){
                bp.start();
                BPlistener.onNotificationPlayPressed();
                notifView.setInt(R.id.control, "setImageResource", R.drawable.notification_pause);
                Intent pauseIntent = new Intent("com.vibin.billy.ACTION_PAUSE");
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(),100,pauseIntent,0);
                notifView.setOnClickPendingIntent(R.id.control,pendingIntent);
            }
            if(action.equalsIgnoreCase("com.vibin.billy.ACTION_QUIT")){
                stopForeground(true);
                stopMedia();
                stopSelf();
            }
        }
    };

}