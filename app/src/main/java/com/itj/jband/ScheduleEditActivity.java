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

import java.util.Calendar;
import java.util.GregorianCalendar;

public class ScheduleEditActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ScheduleEditActivity.class.getSimpleName();;

    private long mScheduleId = -1;

    private int mHour = -1;
    private int mMin = -1;

    private EditText mScheduleName;
    private EditText mScheduleNoti;
    private TextView mScheduleDays;
    private TextView mScheduleTime;
    private CheckBox mCheckBoxReport;

    private static final int SCHEDULE_LOADER = 1;

    private int mSelectedDays = 0;

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
                fragment.setSelectedItem(Utils.getBooleanDaysFromInteger(mSelectedDays));
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
                int hour = mHour < 0 ? calendar.get(Calendar.HOUR_OF_DAY) : mHour;
                int min = mMin < 0 ? calendar.get(Calendar.MINUTE) : mMin;
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
            mScheduleId = intent.getLongExtra("schedule_id", -1);
            if (mScheduleId >= 0) {
                getSupportLoaderManager().initLoader(SCHEDULE_LOADER, null, this);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_schedule_edit, menu);

        MenuItem item = menu.findItem(R.id.action_delete);
        if (mScheduleId < 0) {
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
        String selection = ProviderContract.Schedule._ID + " = ?";
        String[] selectionArgs = { String.valueOf(mScheduleId) };

        String[] columns = {
                ProviderContract.Schedule._ID,
                ProviderContract.Schedule.COLUMN_SCHEDULE_NAME,
                ProviderContract.Schedule.COLUMN_SCHEDULE_DAYS,
                ProviderContract.Schedule.COLUMN_SCHEDULE_HOUR,
                ProviderContract.Schedule.COLUMN_SCHEDULE_MIN,
                ProviderContract.Schedule.COLUMN_SCHEDULE_REPORT_LOCATION,
        };

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int nameColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule.COLUMN_SCHEDULE_NAME);
                int notiColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule.COLUMN_SCHEDULE_NOTIFICATION);
                int daysColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule.COLUMN_SCHEDULE_DAYS);
                int hourColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule.COLUMN_SCHEDULE_HOUR);
                int minColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule.COLUMN_SCHEDULE_MIN);
                int reportLocationColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule.COLUMN_SCHEDULE_REPORT_LOCATION);

                mScheduleName.setText(cursor.getString(nameColumnIndex));
                mScheduleNoti.setText(cursor.getString(notiColumnIndex));

                setDays(cursor.getInt(daysColumnIndex));
                setTime(cursor.getInt(hourColumnIndex), cursor.getInt(minColumnIndex));

                boolean report = cursor.getInt(reportLocationColumnIndex) == 0 ? false : true;
                mCheckBoxReport.setChecked(report);
            }

            cursor.close();
        };
    }

    private void setDays(int dayValue) {
        mSelectedDays = dayValue;
        mScheduleDays.setText(Utils.getDaysStringFromInteger(this, dayValue));
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

        ContentValues values = new ContentValues();
        values.put(ProviderContract.Schedule.COLUMN_SCHEDULE_NAME, name);
        values.put(ProviderContract.Schedule.COLUMN_SCHEDULE_NOTIFICATION, noti);
        values.put(ProviderContract.Schedule.COLUMN_SCHEDULE_DAYS, mSelectedDays);
        values.put(ProviderContract.Schedule.COLUMN_SCHEDULE_HOUR, mHour);
        values.put(ProviderContract.Schedule.COLUMN_SCHEDULE_MIN, mMin);
        values.put(ProviderContract.Schedule.COLUMN_SCHEDULE_REPORT_LOCATION, report);

        if (mScheduleId >= 0) {
            getContentResolver().update(
                    Uri.withAppendedPath(ProviderContract.Schedule.CONTENT_SCHEDULE_ID_URI_BASE, "" + mScheduleId),
                    values, null, null);
        } else {
            getContentResolver().insert(ProviderContract.Schedule.CONTENT_URI, values);
        }

        finish();
    }

    void deleteSchedule() {
        getContentResolver().delete(ProviderContract.Schedule.CONTENT_URI, ProviderContract.Schedule._ID + " = " + mScheduleId, null);
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
        String[] columns = {
                ProviderContract.Schedule._ID,
                ProviderContract.Schedule.COLUMN_SCHEDULE_NAME,
                ProviderContract.Schedule.COLUMN_SCHEDULE_NOTIFICATION,
                ProviderContract.Schedule.COLUMN_SCHEDULE_DAYS,
                ProviderContract.Schedule.COLUMN_SCHEDULE_HOUR,
                ProviderContract.Schedule.COLUMN_SCHEDULE_MIN,
                ProviderContract.Schedule.COLUMN_SCHEDULE_REPORT_LOCATION,
                ProviderContract.Schedule.COLUMN_SCHEDULE_IS_ON
        };

        Uri uri = ProviderContract.Schedule.CONTENT_URI;
        String selection = ProviderContract.Schedule._ID + " = " + mScheduleId;
        return new CursorLoader(this, uri, columns, selection, null, ProviderContract.Schedule.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        initValues(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
