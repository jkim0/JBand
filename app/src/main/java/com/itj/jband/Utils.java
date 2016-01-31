package com.itj.jband;

import android.content.Context;
import android.support.v4.view.PagerAdapter;

/**
 * Created by Loyid on 2016-01-31.
 */
public class Utils {
    public static final int DAY_SUNDAY = 0x0001;
    public static final int DAY_MONDAY = 0x0002;
    public static final int DAY_TUESDAY = 0x0004;
    public static final int DAY_WEDNESDAY = 0x0008;
    public static final int DAY_THURSDAY = 0x0010;
    public static final int DAY_FIRDAY = 0x0020;
    public static final int DAY_SATURDAY = 0x0040;
    public static final int DAY_ALL = DAY_SUNDAY | DAY_MONDAY | DAY_TUESDAY | DAY_WEDNESDAY | DAY_THURSDAY | DAY_FIRDAY | DAY_SATURDAY;
    public static final int[] WEEKDAYS = new int[] {DAY_SUNDAY, DAY_MONDAY, DAY_TUESDAY, DAY_WEDNESDAY, DAY_THURSDAY, DAY_FIRDAY, DAY_SATURDAY};

    private static void addStringToStringBuilder(StringBuilder sb, String str) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(str);
    }

    public static String getDaysStringFromInteger(Context context, int dayValue) {
        if ((dayValue & DAY_ALL) == DAY_ALL) {
            return context.getString(R.string.label_every_day);
        }

        StringBuilder sb = new StringBuilder();
        String[] days = context.getResources().getStringArray(R.array.days_of_week);

        for (int i = 0; i < WEEKDAYS.length; i++) {
            if ((dayValue & WEEKDAYS[i]) == WEEKDAYS[i]) {
                addStringToStringBuilder(sb, days[i]);
            }
        }

        return sb.toString();
    }

    public static boolean[] getBooleanDaysFromInteger(int dayValue) {
        boolean[] result = new boolean[] {false, false, false, false, false, false, false};
        for (int i = 0; i < WEEKDAYS.length; i++) {
            if ((dayValue & WEEKDAYS[i]) == WEEKDAYS[i]) {
                result[i] = true;
            }
        }

        return result;
    }

    public static int getDaysFromBooleanArray(boolean[] array) {
        int days = 0;
        for (int i = 0; i < WEEKDAYS.length; i++) {
            if (array[i]) {
                days |= WEEKDAYS[i];
            }
        }
        return days;
    }
}
