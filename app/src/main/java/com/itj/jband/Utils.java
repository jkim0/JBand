package com.itj.jband;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.util.Log;

import java.util.Calendar;

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

    public static final boolean isKitkatOrLater() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    private static void addStringToStringBuilder(StringBuilder sb, String str) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(str);
    }

    public static String getDaysStringFromInteger(Context context, int dayValue) {
        if (dayValue == 0) {
            return null;
        } else if ((dayValue & DAY_ALL) == DAY_ALL) {
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

    public static int calculateDaysToNextSchedule(int weekDays, Calendar current) {
        int dayCount = 0;
        int dayOfWeek = current.get(Calendar.DAY_OF_WEEK) - 1;
        for (; dayCount < WEEKDAYS.length; dayCount ++) {
            if ((weekDays & WEEKDAYS[(dayOfWeek + dayCount) % WEEKDAYS.length]) > 0) {
                break;
            }
        }

        return dayCount;
    }

    public static boolean getSavedSleepMode(Context context) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), context.MODE_PRIVATE);
        return sp.getBoolean("sleep_mode", false);
    }

    public static void saveSleepMode(Context context, boolean mode) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("sleep_mode", mode);
        editor.commit();
    }

    /**
     * the functions of below is from GaiaControl packages.
     */

    public static final int BYTES_IN_INT = 4;
    private static final int BITS_IN_BYTE = 8;

    /**
     * Extract an <code>int</code> field from an array.
     * @param source The array to extract from.
     * @param offset Offset within source array.
     * @param length Number of bytes to use (maximum 4).
     * @param reverse True if bytes should be interpreted in reverse (little endian) order.
     * @return The extracted integer.
     */
    public static int extractIntField(byte [] source, int offset, int length, boolean reverse) {
        if (length < 0 | length > BYTES_IN_INT) throw new IndexOutOfBoundsException("Length must be between 0 and " + BYTES_IN_INT);
        int result = 0;
        int shift = (length-1) * BITS_IN_BYTE;

        if (reverse) {
            for (int i = offset+length-1; i >= offset; i--) {
                result |= ((source[i] & 0xFF) << shift);
                shift -= BITS_IN_BYTE;
            }
        }
        else {
            for (int i = offset; i < offset+length; i++) {
                result |= ((source[i] & 0xFF) << shift);
                shift -= BITS_IN_BYTE;
            }
        }
        return result;
    }

    /**
     * Get 16-bit hexadecimal string representation of byte.
     *
     * @param i
     *            The value.
     *
     * @return Hex value as a string.
     */
    public static String getIntToHexadecimal(int i) {
        return String.format("%04X", i & 0xFFFF);
    }

    /**
     * Convert a byte array to a human readable String.
     *
     * @param value
     *            The byte array.
     * @return String object containing values in byte array formatted as hex.
     */
    public static String getStringFromBytes(byte[] value) {
        if (value == null)
            return "null";
        String out = "";
        for (byte b : value) {
            out += String.format("0x%02x ", b);
        }
        return out;
    }
}
