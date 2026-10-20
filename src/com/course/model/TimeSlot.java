package com.course.model;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

/** Immutable weekly class time. Two slots conflict only on the same day and overlap. */
public final class TimeSlot {
    private final DayOfWeek dayOfWeek;
    private final int startMinute;
    private final int endMinute;

    public TimeSlot(DayOfWeek dayOfWeek, int startMinute, int endMinute) {
        if (dayOfWeek == null || startMinute < 0 || endMinute > 24 * 60 || startMinute >= endMinute) {
            throw new IllegalArgumentException("invalid class time");
        }
        this.dayOfWeek = dayOfWeek;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public boolean overlaps(TimeSlot other) {
        return dayOfWeek == other.dayOfWeek
                && startMinute < other.endMinute
                && other.startMinute < endMinute;
    }

    private String formatMinute(int minute) {
        return String.format("%02d:%02d", minute / 60, minute % 60);
    }

    @Override
    public String toString() {
        String day = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA);
        return day + " " + formatMinute(startMinute) + "-" + formatMinute(endMinute);
    }
}
