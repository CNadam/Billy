package com.vibin.billy.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vibin.billy.R;

public class ChangelogDialog extends DialogFragment {
    View changelogView;

    private static final String TAG = DialogFragment.class.getSimpleName();
    public static ChangelogDialog newInstance() {
        ChangelogDialog frag = new ChangelogDialog();
        Bundle args = new Bundle();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        changelogView = inflater.inflate(R.layout.changelog_view, container);
        return changelogView;
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            getDialog().setTitle("What's New");
            int divierId = getDialog().getContext().getResources()
                    .getIdentifier("android:id/titleDivider", null, null);
            View divider = getDialog().findViewById(divierId);
            divider.setBackgroundColor(this.getResources().getColor(R.color.billy));
        } catch (NullPointerException e) {
            Log.d(TAG,e.toString());
        }
    }
}
