package com.itj.jband.databases;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ITJBandProvider extends ContentProvider {
    private static final String TAG = ITJBandProvider.class.getSimpleName();

    private DatabaseHelper mDatabaseHelper;

    private static final int SCHEDULE = 0;
    private static final int SCHEDULE_ID = 1;

    private static final UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(ProviderContract.AUTHORITY, ProviderContract.Schedule.TABLE_NAME, SCHEDULE);
        sUriMatcher.addURI(ProviderContract.AUTHORITY, ProviderContract.Schedule.TABLE_NAME + "/#", SCHEDULE_ID);
    }

    public ITJBandProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "delete uri = " + uri + " selection = " + selection + " selectionArgs = " + getStringFromArray(selectionArgs));
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case SCHEDULE_ID:
                String scheduleId = uri.getPathSegments().get(ProviderContract.Schedule.SCHEDULE_ID_PATH_POSITION);
                if (selection != null) {
                    selection += " AND " + ProviderContract.Schedule._ID + " = " + scheduleId;
                }
            case SCHEDULE:
                count = db.delete(ProviderContract.Schedule.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Invalid URI = " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case SCHEDULE:
                return ProviderContract.Schedule.CONTENT_TYPE;
            case SCHEDULE_ID:
                return ProviderContract.Schedule.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI = " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert uri = " + uri + " values = " + values);
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        long rowId;
        switch (sUriMatcher.match(uri)) {
            case SCHEDULE:
                rowId = db.insert(ProviderContract.Schedule.TABLE_NAME, null, values);
                break;
            default:
                throw new UnsupportedOperationException("Invalid URI = " + uri);
        }

        if (rowId > 0) {
            Uri scheduleUri = ContentUris.withAppendedId(ProviderContract.Schedule.CONTENT_SCHEDULE_ID_URI_BASE, rowId);
            if (scheduleUri != null) {
                getContext().getContentResolver().notifyChange(scheduleUri, null);
                return scheduleUri;
            }
        }

        return null;
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query uri = " + uri + " projection = " + getStringFromArray(projection)
        + " selection = " + selection + " selectionArgs = " + getStringFromArray(selectionArgs)
        + " sortOrder = " + sortOrder);

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        List<String> prependArgs = new ArrayList<String>();
        String limit = uri.getQueryParameter("limit");

        if (uri.getQueryParameter("distinct") != null) {
            qb.setDistinct(true);
        }

        switch (sUriMatcher.match(uri)) {
            case SCHEDULE_ID:
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getPathSegments().get(ProviderContract.Schedule.SCHEDULE_ID_PATH_POSITION));
            case SCHEDULE:
                qb.setTables(ProviderContract.Schedule.TABLE_NAME);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI = " + uri);
        }

        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();

        Cursor cursor = qb.query(db, projection, selection, combine(prependArgs, selectionArgs), null, null, sortOrder, limit);
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        Log.d(TAG, "update uri = " + uri + " values = " + values + " selection = " + selection
        + " selelectionArgs = " + getStringFromArray(selectionArgs));
        int count;

        switch (sUriMatcher.match(uri)) {
            case SCHEDULE_ID:
                String scheduleId = uri.getPathSegments().get(ProviderContract.Schedule.SCHEDULE_ID_PATH_POSITION);
                String prependSelection = ProviderContract.Schedule._ID + " = " + scheduleId;
                if (selection != null) {
                    selection += " AND " + prependSelection;
                } else {
                    selection = prependSelection;
                }
            case SCHEDULE:
                break;
            default:
                throw new IllegalArgumentException("Unknown URI = " + uri);
        }

        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        count = db.update(ProviderContract.Schedule.TABLE_NAME, values, selection, selectionArgs);

        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    private String getStringFromArray(String[] array) {
        String dest = "[";
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                dest += array[i];
                if (i < array.length - 1) {
                    dest += ", ";
                }
            }
        }
        dest += "]";
        return dest;
    }

    private String[] combine(List<String> prepend, String[] userArgs) {
        int presize = prepend.size();
        if (presize == 0) {
            return userArgs;
        }

        int usersize = (userArgs != null) ? userArgs.length : 0;
        String [] combined = new String[presize + usersize];

        for (int i = 0; i < presize; i++) {
            combined[i] = prepend.get(i);
        }

        for (int i = 0; i < usersize; i++) {
            combined[presize + i] = userArgs[i];
        }

        return combined;
    }
}
