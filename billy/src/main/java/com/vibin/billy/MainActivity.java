package com.vibin.billy;


import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.viewpagerindicator.*;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    ViewPager mPager;
    CustomFragmentAdapter mAdapter;
    TitlePageIndicator mIndicator;
    View customActionView;
    MenuItem nowplaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        customActionBar();

        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setTintColor(getResources().getColor(R.color.billyred));

        mAdapter = new CustomFragmentAdapter(getSupportFragmentManager(), this);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
    }

    private void customActionBar() {
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayShowCustomEnabled(true);

        customActionView = getLayoutInflater().inflate(R.layout.custom_actionbar, null);
        getActionBar().setCustomView(customActionView);
        setTitle("Billy");
    }

    // Override this activity's setTitle method
    @Override
    public void setTitle(CharSequence title) {
        getActionBar().setTitle("");
        ((TextView) customActionView.findViewById(R.id.title)).setText(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.main, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        nowplaying = menu.findItem(R.id.nowplaying);
        nowplaying.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.nowplaying:
                Toast.makeText(getBaseContext(), "This is now playing",
                        Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_settings:
                Toast.makeText(getBaseContext(), "This is settings",
                        Toast.LENGTH_LONG).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

//TODO handle 2G/3G devices efficiently
//TODO About page
//TODO Intelligent SoundCloud track fetch