package com.vibin.billy;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;

import com.readystatesoftware.systembartint.SystemBarTintManager;

public class Settings extends Activity {
    SystemBarTintManager tintManager;

    private static final String TAG = Settings.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);

        tintManager = new SystemBarTintManager(this);
        getActionBar().setDisplayShowTitleEnabled(true);
        setTitle(" " + "Settings".toUpperCase());

        ((BillyApplication) getApplication()).getActionBarView(getWindow()).addOnLayoutChangeListener(expandedDesktopListener);
        getFragmentManager().beginTransaction().replace(R.id.settingsLinear, new SettingsFragment()).commit();
    }

    /**
     * Detects if the user is using phone in Expanded Desktop/Fullscreen mode, and toggles tint
     */

    View.OnLayoutChangeListener expandedDesktopListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            int position[] = new int[2];
            view.getLocationOnScreen(position);
            if (position[1] == 0) {
                tintManager.setStatusBarTintEnabled(false);
            } else {
                tintManager.setStatusBarTintEnabled(true);
                tintManager.setStatusBarAlpha(1.0f);
                tintManager.setTintColor(getResources().getColor(R.color.billy));
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public static class SettingsFragment extends PreferenceFragment {
        SharedPreferences pref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
        }

    }
}
