package com.example.android.classscheduler;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by jonathanbarrera on 8/4/18.
 */

public class CategoryFragmentPagerAdapter extends FragmentPagerAdapter {

    // Member variables
    private Context mContext;

    // Constructor
    public CategoryFragmentPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new StudentListFragment();
            case 1:
                return new ClassListFragment();
            default:
                throw new IllegalArgumentException("Invalid fragment adapter position: " + position);
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return mContext.getString(R.string.students);
            case 1:
                return mContext.getString(R.string.classes);
            default:
                throw new IllegalArgumentException("Invalid Page Title position: " + position);
        }
    }

}
