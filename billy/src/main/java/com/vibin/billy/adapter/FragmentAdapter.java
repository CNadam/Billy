package com.vibin.billy.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.vibin.billy.BillyApplication;
import com.vibin.billy.R;
import com.vibin.billy.fragment.SongsFragment;

import java.util.Arrays;
import java.util.List;

public class FragmentAdapter extends FragmentStatePagerAdapter {

    private static final String TAG = FragmentAdapter.class.getSimpleName();
    private static String[] content;
    private int mCount;
    private static List<String> resGenres; // Genres list from Resources

    Context c;

    public FragmentAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.c = context;
        Log.d(TAG,"CustomFragmentAdapter constructor");
        BillyApplication billyapp = BillyApplication.getInstance();
        content = billyapp.getGenresList();
        mCount = content.length;
        resGenres = Arrays.asList(c.getResources().getStringArray(R.array.genres));
    }

    /**
     * We get the dynamic index of Fragment by searching its name in {@code R.array.genres}
     *
     * @param position refers to the Fragment requested by user
     */
    public static Fragment newInstance(int position) {
        position = resGenres.indexOf(content[position]);
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
