package com.vibin.billy;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.viewpagerindicator.TitlePageIndicator;

public class MainActivity extends FragmentActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    ViewPager mPager;
    CustomFragmentAdapter mAdapter;
    TitlePageIndicator mIndicator;
    View customActionView;

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, Settings.class);
                startActivity(settingsIntent);
                return true;
            case R.id.about:
                AboutDialog ab = AboutDialog.newInstance();
                ab.show(getSupportFragmentManager(), "ab");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class AboutDialog extends DialogFragment {
        View v;

        public AboutDialog() {
        }

        public static AboutDialog newInstance() {
            AboutDialog frag = new AboutDialog();
            Bundle args = new Bundle();
            frag.setArguments(args);
            return frag;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            v = inflater.inflate(R.layout.dialog_about, container);
            setPlaystoreButton();
            return v;
        }

        private void setPlaystoreButton() {
            v.findViewById(R.id.playStoreButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Uri uri = Uri.parse("market://details?id=" + getActivity().getPackageName());
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    try {
                        startActivity(goToMarket);
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getActivity().getPackageName())));
                    }
                }
            });
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = super.onCreateDialog(savedInstanceState);
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            return dialog;
        }


    }

}

//TODO Allow reordering
//TODO service quits automatically when playing after sometime
//TODO handle 2G/3G devices efficiently API Level 17
//TODO Notification click intent flags
//TODO Intelligent SoundCloud track fetch and HLS implementation
//TODO Change color of notification background


//TODO replace MediaPlayer code with MediaExtractor
//TODO Use RemoteController and put full screen lock image for KitKat+ devices