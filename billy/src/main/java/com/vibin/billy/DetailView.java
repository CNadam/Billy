package com.vibin.billy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.view.MenuItemCompat;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.google.android.youtube.player.YouTubeIntents;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.vibin.billy.swipeable.SwipeableActivity;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;

/**
 * The detail view of a song. Is swipeable.
 */

public class DetailView extends SwipeableActivity implements SeekBar.OnSeekBarChangeListener {
    String song, artwork, artist, album, streamLink, permaLink, lastFmBio, thumbnail, videoId;
    String[] relatedAlbumImg, relatedAlbums;
    int songIndex, songLength;
    float secondaryProgressFactor, scrollOpacity = 0;
    boolean isMusicPlaying, stopTh, isCurrentSongBG, isPreparing;
    static boolean active, mBound;
    MenuItem shareItem;
    Drawable playIcon, pauseIcon;
    ImageButton streamBtn;
    ImageView dashes;
    Intent serviceIntent;
    BillyApplication billyapp;
    Drawable mActionBarBackgroundDrawable;
    SystemBarTintManager tintManager;
    ProcessingTask ft;
    SeekBar seekBar;
    RotateAnimation rotateAnim;
    ScaleAnimation scaleAnim;
    ImageLoader imgload;
    PPlayerService mService;
    Thread progressThread;

    private static final String TAG = DetailView.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.detail_view);

        active = true;
        billyapp = (BillyApplication) this.getApplication();

        imgload = billyapp.getImageLoader();

        Bundle itemData = getIntent().getExtras();
        song = itemData.getString("song");
        artist = itemData.getString("artist");
        if (artist.contains(",")) {
            artist = artist.substring(0, artist.indexOf(","));
        }
        album = itemData.getString("album");
        artwork = itemData.getString("artwork");
        songIndex = itemData.getInt("index");

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        tintManager = new SystemBarTintManager(this);
        customActionBar();

        billyapp.getActionBarView(getWindow()).addOnLayoutChangeListener(expandedDesktopListener);

        streamBtn = (ImageButton) findViewById(R.id.streamButton);
        dashes = (ImageView) findViewById(R.id.dashes);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setAlpha(0.85f);
        seekBar.setOnSeekBarChangeListener(this);

        playIcon = getResources().getDrawable(R.drawable.play);
        pauseIcon = getResources().getDrawable(R.drawable.pause);
        setButtonListener();

        dashes.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                dashes.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                animate();
            }
        });

        RequestQueue req = billyapp.getRequestQueue();
        if (savedInstanceState == null) {
            performRequests(req);
        }

        NetworkImageView hero = (NetworkImageView) findViewById(R.id.image_header);
        hero.setImageUrl(artwork, imgload);

        serviceIntent = new Intent(this, PPlayerService.class);
        if (PPlayerService.isRunning) {
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void performRequests(RequestQueue req) {
        super.enableSwipeToDismiss();

        final String scUrl = getResources().getString(R.string.soundcloud, (song + " " + StringUtils.stripAccents(artist)).replaceAll(" ", "+"));
        final String lastFmBioUrl = getResources().getString(R.string.lastfm, "getinfo", artist.replaceAll(" ", "+").replaceAll("&","and"));
        final String lastFmTopAlbumsUrl = getResources().getString(R.string.lastfm, "gettopalbums", artist.replaceAll(" ", "+"));
        final String youtubeUrl = getResources().getString(R.string.youtube, (song + " " + StringUtils.stripAccents(artist)).replaceAll(" ", "+"));
        StringRequest stringreq = new StringRequest(Request.Method.GET, scUrl, scComplete(), scError());
        JsonObjectRequest lastFmBio = new JsonObjectRequest(Request.Method.GET, lastFmBioUrl, null, lastFmBioComplete(), lastFmBioError());
        JsonObjectRequest lastFmTopAlbums = new JsonObjectRequest(Request.Method.GET, lastFmTopAlbumsUrl, null, lastFmTopAlbumsComplete(), lastFmTopAlbumsError());
        JsonObjectRequest youtubeSearch = new JsonObjectRequest(Request.Method.GET, youtubeUrl, null, youtubeSearchComplete(), youtubeSearchError());

        Log.d(TAG, "scUrl is " + scUrl);
        Log.d(TAG, "topalbum " + lastFmTopAlbumsUrl);
        Log.d(TAG, "YoutubeURL is " + youtubeUrl);

        req.add(stringreq);
        req.add(lastFmTopAlbums);
        req.add(lastFmBio);
        req.add(youtubeSearch);

        ft = new ProcessingTask(getBaseContext());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("lastFmBio", lastFmBio);
        outState.putString("streamLink", streamLink);
        outState.putStringArray("relatedAlbumImg", relatedAlbumImg);
        outState.putStringArray("relatedAlbums", relatedAlbums);
        outState.putString("thumbnail", thumbnail);
        outState.putString("videoId", videoId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        super.enableSwipeToDismiss();
        lastFmBio = savedInstanceState.getString("lastFmBio");
        streamLink = savedInstanceState.getString("streamLink");
        relatedAlbums = savedInstanceState.getStringArray("relatedAlbums");
        relatedAlbumImg = savedInstanceState.getStringArray("relatedAlbumImg");
        thumbnail = savedInstanceState.getString("thumbnail");
        videoId = savedInstanceState.getString("videoId");
        try {
            if (lastFmBio.isEmpty() || streamLink.isEmpty() || Arrays.asList(relatedAlbumImg).contains(null) || Arrays.asList(relatedAlbumImg).contains(null) || thumbnail.isEmpty()) {
                Log.d(TAG, "some data is null, requests performed again");
                billyapp = (BillyApplication) this.getApplication();
                performRequests(billyapp.getRequestQueue());
            } else {
                streamBtn.setVisibility(View.VISIBLE);
                (findViewById(R.id.spinner)).setVisibility(View.GONE);
                setLastFmBio();
                setRelatedAlbums();
                setYoutube(thumbnail, videoId);
            }
        } catch (NullPointerException e) {
            Log.d(TAG, e.toString());
        }
    }

    private Response.Listener<String> scComplete() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    String[] result = ft.parseSoundcloud(response, song);
                    permaLink = result[0];
                    streamLink = result[1];
                    setShareButton();
//                    JsonObjectRequest i1 = new JsonObjectRequest(Request.Method.GET, "https://api.soundcloud.com/i1/tracks/133433134/streams?client_id=apigee", null, i1Complete(), i1Error());
//                    req.add(i1);
                    if (scaleAnim != null) {
                        streamBtn.startAnimation(scaleAnim);
                    }
                    streamBtn.setVisibility(View.VISIBLE);
                    Log.d(TAG, "original streamLink is " + streamLink);
                    Log.d(TAG, "original permaLink is " + permaLink);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
            }

/*            private Response.Listener<JSONObject> i1Complete() {
                return new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        try {
                            String hlsurl = jsonObject.getString("hls_mp3_128_url");
                            AsyncHttpClient ac = new AsyncHttpClient();
                            ac.get(getBaseContext(), hlsurl, new FileAsyncHttpResponseHandler(getBaseContext()) {
                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                                    Toast.makeText(getBaseContext(), "File failed.",
                                            Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onSuccess(int statusCode, Header[] headers, File file) {
                                    Toast.makeText(getBaseContext(), "File download success",
                                            Toast.LENGTH_LONG).show();

                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
            }*/

        };
    }

    private Response.ErrorListener i1Error() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d(TAG, "" + volleyError);
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
     * Strip HTML tags from returned bio
     * If result contains two artists, choose the first one
     */
    private Response.Listener<JSONObject> lastFmBioComplete() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    (findViewById(R.id.spinner)).setVisibility(View.GONE);
                    lastFmBio = Html.fromHtml(jsonObject.getJSONObject("artist").getJSONObject("bio").getString("summary")).toString();
                    if (!lastFmBio.isEmpty()) {
                        if (lastFmBio.contains(" 1) ")) {
                            lastFmBio = lastFmBio.substring(lastFmBio.lastIndexOf(" 1) ") + 4);
                        } else if (lastFmBio.contains(" 1. ")) {
                            lastFmBio = lastFmBio.substring(lastFmBio.lastIndexOf(" 1. ") + 4);
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
                    int length = jsonArray.length();
                    relatedAlbums = new String[length];
                    relatedAlbumImg = new String[length];
                    for (int i = 0; i < length; i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        relatedAlbums[i] = obj.getString("name");
                        relatedAlbumImg[i] = obj.getJSONArray("image").getJSONObject(3).getString("#text");
                    }
                    setRelatedAlbums();
                } catch (JSONException e) {
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
                    thumbnail = jsonObject.getJSONArray("items").getJSONObject(0).getJSONObject("snippet").getJSONObject("thumbnails").getJSONObject("high").getString("url");
                    setYoutube(thumbnail, videoId);
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
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.topAlbumImages);
        for (int i = 0; i < relatedAlbums.length; i++) {
            int id = res.getIdentifier("relatedImage" + i, "id", getPackageName());
            NetworkImageView v = (NetworkImageView) relativeLayout.findViewById(id);
            v.setImageUrl(relatedAlbumImg[i], imgload);

            TextView tv = (TextView) relativeLayout.findViewById(res.getIdentifier("relatedText" + i, "id", getBaseContext().getPackageName()));
            if (relatedAlbums[i].length() > 16) {
                tv.setText(relatedAlbums[i].substring(0, 14) + "…");
            } else {
                tv.setText(relatedAlbums[i]);
            }
        }

        (findViewById(R.id.topAlbums)).setVisibility(View.VISIBLE);
    }

    private void setYoutube(String thumbnail, final String videoId) {
        final NetworkImageView youTubeThumbnail = (NetworkImageView) findViewById(R.id.youTubeThumbnail);
        youTubeThumbnail.setImageUrl(thumbnail, imgload);
        findViewById(R.id.youTube).setVisibility(View.VISIBLE);
        final ImageButton youTubePlay = (ImageButton) findViewById(R.id.youTubePlay);
        View.OnTouchListener opacityListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (view.equals(youTubePlay) || view.equals(youTubeThumbnail)) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        youTubePlay.setAlpha(1f);
                    } else {
                        youTubePlay.setAlpha(.8f);
                    }
                }
                return false;
            }
        };
        View.OnClickListener startYouTube = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (YouTubeIntents.isYouTubeInstalled(getBaseContext())) {
                    if (YouTubeIntents.canResolvePlayVideoIntent(getBaseContext())) {
                        Intent intent = YouTubeIntents.createPlayVideoIntentWithOptions(getBaseContext(), videoId, true, true);
                        if (isMusicPlaying && PPlayerService.isRunning) {
                            mService.doPause();
                        }
                        if (!isPreparing) {
                            startActivity(intent);
                        }
                    } else {
                        Toast.makeText(getBaseContext(), "Please update the YouTube app.",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getBaseContext(), "Install YouTube app to watch videos.",
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        youTubeThumbnail.setOnTouchListener(opacityListener);
        youTubeThumbnail.setOnClickListener(startYouTube);
        youTubePlay.setOnTouchListener(opacityListener);
        youTubePlay.setOnClickListener(startYouTube);
    }


    /**
     * Bind this activity and PPlayerService
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            PPlayerService.PPlayerServiceBinder binder = (PPlayerService.PPlayerServiceBinder) service;
            mService = binder.getService();

            isMusicPlaying = mService.bp.isPlaying();
            if (PPlayerService.isRunning && !PPlayerService.isIdle) {
                Log.d(TAG, "Service is running and is not idle");
                if (song.equalsIgnoreCase(mService.song)) {
                    isCurrentSongBG = true;
                    setSeekBar();
                    if (isMusicPlaying) {
                        streamBtn.setImageDrawable(pauseIcon);
                    }
                }
            }

            binder.setListener(new PPlayerService.onBPChangedListener() {
                @Override
                public void onPrepared(int duration) {
                    isMusicPlaying = true;
                    dashes.clearAnimation();
                    isPreparing = false;
                    dashes.setVisibility(View.GONE);
                    songLength = duration;
                    setSeekBar();
                    streamBtn.setImageDrawable(pauseIcon);
                    isCurrentSongBG = true;
                }

                // Only onCompletion and onError are called if streamLink is wrong (404, etc.)
                @Override
                public void onCompletion() {
                    streamBtn.setImageDrawable(playIcon);
                    seekBar.setVisibility(View.GONE);
                    stopTh = true;
                    isMusicPlaying = false;
                }

                @Override
                public void onError() {
                    //Toast.makeText(getBaseContext(), "An error has occurred. " + i + " " + i2,
                    //        Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Cannot play this song. ");
                    dashes.clearAnimation();
                    dashes.setVisibility(View.GONE);
                }

                @Override
                public void onStop() {
                    Log.d(TAG, "Onstop");
                    onCompletion();
                }

                @Override
                public void onNotificationPausePressed() {
                    streamBtn.setImageDrawable(playIcon);
                    //seekBar.setVisibility(View.GONE);
                    isMusicPlaying = false;
                }

                @Override
                public void onNotificationPlayPressed() {
                    streamBtn.setImageDrawable(pauseIcon);
                    seekBar.setVisibility(View.VISIBLE);
                    isMusicPlaying = true;
                }

                @Override
                public void onNotificationStopPressed() {
                    seekBar.setVisibility(View.GONE);
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
        progressThread = new Thread(progress);
        progressThread.start();
        songLength = (int) mService.bp.getLength() / 1000; //ms
        secondaryProgressFactor = (float) songLength / 100;
        Log.d(TAG, "songlength, secondary " + songLength + " " + secondaryProgressFactor);
        seekBar.setMax(1000);
        seekBar.setVisibility(View.VISIBLE);
    }

    /**
     * Handles clicks for play button
     * If loading animation of button is on, do nothing.
     * If current song is in background and is not playing, then play it. If already playing, then pause it.
     * If background song is different from current song, stream it.
     */
    private void setButtonListener() {
        streamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPreparing) {
                    if (isCurrentSongBG) {
                        if (!isMusicPlaying) {
                            Log.d(TAG, "Song matched");
                            if (PPlayerService.isRunning) {
                                mService.doPlay();
                                isMusicPlaying = true;
                                streamBtn.setImageDrawable(pauseIcon);
                                if (!progressThread.isAlive()) {
                                    setSeekBar();
                                }
                                seekBar.setVisibility(View.VISIBLE);
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
                    } else {
                        streamTrack();
                    }
                }
            }
        });
    }

    /**
     * Gets that damn song
     */

    void streamTrack() {
        if (streamLink != null) {
            isMusicPlaying = true;
            dashes.setVisibility(View.VISIBLE);
            dashes.startAnimation(rotateAnim);
            isPreparing = true;
            //streamLink = "rtmp://ec-rtmp-media.soundcloud.com/mp3:7faed9oUCfzf.128?9527d18f1063a01f059bf10590159adb10dea0996b8c0cdb674f9e2a22158b9e2c124b95828db74e27f9807e908a0a15c5d9a2b9db27558bfafb06c4246b4f9e1e181b56d687209f037cda21bb2a36b9f63ca84bed96bfaa0d62";
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
            Toast.makeText(getBaseContext(), "The song cannot be streamed. Try again later.",
                    Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Runs the progressbar in a Runnable
     * {@code stopTh} determines whether to stop the thread or not
     */
    Handler progressHandler = new Handler();
    Runnable progress = new Runnable() {
        @Override
        public void run() {
            try {
                if (!DetailView.active || stopTh) {
                    stopTh = false;
                    return;
                } else if (PPlayerService.isRunning) {
                    if (!PPlayerService.isIdle) {
                        seekBar.setProgress((int) (mService.bp.getPosition() * 1000));
                        seekBar.setSecondaryProgress(mService.bufferPercent * 10);
                        if (seekBar.getProgress() == 1000) {
                            Log.d(TAG, "killing thread");
                            stopTh = true;
                        }
                        Log.d(TAG, "Max " + seekBar.getMax() + " progress " + seekBar.getProgress() + " and secondary " + seekBar.getSecondaryProgress());
                        //Log.d(TAG,"Length "+mService.bp.getLength()+"          Time "+mService.bp.getTime()+"           Position "+mService.bp.getPosition());
                    }
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            progressHandler.postDelayed(progress, 1000);
        }
    };


    /**
     * Animates the play button and dashes
     * Call this only after views are laid out. Use {@code getViewTreeObserver().addOnGlobalLayoutListener}
     */

    private void animate() {
        rotateAnim = new RotateAnimation(0.0f, 360.0f, dashes.getWidth() / 2, dashes.getHeight() / 2);
        rotateAnim.setDuration(6000);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.setRepeatMode(Animation.RESTART);
        rotateAnim.setRepeatCount(Animation.INFINITE);

        scaleAnim = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, streamBtn.getWidth() / 2, streamBtn.getHeight() / 2);
        scaleAnim.setDuration(800);
        scaleAnim.setInterpolator(new BounceInterpolator());
    }

    /**
     * Generate a new alpha value for every scroll event
     * Apply parallax to ScrollView and alpha to ActionBar, Status Bar
     */

    private NotifyingScrollView.OnScrollChangedListener mOnScrollChangedListener = new NotifyingScrollView.OnScrollChangedListener() {
        public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
            final int headerHeight = findViewById(R.id.image_header).getHeight() - getSupportActionBar().getHeight() - 500;
            scrollOpacity = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
            final int newAlpha = (int) (scrollOpacity * 255);
            mActionBarBackgroundDrawable.setAlpha(newAlpha);
            tintManager.setTintAlpha(scrollOpacity);
        }
    };

    /**
     * Detects if the user is using phone in Expanded Desktop/Fullscreen mode, and toggles Status Bar tint
     */

    View.OnLayoutChangeListener expandedDesktopListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            int position[] = new int[2];
            view.getLocationOnScreen(position);
            if (position[1] == 0) {
                tintManager.setStatusBarTintEnabled(false);
            } else {
                tintManager.setStatusBarTintEnabled(true);
                tintManager.setStatusBarAlpha(scrollOpacity);
                tintManager.setTintColor(getResources().getColor(R.color.billy));
            }
        }
    };

    /**
     * Most of the ActionBar customization is done using /values[-v19]/styles.xml, remaining stuff is here
     * Transit ActionBar and status bar colors if running JellyBean 4.2 or newer
     * Apply parallax scrolling
     */

    private void customActionBar() {
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(" " + song.toUpperCase());

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_solid));
            getBaseContext().setTheme(R.style.Theme_Billy);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mActionBarBackgroundDrawable = getResources().getDrawable(R.drawable.ab_solid);
            mActionBarBackgroundDrawable.setAlpha(0);
            getSupportActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);
        }

        ((NotifyingScrollView) findViewById(R.id.scroll_view)).setOnScrollChangedListener(mOnScrollChangedListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail, menu);

        shareItem = menu.findItem(R.id.share);
        setShareButton();

        super.onCreateOptionsMenu(menu);
        return true;
    }

    /**
     * Puts a share button in ActionBar, with share history included
     * We ideally want to hide the share history, but it's not possible
     */
    void setShareButton() {
        CustomShareActionProvider myShareActionProvider = null;
        try {
            myShareActionProvider = (CustomShareActionProvider) MenuItemCompat.getActionProvider(shareItem);

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, getIntentText());
            shareIntent.setType(HTTP.PLAIN_TEXT_TYPE);

            myShareActionProvider.setShareIntent(shareIntent);
            myShareActionProvider.setShareHistoryFileName("deleteMe");
        } catch (NullPointerException ignored) {
        }
    }

    /**
     * Append SoundCloud permalink to intent text, if it's not null
     */
    String getIntentText() {
        if (permaLink != null) {
            return song + " by " + artist + " #nowplaying " + permaLink;
        } else {
            return song + " by " + artist + " #nowplaying";
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
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

    /**
     * Unbind service before Activity gets destroyed
     */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            try {
                unbindService(mConnection);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            mBound = false;
        }
        active = false;
    }

    /**
     * Highlights the seekBar on touch and seeks audio on release
     */

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        seekBar.setAlpha(1.0f);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        seekBar.setAlpha(0.85f);
        mService.bp.setPosition((float) seekBar.getProgress() / 1000);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
    }
}
