package com.itj.jband;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.itj.jband.databases.ProviderContract;
import com.itj.jband.schedule.Schedule;
import com.itj.jband.schedule.ScheduleManager;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class ScheduleEditActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ScheduleEditActivity.class.getSimpleName();;

    private int mHour = -1;
    private int mMin = -1;

    private EditText mScheduleName;
    private EditText mScheduleNoti;
    private TextView mScheduleDays;
    private TextView mScheduleTime;
    private CheckBox mCheckBoxReport;

    private static final int SCHEDULE_LOADER = 1;

    private Schedule mSchedule = null;

    private ScheduleManager mScheduleManager;

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
                fragment.setSelectedItem(Utils.getBooleanDaysFromInteger(mSchedule.mWeekdays));
                fragment.setItemSelectedListener(new DaySelectDialogFragment.OnItemSelectedListener() {
                    @Override
                    public void onSelectItems(boolean[] items) {
                        setDays(Utils.getDaysFromBooleanArray(items));
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
                int hour = mSchedule.mHour < 0 ? calendar.get(Calendar.HOUR_OF_DAY) : mSchedule.mHour;
                int min = mSchedule.mMin < 0 ? calendar.get(Calendar.MINUTE) : mSchedule.mMin;
                new TimePickerDialog(ScheduleEditActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int min) {
                        Log.d(TAG, "hour = " + hour + " min = " + min);
                        setTime(hour, min);
                    }
                }, hour, min, false).show();

            }
        });

        mScheduleManager = ScheduleManager.getInstance(this);

        Intent intent = getIntent();
        long id = intent.getLongExtra("schedule_id", Schedule.INVALID_ID);

        if ( id >= 0) {
            Bundle arg = new Bundle();
            arg.putLong("schedule_id", id);
            getSupportLoaderManager().initLoader(SCHEDULE_LOADER, arg, this);
        } else {
            mSchedule = new Schedule();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_schedule_edit, menu);

        MenuItem item = menu.findItem(R.id.action_delete);
        if (mSchedule != null && mSchedule.mId < 0) {
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

    private void initValues(Cursor cursor) {
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                mSchedule = new Schedule(cursor);
                mScheduleName.setText(mSchedule.mName);
                mScheduleNoti.setText(mSchedule.mNotification);
                setDays(mSchedule.mWeekdays);
                setTime(mSchedule.mHour, mSchedule.mMin);
                mCheckBoxReport.setChecked(mSchedule.mReportLocation);
            }

            cursor.close();
        };
    }

    private void setDays(int dayValue) {
        mSchedule.mWeekdays = dayValue;
        String weekDayString = Utils.getDaysStringFromInteger(this, dayValue);
        if (weekDayString != null && weekDayString.length() > 0) {
            mScheduleDays.setText(weekDayString);
        } else {
            mScheduleDays.setText(R.string.label_hint_schedule_day);
        }
    }

    private void setTime(int hour, int min) {
        mSchedule.mHour = hour;
        mSchedule.mMin = min;
        boolean pm = hour >= 12;
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
        mSchedule.mName = mScheduleName.getText().toString();
        mSchedule.mNotification = mScheduleNoti.getText().toString();
        mSchedule.mReportLocation = mCheckBoxReport.isChecked();

        if (mSchedule.mId >= 0) {
            Schedule.updateSchedule(getContentResolver(), mSchedule);
        } else {
            Schedule.addSchedule(getContentResolver(), mSchedule);
        }

        if (mSchedule.mIsOn) {
            mScheduleManager.updateSchedule(mSchedule);
        }
        finish();
    }

    void deleteSchedule() {
        Schedule.deleteSchedule(getContentResolver(), mSchedule);

        mScheduleManager.cancelSchedule(mSchedule);
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = ProviderContract.ScheduleColumns._ID + " = " + args.getLong("schedule_id");
        CursorLoader loader = Schedule.createCursorLoader(this);
        loader.setSelection(selection);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        initValues(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
