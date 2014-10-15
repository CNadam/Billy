package com.vibin.billy;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

public class Settings extends Activity {
    SystemBarTintManager tintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);

        /**
         * Detects if the user is using phone in Expanded Desktop/Full Screen mode, on a KitKat+ device
         */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @TargetApi(Build.VERSION_CODES.KITKAT)
                @Override
                public void onSystemUiVisibilityChange(int i) {
                    Toast.makeText(getBaseContext(), "SystemUIchange " + i,
                            Toast.LENGTH_LONG).show();
                    if (i > 0) {
                        tintManager.setStatusBarTintEnabled(false);
                    } else {
                        customActionBar();
                    }

                }
            });
        }

        getFragmentManager().beginTransaction().replace(R.id.settingsLinear, new SettingsFragment()).commit();
        customActionBar();
    }

    private void customActionBar() {
        getActionBar().setDisplayShowTitleEnabled(true);
        setTitle(" " + "Settings".toUpperCase());

        tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setTintColor(getResources().getColor(R.color.billyred));
        tintManager.setTintAlpha(1.0f);
    }

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
