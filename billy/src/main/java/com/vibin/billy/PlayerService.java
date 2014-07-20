package com.vibin.billy;

import android.app.Service;
import android.content.Intent;
import android.drm.DrmManagerClient;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class PlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener,MediaPlayer.OnPreparedListener,MediaPlayer.OnInfoListener {
    private static final String TAG = "Player".getClass().getSimpleName();
    private MediaPlayer bp = new MediaPlayer();
    String streamLink, songName, albumName;
    int songIndex;
    private onBPChangedListener BPlistener;
    private final IBinder mBinder = new PlayerServiceBinder();
    public static boolean isRunning;

    public interface onBPChangedListener{
        void onPrepared(int duration);
        void onCompletion();
        void onError(int i, int i2);
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

    //TODO implement pause button
    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        bp.setOnPreparedListener(this);
        bp.setOnBufferingUpdateListener(this);
        bp.setOnCompletionListener(this);
        bp.setOnErrorListener(this);
        bp.setOnInfoListener(this);
        bp.reset();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        streamLink = intent.getStringExtra("streamLink");
        songName = intent.getStringExtra("songName");
        albumName = intent.getStringExtra("album");
        songIndex = intent.getIntExtra("songIndex",50);

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
        BPlistener.onPrepared(bp.getDuration()/1000);
        playMedia();
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        BPlistener.onCompletion();
        stopMedia();
        stopSelf();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
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
            bp.release();
        }
    }

    private void playMedia() {
        if(!bp.isPlaying()){
            bp.start();
        }
    }


    private void stopMedia() {
        if(bp.isPlaying())
        {
            bp.stop();
        }
    }
}