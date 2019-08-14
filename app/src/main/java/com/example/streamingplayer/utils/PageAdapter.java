package com.example.streamingplayer.utils;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class PageAdapter extends FragmentStatePagerAdapter {
    private final List<Fragment> mFragmentList = new ArrayList<>();
    private final List<String> mFragmentTitleList = new ArrayList<>();

    public PageAdapter(FragmentManager manager) {
        super(manager);
    }

    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    public CharSequence getPageTitle(int position) {
        return mFragmentTitleList.get(position);
    }

    public int getCount() {
        return mFragmentList.size();
    }

    public void addFragment(Fragment fragment, String title) {
        mFragmentList.add(fragment);
        mFragmentTitleList.add(title);
    }
}