package com.vibin.billy.fragment;

/**
 * Copyright 2013 Adam Speakman

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.vibin.billy.BillyApplication;
import com.vibin.billy.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

// TODO If you don't support Android 2.x, you should use the non-support version!


/**
 * Created by Adam Speakman on 24/09/13.
 * http://speakman.net.nz
 */

/**
 * Changes made: switched to non-support classes wherever possible. (to work with PreferenceFragment)
 */

public class LicensesFragment extends DialogFragment {

    private static final String TAG = LicensesFragment.class.getSimpleName();
    private AsyncTask<Void, Void, String> mLicenseLoader;

    private static final String FRAGMENT_TAG = "nz.net.speakman.androidlicensespage.LicensesFragment";

    public static LicensesFragment newInstance() {
        return new LicensesFragment();
    }

    /**
     * Builds and displays a licenses fragment for you. Requires "/res/raw/licenses.html" and
     * "/res/layout/licenses_fragment.xml" to be present.
     *
     * @param fm A fragment manager instance used to display this LicensesFragment.
     */
    public static void displayLicensesFragment(FragmentManager fm) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(FRAGMENT_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = LicensesFragment.newInstance();
        newFragment.show(ft, FRAGMENT_TAG);
    }

    private WebView mWebView;
    private ProgressBar mIndeterminateProgress;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO Extract this title out into your strings resource file.
        getDialog().setTitle("Open Source licenses");
        View view = inflater.inflate(R.layout.licenses_fragment, container, false);
        mIndeterminateProgress = (ProgressBar) view.findViewById(R.id.licensesFragmentIndeterminateProgress);
        mWebView = (WebView) view.findViewById(R.id.licensesFragmentWebView);
        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if(BillyApplication.getInstance().isL) return new AppCompatDialog(getActivity(), R.style.Dialog_Billy);
        else return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            getDialog().setTitle("Open Source Licenses");
            int divierId = getDialog().getContext().getResources()
                    .getIdentifier("android:id/titleDivider", null, null);
            View divider = getDialog().findViewById(divierId);
            divider.setBackgroundColor(this.getResources().getColor(R.color.billy));
        } catch (NullPointerException e) {
            Log.d(TAG, e.toString());
        }
        int width = getResources().getDimensionPixelSize(R.dimen.licensedialog_width);
        int height = getResources().getDimensionPixelSize(R.dimen.licensedialog_height);
        getDialog().getWindow().setLayout(width, height);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadLicenses();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLicenseLoader != null) {
            mLicenseLoader.cancel(true);
        }
    }

    private void loadLicenses() {
        // Load asynchronously in case of a very large file.
        mLicenseLoader = new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                InputStream rawResource = getActivity().getResources().openRawResource(R.raw.licenses);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(rawResource));

                String line;
                StringBuilder sb = new StringBuilder();

                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                    bufferedReader.close();
                } catch (IOException e) {
                    // TODO You may want to include some logging here.
                }

                return sb.toString();
            }

            @Override
            protected void onPostExecute(String licensesBody) {
                super.onPostExecute(licensesBody);
                if (getActivity() == null || isCancelled()) return;
                mIndeterminateProgress.setVisibility(View.INVISIBLE);
                mWebView.setVisibility(View.VISIBLE);
                mWebView.loadDataWithBaseURL(null, licensesBody, "text/html", "utf-8", null);
                mLicenseLoader = null;
            }

        }.execute();
    }
}
