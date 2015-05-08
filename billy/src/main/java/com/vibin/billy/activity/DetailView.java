package com.vibin.billy.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.youtube.player.YouTubeIntents;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.vibin.billy.BillyApplication;
import com.vibin.billy.BillyItem;
import com.vibin.billy.BuildConfig;
import com.vibin.billy.R;
import com.vibin.billy.custom.NotifyingScrollView;
import com.vibin.billy.custom.ShareActionProvider;
import com.vibin.billy.http.JsonObjectRequest;
import com.vibin.billy.http.StringRequest;
import com.vibin.billy.service.PlayerService;
import com.vibin.billy.swipeable.SwipeableActivity;
import com.vibin.billy.util.ProcessingTask;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * The detail view of a song.
 */

public class DetailView extends SwipeableActivity implements SeekBar.OnSeekBarChangeListener {
    private String song, artist, streamLink, permaLink, lastFmBio, thumbnail, videoId;
    private String[] relatedAlbumImg, relatedAlbums;
    BillyItem b;
    int paletteColor;
    private int songRank, songLength, orientation;
    private float secondaryProgressFactor;
    private boolean isMusicPlaying;
    private boolean stopTh;
    private boolean isPreparing;
    private boolean appendOrignal;
    private static boolean active, mBound;
    private AdView mAdView;
    private Drawable playIcon, pauseIcon;
    private ImageButton streamBtn;
    private ImageView dashes;
    private Intent serviceIntent;
    private BillyApplication billyapp;
    private Drawable mActionBarBackgroundDrawable;
    private SystemBarTintManager tintManager;
    private Toolbar bar;
    private ProcessingTask ft;
    private SeekBar seekBar;
    private RotateAnimation rotateAnim;
    private ScaleAnimation scaleAnim;
    private ImageLoader imgload;
    private PlayerService mService;
    private Thread progressThread;

    private static final String TAG = DetailView.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.detail_view);
        onNewIntent(getIntent());
    }

    /**
     * All intents including the first one is sent to this
     * <p/>
     * Use {@link #setIntent} to change the Activity's current intent to the new one and refresh UI.
     */
    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);

        setIntent(newIntent);
        active = true;
        billyapp = (BillyApplication) this.getApplication();

        imgload = billyapp.getImageLoader();

        b = newIntent.getExtras().getParcelable("item");
        song = b.getSong();
        artist = b.getArtist();
        String artwork = b.getArtwork();
        songRank = b.getRank();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        orientation = getResources().getConfiguration().orientation;
        tintManager = new SystemBarTintManager(this);
        paletteColor = newIntent.getExtras().getInt("paletteColor");
        setToolbar(paletteColor);

        bar.addOnLayoutChangeListener(expandedDesktopListener);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarAlpha(0.0f);
        tintManager.setTintColor(getResources().getColor(R.color.billy));

        mAdView = (AdView) findViewById(R.id.adView);

        streamBtn = (ImageButton) findViewById(R.id.streamButton);
        dashes = (ImageView) findViewById(R.id.dashes);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setAlpha(0.85f);
        seekBar.setOnSeekBarChangeListener(this);

        playIcon = getResources().getDrawable(R.drawable.play);
        pauseIcon = getResources().getDrawable(R.drawable.pause);
        setButtonListener();

        //playIcon.setTint(paletteColor);
        //pauseIcon.setTint(paletteColor);

        dashes.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                dashes.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                animate();
            }
        });

        RequestQueue req = billyapp.getRequestQueue();
        try {
            performRequests(req);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        NetworkImageView hero = (NetworkImageView) findViewById(R.id.artwork);
        hero.setImageUrl(artwork, imgload);

        serviceIntent = new Intent(this, PlayerService.class);
        if (PlayerService.isRunning) {
            if (mBound) {
                progressThread = null;
                stopTh = true;
                removeBonding(); //unbind service if already bound so as to update UI with new intent
            }
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    private void performRequests(RequestQueue req) throws UnsupportedEncodingException {
        super.enableSwipeToDismiss();

        String singleArtist = b.getSingleArtist();
        String simpleArtist = b.getSimpleArtist();
        String simpleSong = b.getSimpleSong();

        performSCRequest(req, simpleSong, simpleArtist);
        final String lastFmBioUrl = getResources().getString(R.string.lastfm, "getinfo", billyapp.UTF8(singleArtist).replaceAll(" ", "+"));
        final String lastFmTopAlbumsUrl = getResources().getString(R.string.lastfm, "gettopalbums", billyapp.UTF8(singleArtist).replaceAll(" ", "+"));
        final String youtubeUrl = getResources().getString(R.string.youtube, (simpleSong + " " + billyapp.UTF8(simpleArtist)).replaceAll(" ", "+"));
        JsonObjectRequest lastFmBio = new JsonObjectRequest(lastFmBioUrl, null, lastFmBioComplete(), lastFmBioError());
        JsonObjectRequest lastFmTopAlbums = new JsonObjectRequest(lastFmTopAlbumsUrl, null, lastFmTopAlbumsComplete(), lastFmTopAlbumsError());
        JsonObjectRequest youtubeSearch = new JsonObjectRequest(youtubeUrl, null, youtubeSearchComplete(), youtubeSearchError());

        Log.d(TAG, "topalbum " + lastFmTopAlbumsUrl);
        Log.d(TAG, "YoutubeURL is " + youtubeUrl);

        req.add(lastFmTopAlbums);
        req.add(lastFmBio);
        req.add(youtubeSearch);
        loadAd(BuildConfig.DEBUG);

        ft = new ProcessingTask(getBaseContext());
    }

    private void loadAd(boolean isDebug) {
        boolean isL = billyapp.isL;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mAdView.getLayoutParams();
        if(orientation == 2){
            lp.setMargins(0,0,0,0);
            mAdView.setLayoutParams(lp);
        } else if(isL){
            lp.setMargins(0,0,0,billyapp.getDpAsPx(48));
            mAdView.setLayoutParams(lp);
        }
        AdRequest.Builder adBuilder = new AdRequest.Builder();
        if(isDebug){
            adBuilder.addTestDevice("0EF8BB7630D4EDF8936F48900E507AE3");
        }
        AdRequest adRequest = adBuilder.build();
        mAdView.loadAd(adRequest);
    }

    private void performSCRequest(RequestQueue req, String simpleSong, String simpleArtist) throws UnsupportedEncodingException {
        String scUrl = getResources().getString(R.string.soundcloud, (simpleSong + " " + billyapp.UTF8(simpleArtist)).replaceAll(" ", "+"));
        if (appendOrignal) {
            scUrl = getResources().getString(R.string.soundcloud, (simpleSong + " " + billyapp.UTF8(simpleArtist) + " " + "original").replaceAll(" ", "+"));
        }
        StringRequest stringreq = new StringRequest(scUrl, scComplete(), scError());
        req.add(stringreq);
        Log.d(TAG, "scUrl is " + scUrl);
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
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
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
                Log.i(TAG, "some data is null, requests performed again");
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
            Log.e(TAG, e.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private Response.Listener<String> scComplete() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                String[] result = new String[4];
                try {
                    result = ft.parseSoundcloud(response, song, appendOrignal);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (result == null) {
                    appendOrignal = true;
                    try {
                        performSCRequest(billyapp.getRequestQueue(), b.getSimpleSong(), b.getSimpleArtist());
                        return;
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                permaLink = result[2];
                streamLink = result[3];
                b.setStreamLink(streamLink);
                if (result[1] == null) {
                    Crashlytics.log(Log.ERROR, TAG, "Duration is null, for song: " + song);
                }
                b.setDuration(Long.parseLong(result[1]));
                Log.d(TAG, "User is " + result[0] + " duration is " + result[1]);
                Log.d(TAG, "original streamLink is " + streamLink);
                Log.d(TAG, "original permaLink is " + permaLink);
                supportInvalidateOptionsMenu();
                if (scaleAnim != null) {
                    streamBtn.startAnimation(scaleAnim);
                }
                streamBtn.setVisibility(View.VISIBLE);
                RelativeLayout attribution = (RelativeLayout) findViewById(R.id.attribution);
                attribution.setVisibility(View.VISIBLE);
                ((TextView) attribution.getChildAt(0)).setText(result[0] + " on SoundCloud");
                attribution.getChildAt(0).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(permaLink)));
                    }
                });
                if (result[1] != null) {
                    ((TextView) attribution.getChildAt(1)).setText(DurationFormatUtils.formatDuration(b.getDuration(), "mm:ss", true));
                }

            }
        };
    }

    private Response.ErrorListener scError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, volleyError.toString());
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
                    Log.e(TAG, e.toString());
                }
            }
        };
    }

    private Response.ErrorListener lastFmBioError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, volleyError.toString());
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
                    //Log.e(TAG, e.toString());
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
                    JSONArray items = jsonObject.getJSONArray("items");
                    String firstWord = b.getSong().split(" ", 2)[0].toLowerCase();
                    for(int i=0;i<items.length();i++)
                    {
                     JSONObject obj = items.getJSONObject(i);
                     if(obj.getJSONObject("snippet").getString("title").toLowerCase().contains(firstWord)){
                         videoId = obj.getJSONObject("id").getString("videoId");
                         thumbnail = obj.getJSONObject("snippet").getJSONObject("thumbnails").getJSONObject("high").getString("url");
                         break;
                     }
                    }
                    if(videoId == null){    // fallback
                        videoId = items.getJSONObject(0).getJSONObject("id").getString("videoId");
                        thumbnail = items.getJSONObject(0).getJSONObject("snippet").getJSONObject("thumbnails").getJSONObject("high").getString("url");
                    }

                    setYoutube(thumbnail, videoId);
                } catch (JSONException e) {
                    Log.d(TAG, e.toString());
                }
            }
        };
    }

    private Response.ErrorListener youtubeSearchError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, volleyError.toString());
            }
        };
    }

    private void setLastFmBio() {
        ((TextView) findViewById(R.id.artistTitle)).setText(artist);
        ((TextView) findViewById(R.id.artistBio)).setText(lastFmBio);
        (findViewById(R.id.artistInfo)).setVisibility(View.VISIBLE);
        //(findViewById(R.id.topAlbums)).setBackgroundColor(paletteColor);
    }

    private void setRelatedAlbums() {
        Resources res = getResources();
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.topAlbumImages);
        for (int i = 0; i < relatedAlbums.length; i++) {
            int id = res.getIdentifier("relatedImage" + i, "id", getPackageName());
            ((NetworkImageView) relativeLayout.findViewById(id)).setImageUrl(relatedAlbumImg[i], imgload);

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
        NetworkImageView youTubeThumbnail = (NetworkImageView) findViewById(R.id.youTubeThumbnail);
        youTubeThumbnail.setImageUrl(thumbnail, imgload);
        findViewById(R.id.youTube).setVisibility(View.VISIBLE);
        final ImageButton youTubePlay = (ImageButton) findViewById(R.id.youTubePlay);
        View.OnTouchListener ytListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        youTubePlay.setAlpha(1f);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        youTubePlay.setAlpha(.8f);
                        break;
                    case MotionEvent.ACTION_UP:
                        youTubePlay.setAlpha(.8f);
                        openYoutube(videoId);
                        break;
                }
                return true;
            }
        };
        youTubeThumbnail.setOnTouchListener(ytListener);
        youTubePlay.setOnTouchListener(ytListener);
    }

    void openYoutube(String videoId) {
        if (YouTubeIntents.isYouTubeInstalled(getBaseContext())) {
            if (YouTubeIntents.canResolvePlayVideoIntent(getBaseContext())) {
                Intent intent = YouTubeIntents.createPlayVideoIntentWithOptions(getBaseContext(), videoId, true, true);
                if (isMusicPlaying && PlayerService.isRunning) {
                    mService.pauseMedia();
                }
                if (!isPreparing) {
                    startActivity(intent);
                }
            } else {
                Toast.makeText(getBaseContext(), "Please update the YouTube app.",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getBaseContext(), "Please install YouTube app to watch videos.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Bind this activity and PlayerService
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Service connected");
            PlayerService.PlayerServiceBinder binder = (PlayerService.PlayerServiceBinder) service;
            mService = binder.getService();

            isMusicPlaying = mService.bp.isPlaying();
            if (PlayerService.isRunning && !PlayerService.isIdle) {
                Log.i(TAG, "Service is running and is not idle");
                if (isThisSongOn()) {
                    setSeekBar();
                    if (isMusicPlaying) {
                        streamBtn.setImageDrawable(pauseIcon);
                    }
                }
            }


            binder.setListener(new PlayerService.onBPChangedListener() {
                @Override
                public void onPrepared() {
                    if (isThisSongOn()) {
                        dashes.clearAnimation();
                        dashes.setVisibility(View.GONE);
                        //setSeekBar();
                        streamBtn.setImageDrawable(pauseIcon);
                    }
                    isMusicPlaying = true;
                    isPreparing = false;
                }

                // Only onCompletion and onError are called if streamLink is wrong (404, etc.)
                @Override
                public void onCompletion() {
                    if (isThisSongOn()) {
                        streamBtn.setImageDrawable(playIcon);
                        seekBar.setVisibility(View.GONE);
                    }
                    stopTh = true;
                    isMusicPlaying = false;
                }

                @Override
                public void onError(int i, int i2) {
                    Toast.makeText(getBaseContext(), "An error has occurred. " + i + " " + i2,
                            Toast.LENGTH_LONG).show();
                    Log.i(TAG, "OnError");
                    if (isThisSongOn()) {
                        dashes.clearAnimation();
                        dashes.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onMediaStop() {
                    Log.i(TAG, "OnStop");
                    onCompletion();
                }

                @Override
                public void onMediaPause() {
                    if (isThisSongOn()) {
                        streamBtn.setImageDrawable(playIcon);
                        stopTh = true;
                    }
                    isMusicPlaying = false;
                }

                @Override
                public void onMediaPlay() {
                    if (isThisSongOn()) {
                        Log.d(TAG, "onmediaplay");
                        setSeekBar();
                        streamBtn.setImageDrawable(pauseIcon);
                        seekBar.setVisibility(View.VISIBLE);
                    }
                    isMusicPlaying = true;
                }

                @Override
                public void onNotificationStopPressed() {
                    onCompletion();
                    if (mBound) {
                        unbindService(mConnection);
                        mBound = false;
                    }
                }

            });
            mBound = true;
        }
    };

    private boolean isThisSongOn() {
        return song.equals(mService.song) && b.getArtist().equals(mService.artist);
    }

    private void setSeekBar() {
        if (progressThread != null) {
            Log.d(TAG, "thread not null");
            stopTh = true;
        }
        progressThread = new Thread(progress);
        progressThread.start();
        songLength = mService.getDuration() / 1000;
        secondaryProgressFactor = (float) songLength / 100;
        Log.d(TAG, "songlength, secondary " + songLength + " " + secondaryProgressFactor);
        seekBar.setMax(songLength);
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
                    if (PlayerService.isRunning) {
                        if (isThisSongOn()) {
                            Log.i(TAG, "Song matched");
                            if (!isMusicPlaying) {
                                mService.playMedia();
                            } else {
                                mService.pauseMedia();
                            }
                        } else {
                            streamTrack();
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
            isPreparing = true;
            dashes.setVisibility(View.VISIBLE);
            dashes.startAnimation(rotateAnim);
            serviceIntent.putExtra("item", b);
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
                    progressThread = null;
                    stopTh = false;
                    return;
                } else if (PlayerService.isRunning) {
                    if (!PlayerService.isIdle) {
                        seekBar.setProgress(mService.bp.getCurrentPosition() / 1000);
                        seekBar.setSecondaryProgress((int) (mService.bufferPercent * secondaryProgressFactor));
                        if (seekBar.getProgress() == mService.getDuration() / 1000) {
                            Log.i(TAG, "killing thread");
                            stopTh = true;
                        }
                        Log.d(TAG, "Max " + seekBar.getMax() + " progress " + seekBar.getProgress() + " and secondary " + seekBar.getSecondaryProgress());
                    }
                }
            } catch (IllegalStateException e) {
                Log.d(TAG, e.toString());
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
            final int headerHeight = findViewById(R.id.artwork).getHeight() - getSupportActionBar().getHeight() - 500;
            float scrollOpacity = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
            final int newAlpha = (int) (scrollOpacity * 255);
            mActionBarBackgroundDrawable.setAlpha(newAlpha);
            //Log.d(TAG,"headerheight "+ headerHeight);
            //Log.d(TAG, "scroll opacity "+ scrollOpacity);
            //Log.d(TAG, "new alpha "+ newAlpha);
            tintManager.setStatusBarAlpha(scrollOpacity);
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
                tintManager.setStatusBarAlpha(0.0f);
                tintManager.setTintColor(getResources().getColor(R.color.billy));
            }
        }
    };

    /**
     * Most of the ActionBar customization is done using /values[-v19]/styles.xml, remaining stuff is here
     * Transit ActionBar and status bar colors if running JellyBean 4.2 or newer
     * Apply parallax scrolling
     */

    private void setToolbar(int paletteColor) {
        bar = (Toolbar) findViewById(R.id.toolbar);
        //bar.setBackgroundColor(paletteColor);

        setSupportActionBar(bar);

        getSupportActionBar().setTitle(song);
        bar.setNavigationIcon(R.drawable.up);
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_solid));
            getBaseContext().setTheme(R.style.Theme_Billy);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mActionBarBackgroundDrawable = getResources().getDrawable(R.drawable.ab_solid);
            mActionBarBackgroundDrawable.setAlpha(0);
            //getSupportActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);
            bar.setBackground(mActionBarBackgroundDrawable);
        }

        ((NotifyingScrollView) findViewById(R.id.scroll_view)).setOnScrollChangedListener(mOnScrollChangedListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem shareItem = menu.findItem(R.id.share);

        ShareActionProvider actionProv = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        actionProv.setShareIntent(getShareIntent());
        return true;
    }

    /**
     * Make intent for share button
     */
    private Intent getShareIntent() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, getIntentText());
        shareIntent.setType(HTTP.PLAIN_TEXT_TYPE);
        return shareIntent;
    }

    /**
     * Append SoundCloud permalink to intent text, if it's not null
     */
    private String getIntentText() {
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

    /**
     * We unbind service on pause because we might be getting a new intent
     */
    @Override
    protected void onPause() {
        mAdView.pause();
        super.onPause();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeBonding();
        active = false;
    }

    /**
     * Unbind service and Activity
     */

    private void removeBonding() {
        if (mBound) {
            try {
                unbindService(mConnection);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, e.toString());
            }
            mBound = false;
        }
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
        mService.bp.seekTo(seekBar.getProgress() * 1000);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean bool) {
    }
}
