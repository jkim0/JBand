package com.itj.jband;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

public class ScheduleManagementActivity extends AppCompatActivity {
    private static final String TAG = ScheduleManagementActivity.class.getSimpleName();

    private ListView mListView;

    private ArrayList<Schedule> mData = new ArrayList<Schedule>();

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
        mAdapter = new ScheduleListAdapter(this, R.layout.schedule_list_item, mData);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startEditActivity(mAdapter.getItem(position));
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startEditActivity(null);
            }
        });

        addSchedule(new Schedule("병원 가는 시간", "병원 가!!", new boolean[]{false, false, true, true, true, false, false}, 2, 0, true));
    }

    private void startEditActivity(Schedule schedule) {
        Intent intent = new Intent(ScheduleManagementActivity.this, ScheduleEditActivity.class);
        int requestCode = REQUEST_ADD_SCHEDULE;
        if (schedule != null) {
            intent.putExtra("schedule", schedule);
            requestCode = REQUEST_EDIT_SCHEDULE;
        }
        startActivityForResult(intent, requestCode);
    }

    @Override
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

    private void addSchedule(Schedule schedule) {
        mAdapter.add(schedule);
    }

    private void updateSchedule(Schedule schedule) {
        Schedule src = mAdapter.getItem(schedule.mIndex);
        mAdapter.remove(src);
        mAdapter.insert(schedule, schedule.mIndex);
    }

    private void deleteSchedule(Schedule schedule) {
        Schedule src = mAdapter.getItem(schedule.mIndex);
        mAdapter.remove(src);
    }

    public static class Schedule implements Parcelable {
        public String mName;
        public String mNofi;
        public boolean[] mDays;
        public int mHour;
        public int mMin;
        public boolean mReportLocation;
        public boolean mOn = false;
        public int mIndex = -1;

        public Schedule() {}

        public Schedule(String name, String noti, boolean[] days, int hour, int min, boolean reportLocation) {
            mName = name;
            mNofi = noti;
            mDays = days;
            mHour = hour;
            mMin = min;
            mReportLocation = reportLocation;
            mOn = false;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(mName);
            out.writeString(mNofi);
            out.writeInt(mDays.length);
            out.writeBooleanArray(mDays);
            out.writeInt(mHour);
            out.writeInt(mMin);
            out.writeInt(mReportLocation ? 1 : 0);
            out.writeInt(mOn ? 1 : 0);
            out.writeInt(mIndex);
        }

        public static final Parcelable.Creator<Schedule> CREATOR = new
                Parcelable.Creator<Schedule>() {
                    public Schedule createFromParcel(Parcel in) {
                        return new Schedule(in);
                    }

                    public Schedule[] newArray(int size) {
                        return new Schedule[size];
                    }
                };

        private Schedule(Parcel in) {
            readFromParcel(in);
        }

        public void readFromParcel(Parcel in) {
            mName = in.readString();
            mNofi = in.readString();
            mDays = new boolean[in.readInt()];
            in.readBooleanArray(mDays);
            mHour = in.readInt();
            mMin = in.readInt();
            mReportLocation = in.readInt() == 0 ? false : true;
            mOn = in.readInt() == 0 ? false : true;
            mIndex = in.readInt();
        }
    }

    private class ScheduleListAdapter extends ArrayAdapter<Schedule> {
        public ScheduleListAdapter(Context context, int reourceId, ArrayList<Schedule> items) {
            super(context, reourceId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.schedule_list_item, null);
            }

            bindView(position, convertView);

            return convertView;
        }

        private void bindView(int position, View view) {
            ViewHolder holder = (ViewHolder)view.getTag();
            Log.d(TAG, "holder = " + holder);
            if (holder == null) {
                holder = new ViewHolder();
                holder.mName = (TextView)view.findViewById(R.id.text_view_schedule_name);
                holder.mDays = (TextView)view.findViewById(R.id.text_view_schedule_days);
                holder.mTime = (TextView)view.findViewById(R.id.text_view_schedule_time);
                holder.mAmPm = (TextView)view.findViewById(R.id.text_view_schedule_am_pm);
                holder.mReportLocation = (TextView)view.findViewById(R.id.text_view_report_location);
                holder.mSwitch = (Switch)view.findViewById(R.id.schedule_switch);
                view.setTag(holder);
            }

            Schedule schedule = getItem(position);
            schedule.mIndex = position;

            holder.mReportLocation.setVisibility(schedule.mOn ? View.VISIBLE : View.INVISIBLE);
            holder.mName.setText(schedule.mName);

            StringBuilder sb = new StringBuilder();
            String[] days = getResources().getStringArray(R.array.days_of_week);
            int length = schedule.mDays.length;
            for (int i = 0; i < length; i++) {
                if (schedule.mDays[i]) {
                    sb.append(days[i] + " ");
                }
            }

            holder.mDays.setText(sb.toString());

            boolean pm = schedule.mHour > 12;
            if (pm) {
                holder.mAmPm.setText(getString(R.string.pm));
            } else {
                holder.mAmPm.setText(getString(R.string.am));
            }

            int hour = schedule.mHour;
            if (hour > 12) {
                hour -= 12;
            }
            holder.mTime.setText(String.format(getString(R.string.alarm_time_format), hour, schedule.mMin));
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

}
