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

import java.io.File;


public class BillyApplication extends Application {
    RequestQueue req;
    ImageLoader.ImageCache imageCache;
    ImageLoader imageLoader;
    String[] userScreensList;
    private static BillyApplication mInstance;
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
        Crashlytics.start(this);
        getRequestQueue();
        createImageLoader();
        getScreensList();
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
     * Get screens list from Settings, if set, or else provide default set of screen names
     *
     * @return Array containing list of Strings of screen names
     */

    public String[] getScreensList() {
        if(userScreensList == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            String line = pref.getString("screens", "Most Popular.Pop.Rock.Dance.");
            userScreensList = line.split("\\.");
            for (String i : userScreensList) {
                Log.d(TAG, "billyapp " + i);
            }
        }
        return userScreensList;
    }

    public int getBillySize() {
        return 20;
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
            Log.v("x", desc);

            if (v instanceof ViewGroup) {
                printViewHierarchy((ViewGroup) v, desc);
            }
        }
    }

    public View getActionBarView(Window window) {
        View decorView = window.getDecorView();
        int resId = getResources().getIdentifier("action_bar_container", "id", this.getPackageName());
        return decorView.findViewById(resId);
    }
}
