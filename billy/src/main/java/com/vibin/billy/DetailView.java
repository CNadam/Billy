package com.vibin.billy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.manuelpeinado.fadingactionbar.FadingActionBarHelper;
import com.readystatesoftware.systembartint.SystemBarTintManager;

public class DetailView extends Activity {
    String song;
    String artwork;
    View customActionView;
    ImageButton imagebtn;
    BillyApplication billyapp;
    ImageLoader imgload;
    NetworkImageView hero;
    private CharSequence mTitle;

    private static final String TAG = DetailView.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FadingActionBarHelper fad = new FadingActionBarHelper();
        fad.actionBarBackground(R.drawable.ab_solid_billy).headerLayout(R.layout.detail_header).contentLayout(R.layout.detail_view).parallax(true);

        setContentView(fad.createView(this));
        fad.initActionBar(this);

        billyapp = (BillyApplication) this.getApplication();
        song = getIntent().getStringExtra("song");
        artwork = getIntent().getStringExtra("artwork").replaceAll("400x400", "600x600");

        mTitle = getTitle();

        getActionBar().setDisplayShowTitleEnabled(false);
        customActionBar();
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowCustomEnabled(true);

        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setTintColor(getResources().getColor(R.color.billyred));
        //tintManager.setTintColor(Color.BLACK);

        imagebtn = (ImageButton) findViewById(R.id.imageButton);
        if(billyapp.isConnected()){
            imgload = billyapp.getImageLoader();
        }
        hero = (NetworkImageView) findViewById(R.id.ed);
        Log.d(TAG, "artwork url is "+artwork);
        hero.setImageUrl(artwork,imgload);

        //startService();
    }

    private void customActionBar() {
        customActionView = getLayoutInflater().inflate(R.layout.custom_actionbar, null);
        getActionBar().setCustomView(customActionView);
        setTitle(song);
    }

    // Override this activity's setTitle method
    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        ((TextView) customActionView.findViewById(R.id.title)).setText(mTitle);
        getActionBar().setTitle("");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case android.R.id.home: // Handle the Up button in Actionbar
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
