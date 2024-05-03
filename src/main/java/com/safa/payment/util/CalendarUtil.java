package com.safa.payment.util;

import java.util.Calendar;
import java.util.Date;

public class CalendarUtil {

    public static Date addDaysToDate(Date date, int daysToAdd) {
        // Create a Calendar instance and set it to the specified date using the system's default timezone
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        // Add the specified number of days
        calendar.add(Calendar.DAY_OF_MONTH, daysToAdd);

        // Get the updated date from the Calendar
        Date newDate = calendar.getTime();

        return newDate;
    }

}
