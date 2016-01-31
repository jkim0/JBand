package com.itj.jband;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;

public class ServiceAgreementActivity extends AppCompatActivity {
    private static final String TAG = ServiceAgreementActivity.class.getSimpleName();
    private CheckBox mCheckBoxILove;
    private CheckBox mCheckBoxPersonalInfo;
    private CheckBox mCheckBoxPassInfo;
    private CheckBox mCheckBoxWholeCheck;

    private boolean mTriggerSub = false;

    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_agreement);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mCheckBoxWholeCheck = (CheckBox)findViewById(R.id.checkbox_whole_check);
        mCheckBoxWholeCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (mTriggerSub) return;

                if (checked) {
                    mCheckBoxILove.setChecked(true);
                    mCheckBoxPersonalInfo.setChecked(true);
                    mCheckBoxPassInfo.setChecked(true);
                } else {
                    mCheckBoxILove.setChecked(false);
                    mCheckBoxPersonalInfo.setChecked(false);
                    mCheckBoxPassInfo.setChecked(false);
                }
            }
        });

        mCheckBoxILove = (CheckBox)findViewById(R.id.checkbox_i_love);
        mCheckBoxPersonalInfo = (CheckBox)findViewById(R.id.checkbox_personal_info);
        mCheckBoxPassInfo = (CheckBox)findViewById(R.id.checkbox_pass_info);
        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (!checked) {
                    if (mCheckBoxWholeCheck.isChecked()) {
                        mTriggerSub = true;
                        mCheckBoxWholeCheck.setChecked(false);
                    }
                }
            }
        };

        mCheckBoxILove.setOnCheckedChangeListener(listener);
        mCheckBoxPersonalInfo.setOnCheckedChangeListener(listener);
        mCheckBoxPassInfo.setOnCheckedChangeListener(listener);

        Button buttonNext = (Button)findViewById(R.id.button_next);
        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkAgreement()) {
                    Intent intent = new Intent(ServiceAgreementActivity.this, AuthenticationActivity.class);
                    startActivityForResult(intent, REQUEST_CODE);
                }
            }
        });

        final ScrollView containerScrollView = (ScrollView)findViewById(R.id.agreement_container);
        ScrollView scrollViewILove = (ScrollView)findViewById(R.id.scrollview_i_love_agreement);
        ScrollView scrollViewPersonalInfo = (ScrollView)findViewById(R.id.scrollview_personal_info_agreement);
        ScrollView scrollViewPassInfo = (ScrollView)findViewById(R.id.scrollview_pass_info_agreement);
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                containerScrollView.requestDisallowInterceptTouchEvent(true);
                return false;
            }
        };
        scrollViewILove.setOnTouchListener(touchListener);
        scrollViewPersonalInfo.setOnTouchListener(touchListener);
        scrollViewPassInfo.setOnTouchListener(touchListener);
    }

    private boolean checkAgreement() {
        if (!mCheckBoxILove.isChecked()) {
            return false;
        }

        if (!mCheckBoxPersonalInfo.isChecked()) {
            return false;
        }

        if (!mCheckBoxPassInfo.isChecked()) {
            return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK);
                finish();
            }
        }
    }
}
