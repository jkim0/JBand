package com.itj.jband.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Loyid on 2015-11-29.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = DatabaseHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "itj_band.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderContract.ScheduleColumns.TABLE_NAME + " ("
                + ProviderContract.ScheduleColumns._ID + " INTEGER PRIMARY KEY,"
                + ProviderContract.ScheduleColumns.COLUMN_SCHEDULE_NAME + " TEXT,"
                + ProviderContract.ScheduleColumns.COLUMN_SCHEDULE_NOTIFICATION + " TEXT,"
                + ProviderContract.ScheduleColumns.COLUMN_SCHEDULE_DAYS + " INTEGER,"
                + ProviderContract.ScheduleColumns.COLUMN_SCHEDULE_HOUR + " INTEGER,"
                + ProviderContract.ScheduleColumns.COLUMN_SCHEDULE_MIN + " INTEGER,"
                + ProviderContract.ScheduleColumns.COLUMN_SCHEDULE_REPORT_LOCATION + " INTEGER DEFAULT 0,"
                + ProviderContract.ScheduleColumns.COLUMN_SCHEDULE_IS_ON + " INTEGER DEFAULT 0"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ProviderContract.StepColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ProviderContract.ScheduleColumns.TABLE_NAME);
        onCreate(db);
    }
}
