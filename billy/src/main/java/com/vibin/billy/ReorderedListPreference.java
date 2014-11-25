package com.vibin.billy;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.vibin.billy.draglistview.DynamicListView;
import com.vibin.billy.draglistview.StableArrayAdapter;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * We save the preference by concatenating each item's check state (1 or 0) and its text, followed by a fullstop.
 * While showing the dialog, we split the String by fullstop, and apply the given checked state.
 * <p/>
 * Default Preference string is {@value com.vibin.billy.BillyApplication#defaultGenres}
 */

public class ReorderedListPreference extends DialogPreference {
    Context c;
    DynamicListView lv;
    SharedPreferences pref;
    String[] genresWithCheck;
    private static final String TAG = ReorderedListPreference.class.getSimpleName();

    public ReorderedListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.c = context;
        setPositiveButtonText(c.getString(R.string.reorder_pref_ok));
        setNegativeButtonText(android.R.string.cancel);
        pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());

        setDialogIcon(null);
    }

    @Override
    protected View onCreateDialogView() {
        super.onCreateDialogView();
        LayoutInflater lif = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return lif.inflate(R.layout.reorderedlist_preference, null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        getGenresWithCheck();
        String[] genres = new String[genresWithCheck.length];
        int i = 0;
        for (String s : genresWithCheck) {
            genres[i] = s.substring(1);
            i++;
        }

        ArrayList<String> arrayList = new ArrayList<String>(Arrays.asList(genres));
        StableArrayAdapter adapter = new StableArrayAdapter(c, R.layout.reorderedlist_row, arrayList);
        lv = (DynamicListView) view.findViewById(R.id.listview);
        lv.setCheeseList(arrayList);
        lv.setAdapter(adapter);

        lv.post(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < genresWithCheck.length; i++) {
                        if (genresWithCheck[i].charAt(0) == '1') {
                            final CheckBox box = (CheckBox) lv.getChildAt(i).findViewById(R.id.checkBox);
                            //Log.d(TAG, i + " is " + box.isChecked());
                            box.setChecked(true);
    /*                        box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                    Log.d(TAG, "before " + box.isChecked());
                                    box.setChecked(!box.isChecked());
                                    Log.d(TAG, "after " + box.isChecked());
                                }
                            });*/
                            box.setOnTouchListener(new View.OnTouchListener() {
                                @Override
                                public boolean onTouch(View v, MotionEvent event) {
                                    Log.d(TAG, "before " + box.isChecked());
                                    box.setChecked(!box.isChecked());
                                    Log.d(TAG, "after " + box.isChecked());
                                    return false;
                                }
                            });
                        }
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG,e.toString());
                }

            }
        });
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

    private void getGenresWithCheck() {
        String genresPref = pref.getString("genres", BillyApplication.defaultGenres);
        Log.d(TAG, genresPref);
        genresWithCheck = genresPref.split("\\.");
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String prefLine = "";
            int i = 0;
            while (i < lv.getAdapter().getCount()) {
                TextView item = ((TextView) lv.getChildAt(i).findViewById(R.id.checkedTV));
                CheckBox box = ((CheckBox) lv.getChildAt(i).findViewById(R.id.checkBox));
                Log.d(TAG, item.getText().toString());
                if (box.isChecked()) {
                    prefLine += "1";
                } else {
                    prefLine += "0";
                }
                prefLine += item.getText().toString() + ".";
                Log.d(TAG, "onDialogClosed " + prefLine);
                i++;
            }
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("genres", prefLine);
            Log.d(TAG, "commited " + editor.commit());
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }
}