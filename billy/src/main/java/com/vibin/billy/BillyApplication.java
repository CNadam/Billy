package com.vibin.billy;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

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
        setCustomExceptionHandler();
    }

    /**
     * Register a custom UncaughtException Handler for dismissing persistent notification on Service's crash
     * Do call the default Android UncaughtException Handler at last, to get the dialog
     */
    private void setCustomExceptionHandler() {
        final NotificationManager noteMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final Thread.UncaughtExceptionHandler defaultExHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler customExHandler = new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                noteMan.cancel(1);
                Log.d(TAG, "Uncaught Exception is");
                throwable.printStackTrace();

                defaultExHandler.uncaughtException(thread, throwable);
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(customExHandler);
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
     * Get screens list from Settings, or else provide default set of screen names
     *
     * @return Array containing list of Strings of screen names
     */

    public String[] getScreensList() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String line = pref.getString("screens", "Most Popular.Pop.Rock.Dance.");
        String[] list = line.split("\\.");
        for (String i : list) {
            Log.d(TAG, "billyapp " + i);
        }
        return list;
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
}
