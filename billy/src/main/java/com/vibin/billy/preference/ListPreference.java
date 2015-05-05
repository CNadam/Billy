package com.vibin.billy.preference;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.vibin.billy.R;

public class ListPreference extends android.preference.ListPreference {

    private static final String TAG = ListPreference.class.getSimpleName();

    public ListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListPreference(Context context) {
        super(context);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        try {
            int divierId = getDialog().getContext().getResources()
                    .getIdentifier("android:id/titleDivider", null, null);
            View divider = getDialog().findViewById(divierId);
            divider.setBackgroundColor(getContext().getResources().getColor(R.color.billy));
        } catch (NullPointerException e) {
            Log.d(TAG, e.toString());
        }
    }
}
