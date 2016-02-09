package com.itj.jband;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class ActivityCheckActivity extends AppCompatActivity {
    private static final String TAG = ActivityCheckActivity.class.getSimpleName();

    private Switch mSwitch;
    private TextView mStepText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_check);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSwitch = (Switch)findViewById(R.id.switch_activity_check);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean("activity_check", isChecked);
                editor.commit();
            }
        });
        mStepText = (TextView)findViewById(R.id.today_steps);
        Button buttonInitialize = (Button)findViewById(R.id.button_initialize);
        buttonInitialize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doReset();
            }
        });

        loadData();
    }

    private void loadData() {
        SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        boolean isOn = sp.getBoolean("activity_check", false);
        mSwitch.setChecked(isOn);
    }

    private void doReset() {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
