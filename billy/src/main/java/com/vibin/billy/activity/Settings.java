package com.vibin.billy.activity;

import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.vibin.billy.BillyApplication;
import com.vibin.billy.R;
import com.vibin.billy.fragment.ChangelogDialog;
import com.vibin.billy.fragment.LicensesFragment;
import com.vibin.billy.swipeable.SwipeableActivity;

public class Settings extends SwipeableActivity {
    protected SystemBarTintManager tintManager;

    private static final String TAG = Settings.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.enableSwipeToDismiss();
        setContentView(R.layout.settings_view);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        Toolbar bar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(bar);

        getSupportActionBar().setTitle("Settings");
        bar.setNavigationIcon(R.drawable.up);
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarAlpha(1.0f);
        tintManager.setTintColor(getResources().getColor(R.color.billy));

        bar.addOnLayoutChangeListener(expandedDesktopListener);
        getFragmentManager().beginTransaction().replace(R.id.settingsLinear, new SettingsFragment()).commitAllowingStateLoss();
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
        private Preference rate,changelog,licenses;
        private FragmentManager fm;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            fm = getActivity().getFragmentManager();
            rate = getPreferenceScreen().findPreference("rate");
            changelog = getPreferenceScreen().findPreference("changelog");
            licenses = getPreferenceScreen().findPreference("licenses");
            setOnClickListeners();
        }

        @Override
        public void onResume() {
            super.onResume();
            View rootView = getView();
            if (rootView != null) {
                ListView list = (ListView) rootView.findViewById(android.R.id.list);
                //list.setPadding(0, 0, 0, 0);
                list.setDivider(null);
            }
        }

        private void setOnClickListeners() {
            rate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
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
                    return true;
                }
            });

            changelog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ChangelogDialog dialog = ChangelogDialog.newInstance();
                    dialog.show(fm,"change");
                    return true;
                }
            });

            licenses.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    LicensesFragment dialog = LicensesFragment.newInstance();
                    dialog.show(fm,"licenses");
                    return true;
                }
            });
        }

    }
}
