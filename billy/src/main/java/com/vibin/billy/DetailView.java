package com.vibin.billy;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;

import static com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener;

public class DetailView extends FragmentActivity implements SeekBar.OnSeekBarChangeListener {
    String song, artwork, artist, album, streamLink, lastFmBio, videoId;
    String[] relatedAlbumImg, relatedAlbums;
    int songIndex, songLength;
    boolean isMusicPlaying, stopTh;
    static boolean isFullScreen;
    static boolean active, mBound;
    Drawable playIcon, pauseIcon;
    View customActionView;
    NotifyingImageButton streamBtn;
    NotifyingImageView dashes;
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
    ImageLoader imgload;
    RequestQueue req;
    PlayerService mService;
    Handler progressHandler;
    float secondaryProgressFactor;
    YouTubePlayerSupportFragment mYoutubePlayerFragment;
    static YouTubePlayer youtubePlayer;

    private static final String youtubeKey = "AIzaSyBTd_9XHpK-Jj7ZW8sNAstNKwSU18gf-6g";
    private static final String TAG = DetailView.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_view);

        active = true;
        billyapp = (BillyApplication) this.getApplication();
        imgload = billyapp.getImageLoader();

        itemData = getIntent().getExtras();
        song = itemData.getString("song");
        artist = itemData.getString("artist");
        album = itemData.getString("album");
        artwork = itemData.getString("artwork");
        songIndex = itemData.getInt("index");

        relatedAlbumImg = new String[3];
        relatedAlbums = new String[3];
        streamBtn = (NotifyingImageButton) findViewById(R.id.streamButton);
        dashes = (NotifyingImageView) findViewById(R.id.dashes);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setAlpha(0.7f);
        seekBar.setOnSeekBarChangeListener(this);

        //TODO Manage uncaught exceptions
/*        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(thread, throwable);
                    mService.uncaughtException();
                }
                catch(Exception e)
                {
                    Log.d(TAG, e.toString());
                }
            }
        });*/

        progressHandler = new Handler();
        playIcon = getResources().getDrawable(R.drawable.play);
        pauseIcon = getResources().getDrawable(R.drawable.pause);
        setButtonListener();

        streamBtn.setLayoutChangedListener(new NotifyingImageButton.OnLayoutChangedListener() {
            @Override
            public void onLayout(boolean changed, int l, int t, int r, int b) {
                animate();
            }
        });

        if (savedInstanceState == null) {
            performRequests();
        }

        customActionBar();

        hero = (NetworkImageView) findViewById(R.id.image_header);
        hero.setImageUrl(artwork, imgload);

        ((NotifyingScrollView) findViewById(R.id.scroll_view)).setOnScrollChangedListener(mOnScrollChangedListener);

        serviceIntent = new Intent(this, PlayerService.class);
        if (PlayerService.isRunning) {
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void performRequests() {
        req = billyapp.getRequestQueue();

        final String scUrl = getResources().getString(R.string.soundcloud) + artist.replaceAll(" ", "+").replaceAll("\u00eb", "e") + "+" + song.replaceAll(" ", "+") + getResources().getString(R.string.sc_params);
        final String lastFmBioUrl = "http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=" + artist.replaceAll(" ", "+") + "&autocorrect=1&api_key=67b01760e70bb90ff51ae8590b3c2ba8&format=json";
        final String lastFmTopAlbumsUrl = "http://ws.audioscrobbler.com/2.0/?method=artist.gettopalbums&artist=" + artist.replaceAll(" ", "+") + "&autocorrect=1&limit=3&api_key=67b01760e70bb90ff51ae8590b3c2ba8&format=json";
        final String youtubeUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=" + artist.replaceAll(" ", "+").replaceAll("\u00eb", "e") + "+" + song.replaceAll(" ", "+") + "&maxResults=2&type=video&key=" + youtubeKey;
        StringRequest stringreq = new StringRequest(Request.Method.GET, scUrl, scComplete(), scError());
        JsonObjectRequest lastFmBio = new JsonObjectRequest(Request.Method.GET, lastFmBioUrl, null, lastFmBioComplete(), lastFmBioError());
        JsonObjectRequest lastFmTopAlbums = new JsonObjectRequest(Request.Method.GET, lastFmTopAlbumsUrl, null, lastFmTopAlbumsComplete(), lastFmTopAlbumsError());
        JsonObjectRequest youtubeSearch = new JsonObjectRequest(Request.Method.GET, youtubeUrl, null, youtubeSearchComplete(), youtubeSearchError());

        //Log.d(TAG, "scUrl is " + scUrl.substring(0, 100) + "...");
        Log.d(TAG, "scUrl is " + scUrl);
        Log.d(TAG,"topalbum "+lastFmTopAlbumsUrl);
        Log.d(TAG, "YoutubeURL is " + youtubeUrl);

        req.add(stringreq);
        req.add(lastFmTopAlbums);
        req.add(lastFmBio);
        req.add(youtubeSearch);

        ft = new ProcessingTask();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("lastFmBio", lastFmBio);
        outState.putString("streamLink", streamLink);
        outState.putStringArray("relatedAlbumImg", relatedAlbumImg);
        outState.putStringArray("relatedAlbums", relatedAlbums);
        outState.putString("videoId",videoId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        lastFmBio = savedInstanceState.getString("lastFmBio");
        streamLink = savedInstanceState.getString("streamLink");
        relatedAlbums = savedInstanceState.getStringArray("relatedAlbums");
        relatedAlbumImg = savedInstanceState.getStringArray("relatedAlbumImg");
        videoId = savedInstanceState.getString("videoId");
        if (lastFmBio.isEmpty() || streamLink.isEmpty() || Arrays.asList(relatedAlbumImg).contains(null) || Arrays.asList(relatedAlbumImg).contains(null) || videoId.isEmpty()) {
            Log.d(TAG,"some data is null, requests performed again");
            performRequests();
        } else {
            streamBtn.setVisibility(View.VISIBLE);
            (findViewById(R.id.spinner)).setVisibility(View.GONE);
            setLastFmBio();
            setRelatedAlbums();
            setYoutube(videoId);
        }
    }

    private Response.Listener<String> scComplete() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    streamLink = ft.parseSoundcloud(response);
                    //streamLink = "https://ec-hls-media.soundcloud.com/playlist/OM6ZltKo22zf.128.mp3/playlist.m3u8?f10880d39085a94a0418a7e062b03d52bbdc0e179b82bde1d76ce4ad1a476e0aa49ee43247155726311c5d77ec8a1001a3f9d1e6ae1204b7d251f059104a2bfb4c9f9fbce72d6ab284af54464e022ea558284df25cb10cb0a6fb9224";
                    Log.d(TAG, "streamLink is " + streamLink);
                    if(scaleAnim != null) {
                        streamBtn.startAnimation(scaleAnim);
                    }
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
                        setLastFmBio();
                    }
                } catch (JSONException e) {
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

    private Response.Listener<JSONObject> lastFmTopAlbumsComplete() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    JSONArray jsonArray = jsonObject.getJSONObject("topalbums").getJSONArray("album");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        relatedAlbums[i] = obj.getString("name");
                        relatedAlbumImg[i] = obj.getJSONArray("image").getJSONObject(3).getString("#text");
                    }

                    setRelatedAlbums();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private Response.ErrorListener lastFmTopAlbumsError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, volleyError.toString());
            }
        };
    }

    private Response.Listener<JSONObject> youtubeSearchComplete() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    videoId = jsonObject.getJSONArray("items").getJSONObject(0).getJSONObject("id").getString("videoId");
                    setYoutube(videoId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private Response.ErrorListener youtubeSearchError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d(TAG, volleyError.toString());
            }
        };
    }

    private void setLastFmBio() {
        ((TextView) findViewById(R.id.artistTitle)).setText(artist);
        ((TextView) findViewById(R.id.artistBio)).setText(lastFmBio);
        (findViewById(R.id.artistInfo)).setVisibility(View.VISIBLE);
    }

    private void setRelatedAlbums() {
        Resources res = getResources();
        RelativeLayout rela = (RelativeLayout) findViewById(R.id.topAlbumImages);
        for (int i = 0; i < relatedAlbums.length; i++) {
            int id = res.getIdentifier("relatedImage" + i, "id", getPackageName());
            NetworkImageView v = (NetworkImageView) rela.findViewById(id);
            v.setImageUrl(relatedAlbumImg[i], imgload);

            TextView tv = (TextView) rela.findViewById(res.getIdentifier("relatedText" + i, "id", getBaseContext().getPackageName()));
            try {
                if (relatedAlbums[i].length() > 16) {
                    tv.setText(relatedAlbums[i].substring(0, 14) + "…");
                } else {
                    tv.setText(relatedAlbums[i]);
                }
            } catch (NullPointerException e) {
                Log.d(TAG, "NullPointer we meet again");
            }
        }

        (findViewById(R.id.topAlbums)).setVisibility(View.VISIBLE);
    }

    private void setYoutube(final String videoId) {
        Log.d(TAG,((FrameLayout) findViewById(R.id.youTubeFrame)).getChildCount()+"");

        Log.d(TAG,"videoId is "+videoId);
        mYoutubePlayerFragment = PlayerYouTubeFrag.newInstance(videoId);
        getSupportFragmentManager().beginTransaction().add(R.id.youTubeFrame, mYoutubePlayerFragment, "youTubeFragment").commit();

        Log.d(TAG, ((FrameLayout) findViewById(R.id.youTubeFrame)).getChildCount() + "");

        ((RelativeLayout) findViewById(R.id.youTube)).setVisibility(View.VISIBLE);

        Log.d(TAG,((FrameLayout) findViewById(R.id.youTubeFrame)).getChildCount()+"");
        Log.d(TAG,((FrameLayout) findViewById(R.id.youTubeFrame)).getChildAt(0).toString());
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
                    setSeekBar();
                    if (mService.bp.isPlaying()) {
                        isMusicPlaying = true;
                        streamBtn.setImageDrawable(pauseIcon);
                    }
                }
            }

            binder.setListener(new PlayerService.onBPChangedListener() {
                @Override
                public void onPrepared(int duration) {
                    dashes.clearAnimation();
                    dashes.setVisibility(View.GONE);
                    songLength = duration;
                    setSeekBar();
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

                @Override
                public void doUnbind() {
                    if (mBound) {
                        unbindService(mConnection);
                        mBound = false;
                    }
                }

            });
            mBound = true;
        }
    };

    private void setSeekBar() {
        Thread progressThread = new Thread(progress);
        progressThread.start();
        songLength = mService.bp.getDuration() / 1000;
        secondaryProgressFactor = (float) songLength / 100;
        Log.d(TAG, "songlength, secondary " + songLength + " " + secondaryProgressFactor);
        seekBar.setMax(songLength);
        seekBar.setVisibility(View.VISIBLE);
    }

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

    Runnable progress = new Runnable() {
        @Override
        public void run() {
            if (!DetailView.active || stopTh) {
                stopTh = false;
                return;
            } else if (PlayerService.isRunning) {
                if (!PlayerService.isIdle) {
                    seekBar.setProgress(mService.bp.getCurrentPosition() / 1000);
                    seekBar.setSecondaryProgress((int) (mService.bufferPercent * secondaryProgressFactor));
                    //Log.d(TAG, "Max " + seekBar.getMax() + " progress " + seekBar.getProgress() + " secondary " + seekBar.getSecondaryProgress());
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

        tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setTintColor(getResources().getColor(R.color.billyred));
        tintManager.setTintAlpha(0);
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
            mBound = false;
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

    //TODO Set clicklistener for a play button
    public static class PlayerYouTubeFrag extends YouTubePlayerSupportFragment{

        private String videoId;

        public static PlayerYouTubeFrag newInstance(String videoId) {

            PlayerYouTubeFrag playerYouTubeFrag = new PlayerYouTubeFrag();

            Bundle bundle = new Bundle();
            bundle.putString("videoId", videoId);
            playerYouTubeFrag.setArguments(bundle);

            return playerYouTubeFrag;
        }

        @Override
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            init();

            return super.onCreateView(layoutInflater, viewGroup, bundle);
        }

        private void init() {
            videoId = getArguments().getString("videoId");
            initialize(youtubeKey, new YouTubePlayer.OnInitializedListener() {
                @Override
                public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                    youtubePlayer = youTubePlayer;
                    youtubePlayer.setOnFullscreenListener(new YouTubePlayer.OnFullscreenListener() {
                        @Override
                        public void onFullscreen(boolean b) {
                            isFullScreen = b;
                        }
                    });

                    youTubePlayer.setPlaybackEventListener(new YouTubePlayer.PlaybackEventListener() {
                        @Override
                        public void onPlaying() {
                            Log.d(TAG,"starting video");
                            youtubePlayer.setFullscreen(true);
                        }

                        @Override
                        public void onPaused() {

                        }

                        @Override
                        public void onStopped() {

                        }

                        @Override
                        public void onBuffering(boolean b) {

                        }

                        @Override
                        public void onSeekTo(int i) {

                        }
                    });

                    if (!b) {
                        youtubePlayer.cueVideo(videoId);
                        youtubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.MINIMAL);
                        youTubePlayer.setFullscreenControlFlags(YouTubePlayer.FULLSCREEN_FLAG_CONTROL_ORIENTATION);
                    }
                }

                @Override
                public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                    Log.d(TAG, "Player initialization failed");
                }
            });
        }

    }

    @Override
    public void onBackPressed() {
        Log.d(TAG,"video is fullscreen: "+isFullScreen);
        if(isFullScreen)
        {
            youtubePlayer.pause();
            youtubePlayer.setFullscreen(false);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

/*        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (youtubePlayer != null)
                youtubePlayer.setFullscreen(true);
        }
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (youtubePlayer != null)
                youtubePlayer.setFullscreen(false);
        }*/
    }
}
