package com.example.streamingplayer.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.example.streamingplayer.R;

import static com.example.streamingplayer.activities.MainActivity.PREFS_NAME;

public class SettingsActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Adding toolbar to the activity
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean("settings_exist", false) == true) { //set current settings
        }

        Button btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
				//TODO: Validate input first and then save it
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean("settings_exist", true);
				editor.apply();
				finish();
            }
        });
    }
}