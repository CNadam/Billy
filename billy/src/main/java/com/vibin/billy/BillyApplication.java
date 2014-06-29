package com.vibin.billy;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;


public class BillyApplication extends Application {
    RequestQueue req;
    ImageLoader.ImageCache imageCache;
    ImageLoader imageLoader;
    ConnectivityManager cm;
    NetworkInfo net;

    private static final String TAG = BillyApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        Log.d(TAG, "This is init");
        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        net = cm.getActiveNetworkInfo();
        req = Volley.newRequestQueue(this);
        createImageLoader();
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

    public RequestQueue getRequestQueue() {
        if (req == null) {
            Log.d(TAG, "Request Queue object is not initialized");
            req = Volley.newRequestQueue(this);
        }
        return req;
    }

    public int getBillySize() {
        return 20;
    }

    public boolean isConnected() {
        if (net == null) {
            return false;
        } else {
            return net.isConnected();
        }
    }
}
