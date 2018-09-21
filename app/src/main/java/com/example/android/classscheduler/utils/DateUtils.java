package com.example.android.classscheduler.utils;

import com.example.android.classscheduler.CreateClassesActivity;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by jonathanbarrera on 6/10/18.
 * Util functions for retrieving and formatting dates
 */

public class DateUtils {

    // Helper method for converting from milliseconds to date string
    // ex. "January 01, 2018"
    public static String convertDateLongToString(long birthdate) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String dateString = formatter.format(birthdate);
        String[] dateIntegerStrings = dateString.split("/");

        int day = Integer.parseInt(dateIntegerStrings[0]);
        int month = Integer.parseInt(dateIntegerStrings[1]) - 1;
        int year = Integer.parseInt(dateIntegerStrings[2]);

        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
        return dateFormatSymbols.getMonths()[month] + " " + day + ", " + year;
    }

    // Helper method for getting the age of a student
    public static String getAge(long birthdate) {
        // Birthdate
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String dateString = formatter.format(birthdate);
        String[] dateIntegerStrings = dateString.split("/");

        int birthDay = Integer.parseInt(dateIntegerStrings[0]);
        int birthMonth = Integer.parseInt(dateIntegerStrings[1]);
        int birthYear = Integer.parseInt(dateIntegerStrings[2]);

        // Today's date
        String todaysDateString = formatter.format(Calendar.getInstance().getTime());
        String[] todaysDateIntegerStrings = todaysDateString.split("/");

        int todayDay = Integer.parseInt(todaysDateIntegerStrings[0]);
        int todayMonth = Integer.parseInt(todaysDateIntegerStrings[1]);
        int todayYear = Integer.parseInt(todaysDateIntegerStrings[2]);

        // Algorithm for calculating age
        int age = todayYear - birthYear - 1;
        if (todayMonth > birthMonth || (todayMonth == birthMonth && todayDay >= birthDay)) {
            age = age + 1;
        }

        return String.valueOf(age);
    }

    // Check if the chosen date is later than today's date
    public static boolean isChosenDateAfterToday(long chosenDate) {
        long rightNow = System.currentTimeMillis();
        return chosenDate > rightNow;
    }

    // Helper method to format times
    public static String getFormattedTime(String time) {
        String timeOfDay = "am";
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);

        // Make sure the hours are formatted in 12 hour format, with proper am/pm label
        if (hour > 12) {
            hour = hour - 12;
            timeOfDay = "pm";
        } else if (hour == 12) {
            timeOfDay = "pm";
        } else if (hour == 0) {
            hour = 12;
        }

        // Get the full formatted time string and return
        return hour + ":" + timeParts[1] + timeOfDay;
    }

    // Helper method for getting the day of the week
    public static String getDayOfTheWeek(String scheduleDay) {
        int day = Integer.parseInt(scheduleDay);
        switch (day) {
            case CreateClassesActivity.SUNDAY_INT:
                return "Sunday";
            case CreateClassesActivity.MONDAY_INT:
                return "Monday";
            case CreateClassesActivity.TUESDAY_INT:
                return "Tuesday";
            case CreateClassesActivity.WEDNESDAY_INT:
                return "Wednesday";
            case CreateClassesActivity.THURSDAY_INT:
                return "Thursday";
            case CreateClassesActivity.FRIDAY_INT:
                return "Friday";
            case CreateClassesActivity.SATURDAY_INT:
                return "Saturday";
            default:
                throw new IllegalArgumentException("Invalid day of the week integer: " + day);
        }
    }
}
