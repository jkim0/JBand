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
        db.execSQL("CREATE TABLE " + ProviderContract.Schedule.TABLE_NAME + " ("
                + ProviderContract.Schedule._ID + " INTEGER PRIMARY KEY,"
                + ProviderContract.Schedule.COLUMN_SCHEDULE_NAME + " TEXT,"
                + ProviderContract.Schedule.COLUMN_SCHEDULE_NOTIFICATION + " TEXT,"
                + ProviderContract.Schedule.COLUMN_SCHEDULE_DAYS + " INTEGER,"
                + ProviderContract.Schedule.COLUMN_SCHEDULE_HOUR + " INTEGER,"
                + ProviderContract.Schedule.COLUMN_SCHEDULE_MIN + " INTEGER,"
                + ProviderContract.Schedule.COLUMN_SCHEDULE_REPORT_LOCATION + " INTEGER DEFAULT 0,"
                + ProviderContract.Schedule.COLUMN_SCHEDULE_IS_ON + " INTEGER DEFAULT 0"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ProviderContract.Steps.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ProviderContract.Schedule.TABLE_NAME);
        onCreate(db);
    }
}
