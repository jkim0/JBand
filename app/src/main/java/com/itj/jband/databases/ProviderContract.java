package com.itj.jband.databases;

import android.net.Uri;
import android.provider.BaseColumns;

import com.itj.jband.ScheduleManagementActivity;

/**
 * Created by Loyid on 2015-11-29.
 */
public final class ProviderContract {
    public static final String AUTHORITY = "com.itj.jband.provider";

    private static final String UNKNOWN = "Unknown";

    private interface JBandBaseColumns extends BaseColumns {
        public static final String SCHEME = "content://";

        public static final String DEFAULT_SORT_ORDER = _ID + " ASC"; // ASC or DESC
    }

    public interface StepColumns extends  JBandBaseColumns {
        public static final String TABLE_NAME = "steps";

        public static final String PATH_STEPS = "/steps";

        public static final String PATH_STEPS_ID = "/steps/";

        public static final int SETPS_ID_PATH_POSITION = 1;

        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_STEPS);

        public static final Uri CONTENT_STEPS_ID_URI_BASE = Uri.parse(SCHEME + AUTHORITY + PATH_STEPS_ID);

        public static final Uri CONTENT_STEPS_ID_URI_PATTERN = Uri.withAppendedPath(CONTENT_URI, "#");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/" + AUTHORITY + "." + TABLE_NAME;

        public static final String CONTENT_ITEM_TYPE = "vmd.android.corsor.item/" + AUTHORITY + "." + TABLE_NAME;

        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_HOUR = "hour";
        public static final String COLUMN_NAME_STEPS = "steps";
    }

    public interface ScheduleColumns extends JBandBaseColumns {
        public static final String TABLE_NAME = "schedules";

        public static final String PATH_SCHEDULE = "/schedules";

        public static final String PATH_SCHEDULE_ID = "/schedules/";

        public static final int SCHEDULE_ID_PATH_POSITION = 1;

        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_SCHEDULE);

        public static final Uri CONTENT_SCHEDULE_ID_URI_BASE = Uri.parse(SCHEME + AUTHORITY + PATH_SCHEDULE_ID);

        public static final Uri CONTENT_SCHEDULE_ID_URI_PATTERN = Uri.withAppendedPath(CONTENT_URI, "#");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/" + AUTHORITY + "." + TABLE_NAME;

        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/" + AUTHORITY + "." + TABLE_NAME;

        public static final String COLUMN_SCHEDULE_NAME = "name";
        public static final String COLUMN_SCHEDULE_NOTIFICATION = "notification";
        public static final String COLUMN_SCHEDULE_DAYS = "days";
        public static final String COLUMN_SCHEDULE_HOUR = "hour";
        public static final String COLUMN_SCHEDULE_MIN = "min";
        public static final String COLUMN_SCHEDULE_REPORT_LOCATION = "report_location";
        public static final String COLUMN_SCHEDULE_IS_ON = "is_on";
    }
}
