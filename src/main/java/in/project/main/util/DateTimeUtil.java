package in.project.main.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for consistent date/time formatting across the application.
 */
public final class DateTimeUtil
{
    private DateTimeUtil() {} // prevent instantiation

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm:ss a");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Returns the current date and time as "dd/MM/yyyy, hh:mm:ss AM/PM"
     */
    public static String getCurrentDateTimeFormatted()
    {
        String date = LocalDate.now().format(DATE_FORMAT);
        String time = LocalTime.now().format(TIME_FORMAT);
        return date + ", " + time;
    }

    /**
     * Returns the current date in ISO format "yyyy-MM-dd"
     */
    public static String getCurrentDateISO()
    {
        return LocalDate.now().format(ISO_DATE_FORMAT);
    }

    /**
     * Returns the current time in ISO format "HH:mm:ss"
     */
    public static String getCurrentTimeISO()
    {
        return LocalTime.now().format(ISO_TIME_FORMAT);
    }
}
