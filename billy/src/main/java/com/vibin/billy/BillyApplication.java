package com.vibin.billy;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;

import org.apache.commons.lang3.StringUtils;

import java.io.File;


public class BillyApplication extends Application {
    RequestQueue req;
    ImageLoader.ImageCache imageCache;
    ImageLoader imageLoader;
    String[] genres;
    SharedPreferences pref;
    private static BillyApplication mInstance;
    public static final String defaultGenres = "1Most Popular.1Pop.1Rock.1Dance.1Metal.1R&B.1Country.";
    final int DEFAULT_CACHE_SIZE = 16 * 1024 * 1024; // for DiskBasedCache
    private static final String DEFAULT_CACHE_DIR = "volley";

    private static final String TAG = BillyApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        init();
    }

    /**
     * For classes which do not have Context.getApplication()
     */

    public static BillyApplication getInstance() {
        return mInstance;
    }


    /**
     * Booooom!
     */

    private void init() {
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (!BuildConfig.DEBUG) {
            Crashlytics.start(this);
        }
        checkFirstRun();
        getRequestQueue();
        createImageLoader();
        getGenresList();
    }

    /**
     * Do some stuff when app is run for first time
     */

    private void checkFirstRun() {
     boolean isFirstRun = pref.getBoolean("firstrun",true);
        if(isFirstRun)
        {
            // do some
            SharedPreferences.Editor ed = pref.edit();
            ed.putBoolean("firstrun",false);
            ed.apply();
        }
    }


    private void createImageLoader() {
        if (imageCache == null) {
            imageCache = new BitmapLruCache();
            imageLoader = new ImageLoader(Volley.newRequestQueue(this), imageCache);
        }
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    /**
     * If RequestQueue is null, create one with {@code cacheSize} amount of cache
     */

    public RequestQueue getRequestQueue() {
        if (req == null) {
            req = Volley.newRequestQueue(this);
            File cacheDir = new File(this.getCacheDir(), DEFAULT_CACHE_DIR);
            DiskBasedCache cache = new DiskBasedCache(cacheDir, DEFAULT_CACHE_SIZE);
            req = new RequestQueue(cache, new BasicNetwork(new HurlStack()));
            req.start();
        }
        return req;
    }

    /**
     * Get genres list from Settings, if set, or else provide default set of screen names
     *
     * @return Array containing list of Strings of screen names
     */

    public String[] getGenresList() {
        String genresPref = pref.getString("genres", defaultGenres);
        String[] genresWithCheck = genresPref.split("\\.");
        genres = new String[StringUtils.countMatches(genresPref, "1")];
        int index = 0;
        for (String i : genresWithCheck) {
            Log.d(TAG, i);
            if (i.charAt(0) == '1') {
                genres[index] = i.substring(1);
                index++;
            }
        }
        return genres;
    }

    /**
     * @param billySize Actual number of songs contained in XML. Can be 100/20/15
     * @return the minimum number of songs to be fetched and saved to DB
     */

    public int getMinBillySize(int billySize) {
        if (billySize >= 20) {
            return 20;
        } else {
            return billySize;
        }
    }

    /**
     * To check internet connectivity
     *
     * @return true, if connected to internet
     */

    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    /**
     * @return View associated with Action Bar
     */
    public View getActionBarView(Window window) {
        View decorView = window.getDecorView();
        int resId = getResources().getIdentifier("action_bar_container", "id", this.getPackageName());
        return decorView.findViewById(resId);
    }

    /**
     * Convert display-independent pixels to pixels
     */

    public int getDPAsPixels(int DP) {
        float scale = this.getResources().getDisplayMetrics().density;
        return (int) (DP * scale);
    }

    public static void printViewHierarchy(ViewGroup $vg, String $prefix) {
        for (int i = 0; i < $vg.getChildCount(); i++) {
            View v = $vg.getChildAt(i);
            String desc = $prefix + " | " + "[" + i + "/" + ($vg.getChildCount() - 1) + "] " + v.getClass().getSimpleName() + " " + v.getId();
            Log.d("x", desc);

            if (v instanceof ViewGroup) {
                printViewHierarchy((ViewGroup) v, desc);
            }
        }
    }
}
