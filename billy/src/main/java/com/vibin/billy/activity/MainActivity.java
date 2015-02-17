package com.vibin.billy.activity;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.vibin.billy.BillyApplication;
import com.vibin.billy.R;
import com.vibin.billy.adapter.FragmentAdapter;
import com.vibin.billy.fragment.ChangelogDialog;

/**
 * The main activity. (no pun intended)
 */

public class MainActivity extends ActionBarActivity {

    SystemBarTintManager tintManager;
    FragmentAdapter mAdapter;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        tintManager = new SystemBarTintManager(this);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setElevation(0);
        setTitle(" " + "Billy".toUpperCase());

        setViewpager();

        BillyApplication billyapp = (BillyApplication) this.getApplication();
        if (billyapp.isFirstRun()) {
            ChangelogDialog dialog = ChangelogDialog.newInstance();
            dialog.show(getFragmentManager(), "change");
            Log.d(TAG, "showing changelog");
        }
        billyapp.getActionBarView(getWindow()).addOnLayoutChangeListener(expandedDesktopListener);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    private void setViewpager() {
        try {
            mAdapter = new FragmentAdapter(getSupportFragmentManager(), this);
            ViewPager mPager = (ViewPager) findViewById(R.id.pager);
            mPager.setAdapter(mAdapter);
            PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
            tabs.setViewPager(mPager);
            tabs.setTextColor(Color.WHITE);
        } catch (IllegalStateException e) {
            Log.e(TAG, e.toString());
        }
    }

    View.OnLayoutChangeListener expandedDesktopListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            int position[] = new int[2];
            view.getLocationOnScreen(position);
            if (position[1] == 0) {
                tintManager.setStatusBarTintEnabled(false);
            } else {
                tintManager.setStatusBarTintEnabled(true);
                tintManager.setTintColor(getResources().getColor(R.color.billy));
                tintManager.setStatusBarAlpha(1.0f);
            }
        }
    };

    /**
     * This gets around strange NullPointerExceptions (when tapping Menu key) on LG devices.
     */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            openOptionsMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
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

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).registerOnSharedPreferenceChangeListener(refreshViewpager);
    }

    /**
     * Refresh ViewPager and CustomFragmentAdapter on change of genres preference
     */

    SharedPreferences.OnSharedPreferenceChangeListener refreshViewpager = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals("genres")) {
                setViewpager();
                mAdapter.notifyDataSetChanged();
            }
        }
    };

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

        /**
         * Open app page in Play Store, if unsuccessful, open URL in browser
         */

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
                    Toast.makeText(getActivity(), "You rock! :)",
                            Toast.LENGTH_LONG).show();
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

//TODO proxy script
//TODO volley cache
//TODO Implement playlists - Use Google's draglistview, with handlers (only), show SC likes, comments count

//TODO ripple for play button
//TODO seekbar not visible on notification click
//TODO better SoundCloud track fetch
//TODO change firstRun preference in BillyApplication
//TODO deleting database needed on first run?
//TODO crash fixes

//TODO use AudioManager in Service
//TODO use i1 endpoint api if needed

//TODO add fastscroll in hot100

//TODO service quits automatically when playing after sometime
//TODO handle 2G/3G devices efficiently API Level 17
//TODO replace default spinner in SongsFragment with Google's swiperefreshlayout
//TODO Use RemoteController and put full screen lock image for KitKat+ devices