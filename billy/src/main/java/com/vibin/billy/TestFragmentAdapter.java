package com.vibin.billy;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.viewpagerindicator.IconPagerAdapter;

public class TestFragmentAdapter extends FragmentPagerAdapter implements IconPagerAdapter{
    protected static final String[] CONTENT = new String[] {
            "Most Popular", "Pop", "Rock","Dance"
    };

    private int mCount = CONTENT.length;

    public TestFragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    public TestFragmentAdapter(android.support.v4.app.Fragment fragment)
    {
        super(fragment.getChildFragmentManager());

        // write your code here
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
                break;
            case 1:
                fragment = new Fragment2();
                break;
            case 2:
                fragment = new Fragment3();
                break;
            case 3:
                fragment = new Fragment4();
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
