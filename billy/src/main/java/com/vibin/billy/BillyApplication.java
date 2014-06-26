package com.vibin.billy;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;


public class BillyApplication extends Application {
    private static int DISK_IMAGECACHE_SIZE = 1024 * 1024 * 10;
    private static Bitmap.CompressFormat DISK_IMAGECACHE_COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;
    private static int DISK_IMAGECACHE_QUALITY = 100;  //PNG is lossless so quality is ignored but must be provided
    RequestQueue req;
    ImageLoader.ImageCache imageCache;
    ImageLoader imageLoader;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        Log.d(getClass().getName(), "This is init");
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
            Log.d(getClass().getName(), "Request Queue object is not initialized");
            req = Volley.newRequestQueue(this);
        }
        return req;
    }

    public int getBillySize() {
        return 20;
    }

}
