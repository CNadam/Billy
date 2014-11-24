package com.vibin.billy;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class CustomListPreference extends ListPreference {

    private static final String TAG = CustomListPreference.class.getSimpleName();

    public CustomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomListPreference(Context context) {
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
            Log.d(TAG,e.toString());
        }
    }



}
