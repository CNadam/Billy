package com.vibin.billy;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.viewpagerindicator.IconPagerAdapter;

import java.util.Arrays;
import java.util.List;

public class CustomFragmentAdapter extends FragmentStatePagerAdapter implements IconPagerAdapter {

    private static final String TAG = CustomFragmentAdapter.class.getSimpleName();
    protected static String[] content;
    private int mCount;
    static List<String> resScreens; // Screens list from Resources

    Context c;

    public CustomFragmentAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.c = context;
        Log.d(TAG,"CustomFragmentAdapter constructor");
        BillyApplication billyapp = BillyApplication.getInstance();
        content = billyapp.getScreensList();
        mCount = content.length;
        resScreens = Arrays.asList(c.getResources().getStringArray(R.array.screens));
    }

    @Override
    public int getIconResId(int index) {
        return 0;
    }

    /**
     * We get the dynamic index of Fragment by searching its name in {@code R.array.screens}
     *
     * @param position refers to the Fragment requested by user
     */
    public static Fragment newInstance(int position) {
        position = resScreens.indexOf(content[position]);
        SongsFragment f = new SongsFragment();
        Bundle args = new Bundle();
        args.putInt("position", position);
        f.setArguments(args);
        return f;
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return content[position];
    }

    @Override
    public Fragment getItem(int position) {
        return newInstance(position);
    }
}
