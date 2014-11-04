package com.vibin.billy;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;

public class CustomListPreference extends ListPreference {

    public CustomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomListPreference(Context context) {
        super(context);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        int divierId = getDialog().getContext().getResources()
                .getIdentifier("android:id/titleDivider", null, null);
        View divider = getDialog().findViewById(divierId);
        divider.setBackgroundColor(getContext().getResources().getColor(R.color.billy));
    }
}
