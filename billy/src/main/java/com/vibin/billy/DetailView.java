package com.vibin.billy;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class DetailView extends Activity {
    String song, artwork, artist, scUrl, streamLink;
    int songIndex, songLength;
    boolean isMusicPlaying, mBound;
    Drawable playIcon, pauseIcon;
    View customActionView;
    ImageButton streamBtn;
    ImageView dashes;
    Intent serviceIntent;
    BillyApplication billyapp;
    ImageLoader imgload;
    NetworkImageView hero;
    Drawable mActionBarBackgroundDrawable;
    SystemBarTintManager tintManager;
    TextView actionBarText;
    Bundle itemData;
    CharSequence mTitle;
    RequestQueue req;
    ProcessingTask ft;
    PlayerService mService;
    ProgressBar progressBar;

    private static final String TAG = DetailView.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_view);

        billyapp = (BillyApplication) this.getApplication();

        itemData = getIntent().getExtras();
        song = itemData.getString("song");
        artist = itemData.getString("artist");
        artwork = itemData.getString("artwork").replaceAll("400x400", "600x600");
        songIndex = itemData.getInt("index");

        streamBtn = (ImageButton) findViewById(R.id.imageButton);
        dashes = (ImageView) findViewById(R.id.animate);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        playIcon = getResources().getDrawable(R.drawable.play);
        pauseIcon = getResources().getDrawable(R.drawable.pause);
        setButtonListener();

        req = billyapp.getRequestQueue();
        scUrl = getResources().getStringArray(R.array.url)[6]+artist.replaceAll(" ","+")+"+"+song.replaceAll(" ","+")+getResources().getStringArray(R.array.url)[7];
        Log.d(TAG, "scUrl is "+scUrl);
        StringRequest stringreq = new StringRequest(Request.Method.GET, scUrl, scComplete(), scError());
        req.add(stringreq);
        ft = new ProcessingTask();

        customActionBar();

        tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setTintColor(getResources().getColor(R.color.billyred));
        tintManager.setTintAlpha(0);

        if(billyapp.isConnected()){
            imgload = billyapp.getImageLoader();
        }
        hero = (NetworkImageView) findViewById(R.id.image_header);
        hero.setImageUrl(artwork,imgload);

        ((NotifyingScrollView) findViewById(R.id.scroll_view)).setOnScrollChangedListener(mOnScrollChangedListener);

        serviceIntent = new Intent(this,PlayerService.class);

        if(PlayerService.isRunning) {
            Log.d(TAG,"Service is already running and is now binded");
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG,"Service connected");
            PlayerService.PlayerServiceBinder binder = (PlayerService.PlayerServiceBinder) service;
            mService = binder.getService();

            binder.setListener(new PlayerService.onBPChangedListener() {
                @Override
                public void onPrepared(int duration) {
                    dashes.clearAnimation();
                    dashes.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setBackgroundColor(Color.TRANSPARENT);
                    progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_bar));
                    progressBar.setMax(duration);
                    songLength = duration;
                    thread.start();
                    streamBtn.setImageDrawable(pauseIcon);
                }

                @Override
                public void onCompletion() {
                    streamBtn.setImageDrawable(playIcon);
                    progressBar.setVisibility(View.INVISIBLE);
                    isMusicPlaying = false;
                }
            });
            mBound = true;
        }
    };


    Thread thread = new Thread()
    {
        public void run()
        {
            int prog = 0;
            while(prog < songLength) {
                if (isMusicPlaying) {
                    progressBar.setProgress(prog);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    prog++;
                }
                else
                    return;
            }
        }
    };

    private Response.Listener<String> scComplete() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    streamLink = ft.parseSoundcloud(response)+"?client_id=apigee";
                    ScaleAnimation scaleAnim = new ScaleAnimation(0.0f,1.0f,0.0f,1.0f,streamBtn.getWidth()/2,streamBtn.getHeight()/2);
                    scaleAnim.setDuration(800);
                    scaleAnim.setInterpolator(new BounceInterpolator());
                    streamBtn.startAnimation(scaleAnim);
                    streamBtn.setVisibility(View.VISIBLE);
                    Log.d(TAG,"streamLink is "+streamLink);
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

    private void setButtonListener() {
        streamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isMusicPlaying)
                {
                    if(streamLink !=null)
                    {
                        RotateAnimation rotateAnim = new RotateAnimation(0.0f,360.0f, dashes.getWidth()/2, dashes.getHeight()/2);
                        rotateAnim.setDuration(6000);
                        rotateAnim.setInterpolator(new LinearInterpolator());
                        rotateAnim.setRepeatMode(Animation.INFINITE);
                        dashes.setVisibility(View.VISIBLE);
                        dashes.setAnimation(rotateAnim);
                        dashes.startAnimation(rotateAnim);
                        serviceIntent.putExtra("streamLink",streamLink);
                        serviceIntent.putExtra("songName",song);
                        serviceIntent.putExtra("songIndex",songIndex);
                        startService(serviceIntent);
                        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
                        isMusicPlaying = true;
                    }
                    else{
                        Log.d(TAG,"streamLink is null");
                        Toast.makeText(getBaseContext(), "Something's wrong",
                           Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    streamBtn.setImageDrawable(playIcon);
                    unbindService(mConnection);
                    stopService(serviceIntent);
                    isMusicPlaying = false;
                    mBound = false;
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }


    private NotifyingScrollView.OnScrollChangedListener mOnScrollChangedListener = new NotifyingScrollView.OnScrollChangedListener() {
        public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
            final int headerHeight = findViewById(R.id.image_header).getHeight() - getActionBar().getHeight()-500;
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

    // Override this activity's setTitle method
    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        ((TextView) customActionView.findViewById(R.id.title)).setText(mTitle);
        getActionBar().setTitle("");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
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
        if(mBound)
        {
            unbindService(mConnection);
        }
    }
}
