package com.itj.jband;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class ScheduleEditActivity extends AppCompatActivity {
    private static final String TAG = ScheduleEditActivity.class.getSimpleName();

    private ScheduleManagementActivity.Schedule mSchedule;

    private int mHour = -1;
    private int mMin = -1;

    private EditText mScheduleName;
    private EditText mScheduleNoti;
    private TextView mScheduleDays;
    private TextView mScheduleTime;
    private CheckBox mCheckBoxReport;

    private boolean[] mSelectedDays = new boolean[]{false, false, false, false, false, false, false};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_edit);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mScheduleName = (EditText)findViewById(R.id.edit_text_schedule_name);
        mScheduleNoti = (EditText)findViewById(R.id.edit_text_notification);
        mScheduleDays = (TextView)findViewById(R.id.text_view_schedule_days);
        mScheduleTime = (TextView)findViewById(R.id.text_view_schedule_time);
        mCheckBoxReport = (CheckBox)findViewById(R.id.checkbox_location);

        LinearLayout daySelect = (LinearLayout)findViewById(R.id.layout_days);
        daySelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DaySelectDialogFragment fragment = new DaySelectDialogFragment();
                fragment.setSelectedItem(mSelectedDays);
                fragment.setItemSelectedListener(new DaySelectDialogFragment.OnItemSelectedListener() {
                    @Override
                    public void onSelectItems(boolean[] items) {
                        setDays(items);
                    }
                });
                fragment.show(getSupportFragmentManager(), DaySelectDialogFragment.class.getSimpleName());

            }
        });

        LinearLayout timeSelect = (LinearLayout)findViewById(R.id.layout_time);
        timeSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GregorianCalendar calendar = new GregorianCalendar();
                int hour = mSchedule != null ? mSchedule.mHour : calendar.get(Calendar.HOUR_OF_DAY);
                int min = mSchedule != null ? mSchedule.mMin : calendar.get(Calendar.MINUTE);
                new TimePickerDialog(ScheduleEditActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int min) {
                        Log.d(TAG, "hour = " + hour + " min = " + min);
                        setTime(hour, min);
                    }
                }, hour, min, false).show();

            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            mSchedule = intent.getParcelableExtra("schedule");
            if (mSchedule != null) {
                initValues();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_schedule_edit, menu);

        MenuItem item = menu.findItem(R.id.action_delete);
        if (mSchedule == null) {
            item.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_delete) {
            deleteSchedule();
        } else if (id == R.id.action_save) {
            saveSchedule();
        }

        return super.onOptionsItemSelected(item);
    }

    private void initValues() {
        mScheduleName.setText(mSchedule.mName);
        mScheduleNoti.setText(mSchedule.mNofi);
        setDays(mSchedule.mDays);
        setTime(mSchedule.mHour, mSchedule.mMin);
        mCheckBoxReport.setChecked(mSchedule.mReportLocation);
    }

    private void setDays(boolean[] days) {
        StringBuilder sb = new StringBuilder();
        String[] dayLabels = getResources().getStringArray(R.array.days_of_week);
        int length = days.length;
        for (int i = 0; i < length; i++) {
            if (days[i]) {
                sb.append(dayLabels[i] + " ");
            }
        }

        mSelectedDays = days;
        mScheduleDays.setText(sb.toString());
    }

    private void setTime(int hour, int min) {
        mHour = hour;
        mMin = min;
        boolean pm = hour > 12;
        StringBuilder sb = new StringBuilder();
        if (pm) {
            sb.append(getString(R.string.pm));
        } else {
            sb.append(getString(R.string.am));
        }

        if (hour > 12) {
            hour -= 12;
        }
        sb.append(" " + String.format(getString(R.string.alarm_time_format), hour, min));
        mScheduleTime.setText(sb.toString());
    }

    private void saveSchedule() {
        String name = mScheduleName.getText().toString();
        String noti = mScheduleNoti.getText().toString();
        boolean report = mCheckBoxReport.isChecked();

        if (mSchedule == null) {
            mSchedule = new ScheduleManagementActivity.Schedule();
        }

        mSchedule.mName = name;
        mSchedule.mNofi = noti;
        mSchedule.mDays = mSelectedDays;
        mSchedule.mHour = mHour;
        mSchedule.mMin = mMin;
        mSchedule.mReportLocation = report;

        Intent intent = new Intent();
        intent.putExtra("schedule", mSchedule);
        setResult(RESULT_OK, intent);
        finish();
    }

    void deleteSchedule() {
        Intent intent = new Intent();
        intent.putExtra("schedule", mSchedule);
        intent.putExtra("deleted", true);
        setResult(RESULT_OK, intent);
        finish();
    }

    public static class DaySelectDialogFragment extends DialogFragment {
        private boolean[] mSelectedItems = new boolean[] {false, false, false, false, false, false, false};

        public void setSelectedItem(boolean[] selectedItems) {
            mSelectedItems = selectedItems;
        }

        public interface OnItemSelectedListener {
            public void onSelectItems(boolean[] items);
        }

        private OnItemSelectedListener mItemSelectedListener = null;

        public void setItemSelectedListener(OnItemSelectedListener listener) {
            mItemSelectedListener = listener;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_day_select_popup)
                    .setMultiChoiceItems(R.array.day_labels, mSelectedItems, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            mSelectedItems[which] = isChecked;
                        }
                    })
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mItemSelectedListener != null) {
                                mItemSelectedListener.onSelectItems(mSelectedItems);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null);
            return builder.show();
        }
    }

}
