package com.vibin.billy;

import com.android.volley.toolbox.ImageLoader;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

/**
 * Basic LRU Memory cache.
 *
 * @author Trey Robinson
 */
public class BitmapLruCache
        extends LruCache<String, Bitmap>
        implements ImageLoader.ImageCache {

    public BitmapLruCache() {
        this(getDefaultLruCacheSize());
    }

    public BitmapLruCache(int sizeInKiloBytes) {
        super(sizeInKiloBytes);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getRowBytes() * value.getHeight() / 1024;
    }

    @Override
    public Bitmap getBitmap(String url)
    {
      //  Log.d(getClass().getName(), "Grab "+url);
        return get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap)
    {
      //  Log.d(getClass().getName(), "Put "+url);
        put(url, bitmap);
    }

    public static int getDefaultLruCacheSize() {
        final int maxMemory =
                (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        return cacheSize;
    }
}