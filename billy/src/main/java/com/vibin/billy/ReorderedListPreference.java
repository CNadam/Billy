package com.vibin.billy;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.vibin.billy.draglistview.DynamicListView;
import com.vibin.billy.draglistview.StableArrayAdapter;

import java.util.ArrayList;
import java.util.Arrays;

public class ReorderedListPreference extends DialogPreference {
    Context c;
    DynamicListView lv;
    SharedPreferences pref;
    String[] screensWithCheck;
    private static final String TAG = ReorderedListPreference.class.getSimpleName();

    public ReorderedListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.c = context;
        setDialogMessage("Try dragging items in the list!");
        setDialogLayoutResource(R.layout.reorderedlist_preference);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());

        setDialogIcon(null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        //getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getScreensWithCheck();
        String[] screens = new String[screensWithCheck.length];
        int i=0;
        for(String s:screensWithCheck)
        {
                screens[i] = s.substring(1);
                i++;
        }

        ArrayList<String> arrayList = new ArrayList<String>(Arrays.asList(screens));
        StableArrayAdapter adapter = new StableArrayAdapter(c, R.layout.text_view, arrayList);
        lv = (DynamicListView) view.findViewById(R.id.listview);
        lv.setCheeseList(arrayList);
        lv.setAdapter(adapter);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        for(i=0;i<screensWithCheck.length;i++)
        {
            if(screensWithCheck[i].charAt(0)=='1')
            {
                lv.setItemChecked(i,true);
            }
        }

    }

    public void getScreensWithCheck() {
        String defaultScreens="1Most Popular.1Pop.1Rock.1Dance.";
        String screensPref = pref.getString("screens",defaultScreens);
        Log.d(TAG,screensPref);
        screensWithCheck = screensPref.split("\\.");
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String prefLine="";
            int i = 0;
            while (i < lv.getAdapter().getCount()) {
                CheckedTextView item = ((CheckedTextView) lv.getChildAt(i));
                Log.d(TAG, item.getText().toString());
                if (item.isChecked()) {
                    prefLine += "1";
                } else {
                    prefLine += "0";
                }
                prefLine += item.getText().toString()+".";
                Log.d(TAG,"onDialogClosed "+prefLine);
                i++;
            }
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("screens", prefLine);
            Log.d(TAG, "commited " + editor.commit());
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }
}