package com.vibin.billy;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.viewpagerindicator.IconPagerAdapter;

import java.util.Arrays;

public class CustomFragmentAdapter extends FragmentPagerAdapter implements IconPagerAdapter {

    private static final String TAG = CustomFragmentAdapter.class.getSimpleName();
    protected static String[] content;
    private int mCount;

    static Context c;

    public CustomFragmentAdapter(FragmentManager fm, Context c) {
        super(fm);
        BillyApplication billyapp = BillyApplication.getInstance();
        CustomFragmentAdapter.c = c;
        content = billyapp.getScreensList();
        mCount = content.length;
    }

    @Override
    public int getIconResId(int index) {
        return 0;
    }

    /**
     * We get the dynamic index of Fragment by searching it in the default {@code R.array.screens} array
     *
     * @param position refers to the Fragment requested by user
     */
    public static Fragment newInstance(int position) {
        position = Arrays.asList(c.getResources().getStringArray(R.array.screens)).indexOf((content[position]));
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
