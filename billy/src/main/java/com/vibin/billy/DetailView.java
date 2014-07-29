package com.vibin.billy;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Html;
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
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class DetailView extends Activity implements SeekBar.OnSeekBarChangeListener {
    String song, artwork, artist, album, streamLink, lastFmBio;
    int songIndex, songLength;
    boolean isMusicPlaying, mBound, stopTh;
    static boolean active;
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
    SeekBar seekBar;
    RotateAnimation rotateAnim;
    ScaleAnimation scaleAnim;
    NetworkImageView hero;
    RequestQueue req;
    PlayerService mService;
    Handler progressHandler;
    String lastFmBioUrl;
    float secondaryProgressFactor;

    private static final String TAG = DetailView.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_view);

        active = true;
        billyapp = (BillyApplication) this.getApplication();

        itemData = getIntent().getExtras();
        song = itemData.getString("song");
        artist = itemData.getString("artist");
        album = itemData.getString("album");
        artwork = itemData.getString("artwork");
        songIndex = itemData.getInt("index");

        streamBtn = (ImageButton) findViewById(R.id.streamButton);
        dashes = (ImageView) findViewById(R.id.dashes);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setAlpha(0.7f);
        seekBar.setOnSeekBarChangeListener(this);
        progressHandler = new Handler();
        playIcon = getResources().getDrawable(R.drawable.play);
        pauseIcon = getResources().getDrawable(R.drawable.pause);
        setButtonListener();

        if (savedInstanceState == null) {
            req = billyapp.getRequestQueue();
            final String scUrl = getResources().getString(R.string.soundcloud) + artist.replaceAll(" ", "+") + "+" + song.replaceAll(" ", "+") + getResources().getString(R.string.sc_params);
            Log.d(TAG, "scUrl is " + scUrl.substring(0, 100) + "...");
            StringRequest stringreq = new StringRequest(Request.Method.GET, scUrl, scComplete(), scError());
            req.add(stringreq);
            lastFmBioUrl = "http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=" + artist.replaceAll(" ", "+") + "&autocorrect=1&api_key=67b01760e70bb90ff51ae8590b3c2ba8&format=json";
            Log.d(TAG, lastFmBioUrl);
            JsonObjectRequest wikiSearch = new JsonObjectRequest(Request.Method.GET, lastFmBioUrl, null, lastFmBioComplete(), lastFmBioError());
            req.add(wikiSearch);
            ft = new ProcessingTask();
        }

        customActionBar();

        tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setTintColor(getResources().getColor(R.color.billyred));
        tintManager.setTintAlpha(0);

        ImageLoader imgload = billyapp.getImageLoader();
        hero = (NetworkImageView) findViewById(R.id.image_header);
        hero.setImageUrl(artwork, imgload);

        ((NotifyingScrollView) findViewById(R.id.scroll_view)).setOnScrollChangedListener(mOnScrollChangedListener);

        serviceIntent = new Intent(this, PlayerService.class);
        if (PlayerService.isRunning) {
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("lastFmBio", lastFmBio);
        outState.putString("streamLink", streamLink);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        lastFmBio = savedInstanceState.getString("lastFmBio");
        streamLink = savedInstanceState.getString("streamLink");
        (findViewById(R.id.spinner)).setVisibility(View.GONE);
        if (!lastFmBio.isEmpty()) {
            ((TextView) findViewById(R.id.artistTitle)).setText(artist);
            ((TextView) findViewById(R.id.artistBio)).setText(lastFmBio);
            (findViewById(R.id.artistInfo)).setVisibility(View.VISIBLE);
        }
        animate();
        streamBtn.setVisibility(View.VISIBLE);
    }

    private Response.Listener<JSONObject> lastFmBioComplete() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    (findViewById(R.id.spinner)).setVisibility(View.GONE);
                    lastFmBio = Html.fromHtml(jsonObject.getJSONObject("artist").getJSONObject("bio").getString("summary")).toString();
                    if (!lastFmBio.isEmpty()) {
                        String firstLine = lastFmBio.substring(0, lastFmBio.indexOf("."));
                        if (firstLine.startsWith("There are") || firstLine.startsWith("There is")) {
                            lastFmBio = lastFmBio.substring(lastFmBio.indexOf("1)") + 3);
                        }
                        lastFmBio = lastFmBio.substring(0, lastFmBio.indexOf(".", lastFmBio.indexOf(".") + 1) + 1);
                        if (lastFmBio.length() > 250) {
                            lastFmBio = lastFmBio.substring(0, lastFmBio.indexOf(".") + 1);
                        }
                        ((TextView) findViewById(R.id.artistTitle)).setText(artist);
                        ((TextView) findViewById(R.id.artistBio)).setText(lastFmBio);
                        (findViewById(R.id.artistInfo)).setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    Log.d(TAG,"Last.FM URL is "+lastFmBioUrl);
                    e.printStackTrace();
                }
            }
        };
    }

    private Response.ErrorListener lastFmBioError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d(TAG, volleyError.toString());
            }
        };
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
            mService = binder.getService();

            if (PlayerService.isRunning && !PlayerService.isIdle) {
                Log.d(TAG, "Service is running and is not idle - onServiceConnected");
                if (song.equalsIgnoreCase(mService.song)) {
                    if (mService.bp.isPlaying()) {
                        isMusicPlaying = true;
                        streamBtn.setImageDrawable(pauseIcon);
                        Thread progressThread = new Thread(progress);
                        progressThread.start();
                        songLength = mService.bp.getDuration() / 1000;
                        secondaryProgressFactor = (float) songLength/100;
                        Log.d(TAG,"songlength, secondary "+songLength+" "+secondaryProgressFactor);
                        seekBar.setMax(songLength);
                        seekBar.setVisibility(View.VISIBLE);
                    }
                }
            }

            binder.setListener(new PlayerService.onBPChangedListener() {
                @Override
                public void onPrepared(int duration) {
                    dashes.clearAnimation();
                    dashes.setVisibility(View.GONE);
                    seekBar.setMax(duration);
                    songLength = duration;
                    secondaryProgressFactor = (float) songLength/100;
                    Log.d(TAG,"songlength, secondary "+songLength+" "+secondaryProgressFactor);
                    Thread progressThread = new Thread(progress);
                    seekBar.setVisibility(View.VISIBLE);
                    progressThread.start();
                    streamBtn.setImageDrawable(pauseIcon);
                }

                // Only onCompletion and onError are called if stream-url is wrong (404, etc.)
                @Override
                public void onCompletion() {
                    streamBtn.setImageDrawable(playIcon);
                    seekBar.setVisibility(View.GONE);
                    stopTh = true;
                    isMusicPlaying = false;
                }

                @Override
                public void onError(int i, int i2) {
                    Toast.makeText(getBaseContext(), "Something's wrong. " + i + " " + i2,
                            Toast.LENGTH_LONG).show();
                    dashes.clearAnimation();
                    dashes.setVisibility(View.GONE);
                }

                @Override
                public void onStop() {
                    onCompletion();
                }

                @Override
                public void onNotificationPausePressed() {
                    streamBtn.setImageDrawable(playIcon);
                    seekBar.setVisibility(View.GONE);
                    isMusicPlaying = false;
                }

                @Override
                public void onNotificationPlayPressed() {
                    streamBtn.setImageDrawable(pauseIcon);
                    seekBar.setVisibility(View.VISIBLE);
                    isMusicPlaying = true;
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
                    if (PlayerService.isRunning && !PlayerService.isIdle) {
                        Log.d(TAG, "Service is running and is not idle - onButtonClick");
                        if (song.equalsIgnoreCase(mService.song)) {
                            Log.d(TAG, "Song matched");
                            mService.playMedia();
                            isMusicPlaying = true;
                            streamBtn.setImageDrawable(pauseIcon);
                            Thread progressThread = new Thread(progress);
                            progressThread.start();
                            seekBar.setVisibility(View.VISIBLE);
                        } else {
                            streamTrack();
                        }
                    } else {
                        streamTrack();
                    }
                } else {
                    streamBtn.setImageDrawable(playIcon);
                    dashes.clearAnimation();
                    dashes.setVisibility(View.INVISIBLE);
                    mService.doPause();
                    isMusicPlaying = false;
                }
            }
        });
    }

    //TODO check if streamLink is not a 404
    void streamTrack() {
        if (streamLink != null) {
            isMusicPlaying = true;
            dashes.setVisibility(View.VISIBLE);
            dashes.startAnimation(rotateAnim);
            serviceIntent.putExtra("streamLink", streamLink);
            serviceIntent.putExtra("songName", song);
            serviceIntent.putExtra("songIndex", songIndex);
            serviceIntent.putExtra("albumName", album);
            serviceIntent.putExtra("artistName", artist);
            serviceIntent.putExtra("artwork", artwork);
            startService(serviceIntent);
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        } else {
            Log.d(TAG, "streamLink is null");
            Toast.makeText(getBaseContext(), "The song cannot be streamed.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Run the progressbar in a separate thread
     */

    //TODO this thing crashes on notification dismiss
    Runnable progress = new Runnable() {
        @Override
        public void run() {
            if(!DetailView.active || stopTh){
                stopTh = false;
                return;
            }
            else if (PlayerService.isRunning) {
                if (!PlayerService.isIdle) {
                    seekBar.setProgress(mService.bp.getCurrentPosition() / 1000);
                    seekBar.setSecondaryProgress((int) (mService.bufferPercent*secondaryProgressFactor));
                    Log.d(TAG,"Max "+seekBar.getMax()+" progress "+seekBar.getProgress()+" secondary "+seekBar.getSecondaryProgress());
                }
            }
            progressHandler.postDelayed(progress, 1000);
        }
    };

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
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
        }
        active = false;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        seekBar.setAlpha(1.0f);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        seekBar.setAlpha(0.8f);
        mService.bp.seekTo(seekBar.getProgress() * 1000);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
    }
}
