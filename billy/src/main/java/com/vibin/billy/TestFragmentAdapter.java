package com.vibin.billy;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import com.viewpagerindicator.IconPagerAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TestFragmentAdapter extends FragmentPagerAdapter implements IconPagerAdapter, OnTaskCompleted{

    private static final String TAG = "TestFragmentAdapter";
    protected static final String[] CONTENT = new String[] {
            "Most Popular", "Pop", "Rock","Dance"
    };
    private int mCount = CONTENT.length;
    String searchparam = "dark+horse";
    String result;
    String uri = "http://itunes.apple.com/search?term="+searchparam;

    public TestFragmentAdapter(FragmentManager fm) throws JSONException {
        super(fm);

        //fetch billboard rss

        //fetch itunes json
        FetchTask ft = new FetchTask(this);
        ft.execute(uri);
    }

    @Override
    public void onTaskCompleted(String result) throws JSONException {
        Log.i(TAG,"The JSON is: "+result);

        //parse billboard rss

        //parse itunes json
        JSONObject jsobj = new JSONObject(result);
        JSONArray jsarr = jsobj.getJSONArray("results");
        Log.i(TAG,jsarr.getJSONObject(0).getString("trackName"));
        Log.i(TAG,jsarr.getJSONObject(0).getString("collectionName"));

    }

    public TestFragmentAdapter(android.support.v4.app.Fragment fragment)
    {
        super(fragment.getChildFragmentManager());

    }


    @Override
    public int getIconResId(int index) {
        // for setting icons
        return 0;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = new Fragment1();
        switch(position){
            case 0:
                fragment = new Fragment1();
                Log.i(TAG,"fragment 1 is called");
                break;
            case 1:
                fragment = new Fragment2();
                Log.i(TAG,"fragment 2 is called");
                break;
            case 2:
                fragment = new Fragment3();
                Log.i(TAG,"fragment 3 is called");
                break;
            case 3:
                fragment = new Fragment4();
                Log.i(TAG,"fragment 4 is called");
                break;
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public CharSequence getPageTitle(int position){
        String title = "";
        switch(position){
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

    public void setCount(int count){
        if (count > 0 && count < 10){
            mCount = count;
            notifyDataSetChanged();
        }
    }
}
