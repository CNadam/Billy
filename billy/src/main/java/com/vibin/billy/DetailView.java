package com.vibin.billy;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class DetailView extends Activity {
    String song, artwork, artist, album, streamLink;
    int songIndex, songLength;
    boolean isMusicPlaying, mBound;
    Drawable playIcon, pauseIcon;
    View customActionView;
    ImageButton streamBtn;
    ImageView dashes;
    Intent serviceIntent;
    BillyApplication billyapp;
    Drawable mActionBarBackgroundDrawable;
    SystemBarTintManager tintManager;
    TextView actionBarText;
    Bundle itemData;
    ProcessingTask ft;
    ProgressBar progressBar;
    RotateAnimation rotateAnim;
    ScaleAnimation scaleAnim;
    NetworkImageView hero;
    Notification note;
    NotificationManager noteMan;
    Bitmap notifIcon;
    RequestQueue req;
    RemoteViews notifView;

    private static final String TAG = DetailView.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_view);

        billyapp = (BillyApplication) this.getApplication();

        itemData = getIntent().getExtras();
        song = itemData.getString("song");
        artist = itemData.getString("artist");
        album = itemData.getString("album");
        artwork = itemData.getString("artwork");
        songIndex = itemData.getInt("index");

        streamBtn = (ImageButton) findViewById(R.id.streamButton);
        dashes = (ImageView) findViewById(R.id.dashes);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        playIcon = getResources().getDrawable(R.drawable.play);
        pauseIcon = getResources().getDrawable(R.drawable.pause);
        setButtonListener();

        req = billyapp.getRequestQueue();
        final String scUrl = getResources().getString(R.string.soundcloud) + artist.replaceAll(" ", "+") + "+" + song.replaceAll(" ", "+") + getResources().getString(R.string.sc_params);
        Log.d(TAG, "scUrl is " + scUrl.substring(0, 100) + "...");
        StringRequest stringreq = new StringRequest(Request.Method.GET, scUrl, scComplete(), scError());
        req.add(stringreq);
        ft = new ProcessingTask();

        customActionBar();

        tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setTintColor(getResources().getColor(R.color.billyred));
        tintManager.setTintAlpha(0);

        ImageLoader imgload = billyapp.getImageLoader();
        hero = (NetworkImageView) findViewById(R.id.image_header);
        hero.setImageUrl(artwork, imgload);

        notifView = new RemoteViews(this.getPackageName(), R.layout.notification_view);
        noteMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        ((NotifyingScrollView) findViewById(R.id.scroll_view)).setOnScrollChangedListener(mOnScrollChangedListener);

        serviceIntent = new Intent(this, PlayerService.class);

        if (PlayerService.isRunning) {
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private Response.Listener<String> scComplete() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    streamLink = ft.parseSoundcloud(response);
                    Log.d(TAG, "streamLink is " + streamLink);
                    animate();
                    streamBtn.startAnimation(scaleAnim);
                    streamBtn.setVisibility(View.VISIBLE);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private Response.ErrorListener scError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d(TAG, volleyError.toString());
            }
        };
    }

    /**
     * Bind this and PlayerService
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            PlayerService.PlayerServiceBinder binder = (PlayerService.PlayerServiceBinder) service;
            PlayerService mService = binder.getService();

            binder.setListener(new PlayerService.onBPChangedListener() {
                @Override
                public void onPrepared(int duration) {
                    dashes.clearAnimation();
                    putNotification(); //TODO
                    dashes.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setBackgroundColor(Color.TRANSPARENT);
                    progressBar.setMax(duration);
                    songLength = duration;
                    progress();
                    streamBtn.setImageDrawable(pauseIcon);
                }

                // Only onCompletion and onError are called if stream-url is wrong (404, etc.)
                @Override
                public void onCompletion() {
                    noteMan.cancelAll();
                    streamBtn.setImageDrawable(playIcon);
                    progressBar.setVisibility(View.GONE);
                    isMusicPlaying = false;
                }

                @Override
                public void onError(int i, int i2) {
                    Toast.makeText(getBaseContext(), "Something's wrong. "+i+" "+i2,
                            Toast.LENGTH_LONG).show();
                    dashes.clearAnimation();
                    dashes.setVisibility(View.GONE);
                }
            });
            mBound = true;
        }
    };

    private void setButtonListener() {
        streamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isMusicPlaying) {
                    //TODO check if streamLink is not a 404
                    if (streamLink != null) {
                        isMusicPlaying = true;
                        dashes.setVisibility(View.VISIBLE);
                        dashes.startAnimation(rotateAnim);
                        serviceIntent.putExtra("streamLink", streamLink);
                        serviceIntent.putExtra("songName", song);
                        serviceIntent.putExtra("songIndex", songIndex);
                        serviceIntent.putExtra("albumName", album);
                        startService(serviceIntent);
                        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
                    } else {
                        Log.d(TAG, "streamLink is null");
                        Toast.makeText(getBaseContext(), "The song cannot be streamed.",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    streamBtn.setImageDrawable(playIcon);
                    dashes.clearAnimation();
                    dashes.setVisibility(View.INVISIBLE);
                    unbindService(mConnection);
                    stopService(serviceIntent);
                    isMusicPlaying = false;
                    mBound = false;
                    noteMan.cancelAll();
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * Run the progressbar in a separate thread
     */
    void progress() {
        Thread progressThread = new Thread() {
            public void run() {
                int prog = 0;
                while (prog < songLength) {
                    if (isMusicPlaying) {
                        progressBar.setProgress(prog);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        prog++;
                    } else
                        return;
                }
            }
        };
        progressThread.start();
    }

    /**
     * Animate the Play button and dashes
     */
    private void animate() {
        rotateAnim = new RotateAnimation(0.0f, 360.0f, dashes.getWidth() / 2, dashes.getHeight() / 2);
        rotateAnim.setDuration(6000);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.setRepeatMode(Animation.INFINITE);
        rotateAnim.setRepeatCount(10);

        scaleAnim = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, streamBtn.getWidth() / 2, streamBtn.getHeight() / 2);
        scaleAnim.setDuration(800);
        scaleAnim.setInterpolator(new BounceInterpolator());
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
        noteMan.notify(1, note);
    }

    private NotifyingScrollView.OnScrollChangedListener mOnScrollChangedListener = new NotifyingScrollView.OnScrollChangedListener() {
        public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
            final int headerHeight = findViewById(R.id.image_header).getHeight() - getActionBar().getHeight() - 500;
            final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
            final int newAlpha = (int) (ratio * 255);
            mActionBarBackgroundDrawable.setAlpha(newAlpha);
            tintManager.setTintAlpha(ratio);
            actionBarText.setAlpha(ratio);
        }
    };

    private void customActionBar() {
        mActionBarBackgroundDrawable = getResources().getDrawable(R.drawable.ab_solid_billy);
        mActionBarBackgroundDrawable.setAlpha(1);
        getActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowCustomEnabled(true);

        customActionView = getLayoutInflater().inflate(R.layout.custom_actionbar, null);
        actionBarText = (TextView) customActionView.findViewById(R.id.title);
        getActionBar().setCustomView(customActionView);
        actionBarText.setAlpha(0);
        setTitle(song);
    }

    @Override
    public void setTitle(CharSequence title) {
        getActionBar().setTitle("");
        ((TextView) customActionView.findViewById(R.id.title)).setText(title);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // Handle the Up button in Actionbar
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
        }
    }
}
