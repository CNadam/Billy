package com.vibin.billy;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.viewpagerindicator.IconPagerAdapter;

public class CustomFragmentAdapter extends FragmentPagerAdapter implements IconPagerAdapter {

    private static final String TAG = CustomFragmentAdapter.class.getSimpleName();
    protected static final String[] CONTENT = new String[]{
            "Most Popular", "Pop", "Rock", "Dance" //TODO add more genres
    };
    private int mCount = CONTENT.length;
    Context c;

    public CustomFragmentAdapter(FragmentManager fm, Context c) {
        super(fm);
        this.c = c;
    }

    @Override
    public int getIconResId(int index) {
        return 0;
    }

    public static Fragment newInstance(int position) {
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
        return CONTENT[position];
    }

    @Override
    public Fragment getItem(int position) {
        return newInstance(position);
    }
}
