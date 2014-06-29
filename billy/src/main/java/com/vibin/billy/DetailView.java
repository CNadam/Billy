package com.vibin.billy;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

public class DetailView extends Activity {
    String song;
    View customActionView;
    ImageButton imagebtn;
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_view);

        mTitle = getTitle();

        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayShowCustomEnabled(true);
        customActionBar();

        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        //tintManager.setTintColor(Color.parseColor("#EA5157"));
        tintManager.setTintColor(Color.TRANSPARENT);

        imagebtn = (ImageButton) findViewById(R.id.imageButton);
        imagebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast tt = Toast.makeText(getBaseContext(), "Button is clicked", Toast.LENGTH_LONG);
                tt.show();
            }
        });

        song = getIntent().getStringExtra("song");

        Toast.makeText(this, "Song is " + song, Toast.LENGTH_LONG).show();

        //startService();
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
}
