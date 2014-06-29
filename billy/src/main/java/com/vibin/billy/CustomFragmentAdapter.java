package com.vibin.billy;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
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

    @Override
    public Fragment getItem(int position) {
        ListFragment fragment = new Fragment1();
        switch (position) {
            case 0:
                fragment = new Fragment1();
                Log.d(TAG, "fragment 1 is called");
                break;
            case 1:
                fragment = new Fragment2();
                Log.d(TAG, "fragment 2 is called");
                break;
            case 2:
                fragment = new Fragment3();
                Log.d(TAG, "fragment 3 is called");
                break;
            case 3:
                fragment = new Fragment4();
                Log.d(TAG, "fragment 4 is called");
                break;
        }
        return fragment;
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
}
