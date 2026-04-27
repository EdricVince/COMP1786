package com.pextrack.app.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for date formatting and comparison.
 */
public final class DateUtils {

    public static final String DATE_FORMAT         = "dd/MM/yyyy";
    public static final String DATETIME_FORMAT     = "dd/MM/yyyy HH:mm";

    private DateUtils() {}

    /** Returns the current date as a formatted string. */
    public static String today() {
        return new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(new Date());
    }

    /** Returns the current date-time as a formatted string. */
    public static String now() {
        return new SimpleDateFormat(DATETIME_FORMAT, Locale.getDefault()).format(new Date());
    }

    /**
     * Returns true if end date is strictly after start date.
     * Both must be in dd/MM/yyyy format.
     */
    public static boolean isEndAfterStart(String start, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        try {
            Date s = sdf.parse(start);
            Date e = sdf.parse(end);
            if (s == null || e == null) return false;
            return e.after(s);
        } catch (ParseException ex) {
            return false;
        }
    }

    /** Formats a Date object to the standard display format. */
    public static String format(Date date) {
        return new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(date);
    }
}
