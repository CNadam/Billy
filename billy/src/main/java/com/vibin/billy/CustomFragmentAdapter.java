package com.vibin.billy;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.viewpagerindicator.IconPagerAdapter;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class CustomFragmentAdapter extends FragmentPagerAdapter implements IconPagerAdapter {

    private static final String TAG = "CustomFragmentAdapter";
    protected static final String[] CONTENT = new String[]{
            "Most Popular", "Pop", "Rock", "Dance"
    };
    private int mCount = CONTENT.length;
    String searchparam = "dark+horse";
    Context c;
    RequestQueue req;
    Bundle b1;
    final String KEY_MDATA ="KEY_MDATA";
    String uri = "http://itunes.apple.com/search?term=" + searchparam + "&limit=1";
    String rssurl ="http://www1.billboard.com/rss/charts/hot-100";
    int mIndex;

    public CustomFragmentAdapter(FragmentManager fm, Context c) throws JSONException {
        super(fm);
        this.c = c;
    }



     public CustomFragmentAdapter(android.support.v4.app.Fragment fragment) {
        super(fragment.getChildFragmentManager());

    }


    @Override
    public int getIconResId(int index) {
        return 0;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = new Fragment1();
        switch (position) {
            case 0:
                fragment = new Fragment1();
                Log.d(TAG, "fragment 1 is called");
                break;
            case 1:
                fragment = new Fragment2();
                Log.d(TAG, "fragment 2 is called");
                break;
            case 2:
                fragment = new Fragment3();
                Log.d(TAG, "fragment 3 is called");
                break;
            case 3:
                fragment = new Fragment4();
                Log.d(TAG, "fragment 4 is called");
                break;
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title = "";
        switch (position) {
            case 0:
                title = "Most Popular";
                break;
            case 1:
                title = "Pop";
                break;
            case 2:
                title = "Rock";
                break;
            case 3:
                title = "Dance";
                break;
        }

        return title;
    }

    public void setCount(int count) {
        if (count > 0 && count < 10) {
            mCount = count;
            notifyDataSetChanged();
        }
    }
}
