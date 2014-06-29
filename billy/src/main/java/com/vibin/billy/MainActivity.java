package com.vibin.billy;


import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.viewpagerindicator.*;

import android.support.v4.app.FragmentActivity;

public class MainActivity extends FragmentActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    ViewPager mPager;
    CustomFragmentAdapter mAdapter;
    TitlePageIndicator mIndicator;
    View customActionView;
    private CharSequence mTitle;
    Context c;

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

        mAdapter = new CustomFragmentAdapter(getSupportFragmentManager(), this);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
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

    public void onListItemClicked(Fragment frag) {
        // FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        // trans.replace(R.id.container,frag).addToBackStack(null);
        // findViewById(R.id.indicator).setVisibility(View.GONE);
        // trans.commit();

        Log.d(TAG, "OnListItemClicked");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //findViewById(R.id.indicator).setVisibility(View.VISIBLE);
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
