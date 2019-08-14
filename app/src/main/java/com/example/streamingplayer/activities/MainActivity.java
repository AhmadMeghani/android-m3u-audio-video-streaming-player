package com.example.streamingplayer.activities;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import com.example.streamingplayer.R;
import com.example.streamingplayer.interfaces.IFragmentListener;
import com.example.streamingplayer.interfaces.ISearch;
import com.example.streamingplayer.tabs.TabRadio;
import com.example.streamingplayer.tabs.TabTV;
import com.example.streamingplayer.utils.PageAdapter;

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener, SearchView.OnQueryTextListener, IFragmentListener {
    public static final String PREFS_NAME = "streaming_player";
    private SharedPreferences prefs;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private PageAdapter adapter;
    private MenuItem searchMenuItem;
    private ArrayList<ISearch> iSearch = new ArrayList<>();
    private String searchText;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean("settings_exist", false) == false) //starts SettingsActivity to do settings TODO
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));

        viewPager = (ViewPager) findViewById(R.id.pager);
        setupViewPager(viewPager);
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupViewPager(ViewPager viewPager) { //add tabs here ;)
        adapter = new PageAdapter(getSupportFragmentManager());
        adapter.addFragment(TabTV.newInstance(searchText),"TV");
        adapter.addFragment(TabRadio.newInstance(searchText),"Radio");
        viewPager.setAdapter(adapter);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.action_about:
                final Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.dialog_about);
                dialog.setTitle("Streaming-Player");
                TextView heading = (TextView) dialog.findViewById(R.id.about_heading);
                heading.setText("Streaming-Player");
                TextView text = (TextView) dialog.findViewById(R.id.about_text);
                text.setText("Version: v1.0.0 \n\nCopyright \u00a9 2018 martinjohannes93");
                Button dialogButton = (Button) dialog.findViewById(R.id.about_ok);
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onQueryTextChange(String newText) {
        this.searchText = newText;

        for (ISearch iSearchLocal : this.iSearch)
            iSearchLocal.onTextQuery(newText);
        return true;
    }

    public void onTabSelected(TabLayout.Tab tab) { viewPager.setCurrentItem(tab.getPosition()); }
    public void addiSearch(ISearch iSearch) {
        this.iSearch.add(iSearch);
    }
    public void removeISearch(ISearch iSearch) {
        this.iSearch.remove(iSearch);
    }
    public boolean onQueryTextSubmit(String query) { return false; }
    public void onTabUnselected(TabLayout.Tab tab) { }
    public void onTabReselected(TabLayout.Tab tab) { }
}