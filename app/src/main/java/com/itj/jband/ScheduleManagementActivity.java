package com.itj.jband;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.itj.jband.databases.ProviderContract;

public class ScheduleManagementActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ScheduleManagementActivity.class.getSimpleName();

    private ListView mListView;

    private static final int SCHEDULE_LIST_LOADER = 1;

    private static final int REQUEST_ADD_SCHEDULE = 1;
    private static final int REQUEST_EDIT_SCHEDULE = 2;
    
    private ScheduleListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_management);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mListView = (ListView)findViewById(R.id.alram_list);
        mAdapter = new ScheduleListAdapter(this, null, 0);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startEditActivity(id);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startEditActivity(-1);
            }
        });

        getSupportLoaderManager().initLoader(SCHEDULE_LIST_LOADER, null, this);
        //addSchedule(new Schedule("병원 가는 시간", "병원 가!!", new boolean[]{false, false, true, true, true, false, false}, 2, 0, true));
    }

    private void startEditActivity(long id) {
        Intent intent = new Intent(ScheduleManagementActivity.this, ScheduleEditActivity.class);
        int requestCode = REQUEST_ADD_SCHEDULE;
        if (id >= 0) {
            intent.putExtra("schedule_id", id);
            requestCode = REQUEST_EDIT_SCHEDULE;
        }
        startActivityForResult(intent, requestCode);
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Schedule schedule = (Schedule)data.getParcelableExtra("schedule");
            if (requestCode == REQUEST_ADD_SCHEDULE) {
                addSchedule(schedule);
            } else if (requestCode == REQUEST_EDIT_SCHEDULE) {
                boolean deleted = data.getBooleanExtra("deleted", false);
                if (deleted) {
                    deleteSchedule(schedule);
                } else {
                    updateSchedule(schedule);
                }
            }
        }
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class ScheduleListAdapter extends CursorAdapter {

        public ScheduleListAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.schedule_list_item, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.mName = (TextView)view.findViewById(R.id.text_view_schedule_name);
            holder.mDays = (TextView)view.findViewById(R.id.text_view_schedule_days);
            holder.mTime = (TextView)view.findViewById(R.id.text_view_schedule_time);
            holder.mAmPm = (TextView)view.findViewById(R.id.text_view_schedule_am_pm);
            holder.mReportLocation = (TextView)view.findViewById(R.id.text_view_report_location);
            holder.mSwitch = (Switch)view.findViewById(R.id.schedule_switch);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder)view.getTag();
            int idColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule._ID);
            int nameColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule.COLUMN_SCHEDULE_NAME);
            int daysColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule.COLUMN_SCHEDULE_DAYS);
            int hourColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule.COLUMN_SCHEDULE_HOUR);
            int minColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule.COLUMN_SCHEDULE_MIN);
            int reportLocationColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule.COLUMN_SCHEDULE_REPORT_LOCATION);
            int isOnColumnIndex = cursor.getColumnIndex(ProviderContract.Schedule.COLUMN_SCHEDULE_IS_ON);

            String name = cursor.getString(nameColumnIndex);
            int days = cursor.getInt(daysColumnIndex);
            int hour = cursor.getInt(hourColumnIndex);
            int min = cursor.getInt(minColumnIndex);
            boolean reportLocation = cursor.getInt(reportLocationColumnIndex) == 0 ? false : true;
            boolean isOn = cursor.getInt(isOnColumnIndex) == 0 ? false : true;
            final long id = cursor.getLong(idColumnIndex);

            holder.mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ContentValues values = new ContentValues();
                    values.put(ProviderContract.Schedule.COLUMN_SCHEDULE_IS_ON, isChecked);
                    getContentResolver().update(
                            Uri.withAppendedPath(ProviderContract.Schedule.CONTENT_SCHEDULE_ID_URI_BASE, "" + id),values, null, null);
                }
            });

            holder.mName.setText(name);

            holder.mDays.setText(Utils.getDaysStringFromInteger(ScheduleManagementActivity.this, days));

            boolean pm = hour > 12;
            if (pm) {
                holder.mAmPm.setText(getString(R.string.pm));
            } else {
                holder.mAmPm.setText(getString(R.string.am));
            }
            if (hour > 12) {
                hour -= 12;
            }

            holder.mTime.setText(String.format(getString(R.string.alarm_time_format), hour, min));

            holder.mSwitch.setChecked(isOn);
        }
    }

    private class ViewHolder {
        public TextView mName;
        public TextView mDays;
        public TextView mAmPm;
        public TextView mTime;
        public TextView mReportLocation;
        public Switch mSwitch;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] columns = {
                ProviderContract.Schedule._ID,
                ProviderContract.Schedule.COLUMN_SCHEDULE_NAME,
                ProviderContract.Schedule.COLUMN_SCHEDULE_DAYS,
                ProviderContract.Schedule.COLUMN_SCHEDULE_HOUR,
                ProviderContract.Schedule.COLUMN_SCHEDULE_MIN,
                ProviderContract.Schedule.COLUMN_SCHEDULE_REPORT_LOCATION,
                ProviderContract.Schedule.COLUMN_SCHEDULE_IS_ON
        };

        Uri uri = ProviderContract.Schedule.CONTENT_URI;

        return new CursorLoader(this, uri, columns, null, null, ProviderContract.Schedule.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
