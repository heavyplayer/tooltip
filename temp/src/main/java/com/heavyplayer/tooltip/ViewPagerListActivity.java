package com.heavyplayer.tooltip;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ViewPagerListActivity extends FragmentActivity {
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pager);

        mPager = (ViewPager)findViewById(R.id.pager);
        mPagerAdapter = new RandomListPagerAdapter();
        mPager.setAdapter(mPagerAdapter);
    }

    private class RandomListPagerAdapter extends FragmentPagerAdapter {
        public RandomListPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            return new RandomListFragment();
        }

        @Override
        public int getCount() {
            return 5;
        }
    }

    class RandomListFragment extends ListFragment {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            String[] strings = new String[100];
            for(int i = 0; i < strings.length; i++)
                strings[i] = "Tap me!";

            setListAdapter(
                    new ArrayAdapter<String>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            strings));
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Tooltip tooltip = new Tooltip(getActivity());
            tooltip.setColor(Color.GREEN);
            tooltip.setText("Now try to swipe away");
            tooltip.setTextColor(Color.WHITE);
            tooltip.setTarget(v);
            tooltip.show();
        }
    }
}
