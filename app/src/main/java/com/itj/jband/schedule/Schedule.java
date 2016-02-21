package com.itj.jband.schedule;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.CursorLoader;
import android.widget.LinearLayout;

import com.itj.jband.Utils;
import com.itj.jband.databases.ProviderContract;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Loyid on 2016-02-21.
 */
public class Schedule implements Parcelable, ProviderContract.ScheduleColumns {
    private static final String TAG = Schedule.class.getSimpleName();

    public static final long INVALID_ID = -1;

    public long mId = INVALID_ID;
    public String mName;
    public String mNotification;
    public int mWeekdays;
    public int mHour = -1;
    public int mMin = -1;
    public boolean mReportLocation = false;
    public boolean mIsOn = false;

    private static final String[] QUERY_COLUMNS = {
            _ID,
            COLUMN_SCHEDULE_NAME,
            COLUMN_SCHEDULE_NOTIFICATION,
            COLUMN_SCHEDULE_DAYS,
            COLUMN_SCHEDULE_HOUR,
            COLUMN_SCHEDULE_MIN,
            COLUMN_SCHEDULE_REPORT_LOCATION,
            COLUMN_SCHEDULE_IS_ON
    };

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int NOTIFICATION_INDEX = 2;
    private static final int WEEK_DAYS_INDEX = 3;
    private static final int HOUR_INDEX = 4;
    private static final int MIN_INDEX = 5;
    private static final int REPORT_LOCATION_INDEX = 6;
    private static final int IS_ON_INDEX = 7;

    public static ContentValues createContentValues(Schedule schedule) {
        ContentValues values = new ContentValues(IS_ON_INDEX + 1);
        if (schedule.mId != INVALID_ID) {
            values.put(_ID, schedule.mId);
        }

        values.put(COLUMN_SCHEDULE_NAME, schedule.mName);
        values.put(COLUMN_SCHEDULE_NOTIFICATION, schedule.mNotification);
        values.put(COLUMN_SCHEDULE_DAYS, schedule.mWeekdays);
        values.put(COLUMN_SCHEDULE_HOUR, schedule.mHour);
        values.put(COLUMN_SCHEDULE_MIN, schedule.mMin);
        values.put(COLUMN_SCHEDULE_REPORT_LOCATION, schedule.mReportLocation ? 1 : 0);
        values.put(COLUMN_SCHEDULE_IS_ON, schedule.mIsOn ? 1 : 0);

        return values;
    }

    public static Uri getUri(long scheduleId) {
        return ContentUris.withAppendedId(CONTENT_URI, scheduleId);
    }

    public static long getId(Uri scheduleUri) {
        return ContentUris.parseId(scheduleUri);
    }

    public static CursorLoader createCursorLoader(Context context) {
        return new CursorLoader(context, CONTENT_URI, QUERY_COLUMNS, null, null, DEFAULT_SORT_ORDER);
    }

    public static Schedule getSchedule(ContentResolver resolver, long scheduleId) {
        Cursor cursor = resolver.query(getUri(scheduleId), QUERY_COLUMNS, null, null, null);
        Schedule result = null;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    result = new Schedule(cursor);
                }
            } finally {
                cursor.close();
            }
        }

        return result;
    }

    public static List<Schedule> getSchedules(ContentResolver resolver, String selection, String[] selectionArgs) {
        Cursor cursor = resolver.query(CONTENT_URI, QUERY_COLUMNS, selection, selectionArgs, DEFAULT_SORT_ORDER);
        List<Schedule> result = new LinkedList<Schedule>();

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        result.add(new Schedule(cursor));
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    public static Schedule addSchedule(ContentResolver resolver, Schedule schedule) {
        ContentValues values = createContentValues(schedule);
        Uri uri = resolver.insert(CONTENT_URI, values);
        schedule.mId = getId(uri);
        return schedule;
    }

    public static boolean updateSchedule(ContentResolver resolver, Schedule schedule) {
        if (schedule.mId == INVALID_ID)
            return false;

        ContentValues values = createContentValues(schedule);
        final int updatedRows = resolver.update(getUri(schedule.mId), values, null, null);
        return updatedRows == 1;
    }

    public static boolean deleteSchedule(ContentResolver resolver, Schedule schedule) {
        if (schedule.mId == INVALID_ID)
            return false;
        final int deleteRows = resolver.delete(getUri(schedule.mId), null, null);
        return deleteRows == 1;
    }

    public Schedule() {
    }

    public Schedule(long id, String name, String notification, int weekdays, int hour, int min, boolean reportLocation, boolean isOn) {
        mId = id;
        mName = name;
        mNotification = notification;
        mWeekdays = weekdays;
        mHour = hour;
        mMin = min;
        mReportLocation = reportLocation;
        mIsOn = isOn;
    }

    public Schedule(Parcel in) {
        mId = in.readLong();
        mName = in.readString();
        mNotification = in.readString();
        mWeekdays = in.readInt();
        mHour = in.readInt();
        mMin = in.readInt();
        mReportLocation = in.readInt() == 0 ? false : true;
        mIsOn = in.readInt() == 0 ? false : true;
    }

    public Schedule(Cursor cursor) {
        mId = cursor.getLong(ID_INDEX);
        mName = cursor.getString(NAME_INDEX);
        mNotification = cursor.getString(NOTIFICATION_INDEX);
        mWeekdays = cursor.getInt(WEEK_DAYS_INDEX);
        mHour = cursor.getInt(HOUR_INDEX);
        mMin = cursor.getInt(MIN_INDEX);
        mReportLocation = cursor.getInt(REPORT_LOCATION_INDEX) == 0 ? false : true;
        mIsOn = cursor.getInt(IS_ON_INDEX) == 0 ? false : true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mName);
        dest.writeString(mNotification);
        dest.writeInt(mWeekdays);
        dest.writeInt(mHour);
        dest.writeInt(mMin);
        dest.writeInt(mReportLocation ? 1 : 0);
        dest.writeInt(mIsOn ? 1 : 0);
    }

    public static final Creator<Schedule> CREATOR =
            new Creator<Schedule>() {
                @Override
                public Schedule createFromParcel(Parcel source) {
                    return new Schedule(source);
                }

                @Override
                public Schedule[] newArray(int size) {
                    return new Schedule[size];
                }
            };

    public Schedule dup() {
        Parcel parcel = Parcel.obtain();
        writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return new Schedule(parcel);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Schedule)) {
            return false;
        }

        final Schedule schedule = (Schedule)object;
        return mId == schedule.mId;
    }

    public Calendar getNextInvokeTime(Calendar currentTime) {
        Calendar nextInvokeTime = Calendar.getInstance();

        nextInvokeTime.set(Calendar.YEAR, currentTime.get(Calendar.YEAR));
        nextInvokeTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH));
        nextInvokeTime.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH));
        nextInvokeTime.set(Calendar.HOUR_OF_DAY, mHour);
        nextInvokeTime.set(Calendar.MINUTE, mMin);
        nextInvokeTime.set(Calendar.SECOND, 0);
        nextInvokeTime.set(Calendar.MILLISECOND, 0);

        if (nextInvokeTime.getTimeInMillis() <= currentTime.getTimeInMillis()) {
            nextInvokeTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        int addDays = Utils.calculateDaysToNextSchedule(mWeekdays, nextInvokeTime);
        if (addDays > 0) {
            nextInvokeTime.add(Calendar.DAY_OF_WEEK, addDays);
        }

        return nextInvokeTime;
    }
    @Override
    public int hashCode() {
        return Long.valueOf(mId).hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Schedule : { ");
        sb.append("id : " + mId + ", ");
        sb.append("name : " + mName + ", ");
        sb.append("noti : " + mNotification + ", ");
        sb.append("days : " + mWeekdays + ", ");
        sb.append("hour : " + mHour + ", ");
        sb.append("min : " + mMin + ", ");
        sb.append("reportLocation : " + mReportLocation + ", ");
        sb.append("is_on : " + mIsOn);
        sb.append(" }");
        return sb.toString();
    }
}
