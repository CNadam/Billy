package com.vibin.billy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.viewpagerindicator.*;

import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


public class MainActivity extends FragmentActivity {

    ViewPager mPager;
    CustomFragmentAdapter mAdapter;
    TitlePageIndicator mIndicator;

    View customActionView;
    private CharSequence mTitle;

    String searchparam = "dark+horse";
    Context c;
    RequestQueue req;
    String KEY_MDATA ="KEY_MDATA";
    String uri = "http://itunes.apple.com/search?term=" + searchparam + "&limit=1";
    String rssurl ="http://www1.billboard.com/rss/charts/hot-100";
    int mIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get title of activity
        mTitle = getTitle();

        //Set up custom action bar
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayShowCustomEnabled(true);
        customActionBar();

        c = this; // Setting context object

        //Set up status bar tint
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setTintColor(Color.parseColor("#EA5157"));

        try {
            mAdapter = new CustomFragmentAdapter(getSupportFragmentManager(),this);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mIndicator = (TitlePageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);


    }

    private void customActionBar() {
        customActionView = getLayoutInflater().inflate(R.layout.custom_actionbar, null);
        getActionBar().setCustomView(customActionView);
        setTitle("Billy");
    }

    // Override this activity's setTitle method
    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        ((TextView) customActionView.findViewById(R.id.title)).setText(mTitle);
        getActionBar().setTitle("");
    }

    // Inflate the menu; this adds items to the action bar if it is present.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
