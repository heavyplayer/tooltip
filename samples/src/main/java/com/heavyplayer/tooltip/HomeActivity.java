package com.heavyplayer.tooltip;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class HomeActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setListAdapter(
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_list_item_1,
                        new String[]{"Buttons", "Buttons Sherlock", "ViewPager List"}));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        switch(position) {
            case 0:
                startActivity(new Intent(this, ButtonsActivity.class));
                break;

            case 1:
                startActivity(new Intent(this, ButtonsSherlockActivity.class));
                break;

            case 2:
                startActivity(new Intent(this, ViewPagerListActivity.class));
                break;

            default:
                break;
        }
    }
}
