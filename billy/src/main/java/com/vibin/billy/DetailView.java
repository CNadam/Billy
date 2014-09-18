package com.vibin.billy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
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
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class DetailView extends SuperActivity implements SeekBar.OnSeekBarChangeListener {
    String song, artwork, artist, album, streamLink, lastFmBio, thumbnail, videoId;
    String[] relatedAlbumImg, relatedAlbums;
    int songIndex, songLength;
    boolean isMusicPlaying, stopTh, isCurrentSongBG, isLoadingAnimOn;
    static boolean active, mBound;
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
    ImageLoader imgload;
    RequestQueue req;
    PlayerService mService;
    Handler progressHandler;
    Thread progressThread;
    float secondaryProgressFactor;

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
        if (artist.contains(",")) {
            artist = artist.substring(0, artist.indexOf(","));
        }
        album = itemData.getString("album");
        artwork = itemData.getString("artwork");
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (Integer.parseInt(pref.getString("albumArtQuality", "1")) == 2) {
            artwork = artwork.replaceAll("400x400", "600x600");
        }
        songIndex = itemData.getInt("index");

        relatedAlbumImg = new String[3];
        relatedAlbums = new String[3];
        streamBtn = (ImageButton) findViewById(R.id.streamButton);
        dashes = (ImageView) findViewById(R.id.dashes);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setAlpha(0.85f);
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

        dashes.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                dashes.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                animate();
            }
        });

        if (savedInstanceState == null) {
            performRequests();
        }

        customActionBar();

        hero = (NetworkImageView) findViewById(R.id.image_header);
        hero.setImageUrl(artwork, imgload);

        serviceIntent = new Intent(this, PlayerService.class);
        if (PlayerService.isRunning) {
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

/*    @Override
    public void enableSwipeToDismiss() {
        Log.d(TAG,"Swipe to dismiss enabled. ");
        super.enableSwipeToDismiss();
    }*/

    private void performRequests() {
        super.enableSwipeToDismiss();
        req = billyapp.getRequestQueue();

        final String scUrl = getResources().getString(R.string.soundcloud) + artist.replaceAll(" ", "+").replaceAll("\u00eb", "e") + "+" + song.replaceAll(" ", "+") + getResources().getString(R.string.sc_params);
        final String lastFmBioUrl = "http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=" + artist.replaceAll(" ", "+") + "&autocorrect=1&api_key=67b01760e70bb90ff51ae8590b3c2ba8&format=json";
        final String lastFmTopAlbumsUrl = "http://ws.audioscrobbler.com/2.0/?method=artist.gettopalbums&artist=" + artist.replaceAll(" ", "+") + "&autocorrect=1&limit=3&api_key=67b01760e70bb90ff51ae8590b3c2ba8&format=json";
        final String youtubeUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=" + artist.replaceAll(" ", "+").replaceAll("\u00eb", "e") + "+" + song.replaceAll(" ", "+").replaceAll("#", "") + "&maxResults=2&type=video&key=" + youtubeKey;
        StringRequest stringreq = new StringRequest(Request.Method.GET, scUrl, scComplete(), scError());
        JsonObjectRequest lastFmBio = new JsonObjectRequest(Request.Method.GET, lastFmBioUrl, null, lastFmBioComplete(), lastFmBioError());
        JsonObjectRequest lastFmTopAlbums = new JsonObjectRequest(Request.Method.GET, lastFmTopAlbumsUrl, null, lastFmTopAlbumsComplete(), lastFmTopAlbumsError());
        JsonObjectRequest youtubeSearch = new JsonObjectRequest(Request.Method.GET, youtubeUrl, null, youtubeSearchComplete(), youtubeSearchError());

        //Log.d(TAG, "scUrl is " + scUrl.substring(0, 100) + "...");
        Log.d(TAG, "scUrl is " + scUrl);
        Log.d(TAG, "topalbum " + lastFmTopAlbumsUrl);
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
        outState.putString("thumbnail", thumbnail);
        outState.putString("videoId", videoId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        lastFmBio = savedInstanceState.getString("lastFmBio");
        streamLink = savedInstanceState.getString("streamLink");
        relatedAlbums = savedInstanceState.getStringArray("relatedAlbums");
        relatedAlbumImg = savedInstanceState.getStringArray("relatedAlbumImg");
        thumbnail = savedInstanceState.getString("thumbnail");
        videoId = savedInstanceState.getString("videoId");
        try {
            if (lastFmBio.isEmpty() || streamLink.isEmpty() || Arrays.asList(relatedAlbumImg).contains(null) || Arrays.asList(relatedAlbumImg).contains(null) || thumbnail.isEmpty()) {
                Log.d(TAG, "some data is null, requests performed again");
                performRequests();
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
                    streamLink = ft.parseSoundcloud(response);
                    JsonObjectRequest i1 = new JsonObjectRequest(Request.Method.GET, "https://api.soundcloud.com/i1/tracks/133433134/streams?client_id=apigee", null, i1Complete(), i1Error());
//                    req.add(i1);
                    if (scaleAnim != null) {
                        streamBtn.startAnimation(scaleAnim);
                    }
                    streamBtn.setVisibility(View.VISIBLE);
                    Log.d(TAG, "original streamLink is " + streamLink);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
            }

            private Response.Listener<JSONObject> i1Complete() {
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
            }

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

    private void setYoutube(String thumbnail, final String videoId) {
        final NetworkImageView youTubeThumbnail = (NetworkImageView) findViewById(R.id.youTubeThumbnail);
        youTubeThumbnail.setImageUrl(thumbnail, imgload);
        ((RelativeLayout) findViewById(R.id.youTube)).setVisibility(View.VISIBLE);
        final ImageButton youTubePlay = (ImageButton) findViewById(R.id.youTubePlay);
        View.OnTouchListener opacityListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (view == youTubePlay || view == youTubeThumbnail) {
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
                        if (isMusicPlaying && PlayerService.isRunning) {
                            mService.doPause();
                        }
                        if (!isLoadingAnimOn) {
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

            isMusicPlaying = mService.bp.isPlaying();
            if (PlayerService.isRunning && !PlayerService.isIdle) {
                Log.d(TAG, "Service is running and is not idle - onServiceConnected");
                if (song.equalsIgnoreCase(mService.song)) {
                    isCurrentSongBG = true;
                    setSeekBar();
                    if (isMusicPlaying) {
                        streamBtn.setImageDrawable(pauseIcon);
                    }
                }
            }

            binder.setListener(new PlayerService.onBPChangedListener() {
                @Override
                public void onPrepared(int duration) {
                    isMusicPlaying = true;
                    dashes.clearAnimation();
                    isLoadingAnimOn = false;
                    dashes.setVisibility(View.GONE);
                    songLength = duration;
                    setSeekBar();
                    streamBtn.setImageDrawable(pauseIcon);
                    isCurrentSongBG = true;
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
                    Toast.makeText(getBaseContext(), "An error has occurred. " + i + " " + i2,
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
                if (!isLoadingAnimOn) {
                    if (isCurrentSongBG) {
                        if (!isMusicPlaying) {
                            Log.d(TAG, "Song matched");
                            if (PlayerService.isRunning) {
                                mService.playMedia();
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


    void streamTrack() {
        if (streamLink != null) {
            isMusicPlaying = true;
            dashes.setVisibility(View.VISIBLE);
            dashes.startAnimation(rotateAnim);
            isLoadingAnimOn = true;
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
                    if (seekBar.getProgress() == mService.bp.getDuration() / 1000) {
                        stopTh = true;
                    }
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
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowCustomEnabled(true);

        customActionView = getLayoutInflater().inflate(R.layout.custom_actionbar, null);
        getActionBar().setCustomView(customActionView);
        setTitle(song);

        /**
         * Transit action bar color if running JellyBean 4.2 or newer
         */
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            Log.d(TAG, "LOL");
            getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_solid_billy));
            getBaseContext().setTheme(R.style.Theme_Billy);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mActionBarBackgroundDrawable = getResources().getDrawable(R.drawable.ab_solid_billy);
            mActionBarBackgroundDrawable.setAlpha(1);
            getActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);
            actionBarText = (TextView) customActionView.findViewById(R.id.title);
            actionBarText.setAlpha(0);

            tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setTintColor(getResources().getColor(R.color.billyred));
            tintManager.setTintAlpha(0);

            ((NotifyingScrollView) findViewById(R.id.scroll_view)).setOnScrollChangedListener(mOnScrollChangedListener);
        }
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
        seekBar.setAlpha(0.85f);
        mService.bp.seekTo(seekBar.getProgress() * 1000);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
    }
}
