package com.vibin.billy;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import com.viewpagerindicator.IconPagerAdapter;

public class CustomFragmentAdapter extends FragmentPagerAdapter implements IconPagerAdapter {

    private static final String TAG = CustomFragmentAdapter.class.getSimpleName();
    protected static final String[] CONTENT = new String[]{
            "Most Popular", "Pop", "Rock", "Dance"
    };
    private int mCount = CONTENT.length;
    Context c;

    public CustomFragmentAdapter(FragmentManager fm, Context c) {
        super(fm);
        this.c = c;
    }

    public CustomFragmentAdapter(android.support.v4.app.Fragment fragment) {
        super(fragment.getChildFragmentManager());

    }

    @Override
    public int getIconResId(int index) {
        return 0;
    }

    public static Fragment newInstance(int position) {
        Fragment1 f = new Fragment1();
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
        String title = "";
        switch (position) {
            case 0:
                title = "Most Popular";
                break;
            case 1:
                title = "Pop";
                break;
            case 2:
                title = "Rock";
                break;
            case 3:
                title = "Dance";
                break;
        }

        return title;
    }

    public void setCount(int count) {
        if (count > 0 && count < 10) {
            mCount = count;
            notifyDataSetChanged();
        }
    }

    @Override
    public Fragment getItem(int position) {
        return newInstance(position);
    }
}
