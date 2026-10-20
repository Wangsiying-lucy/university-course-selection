package com.course.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Student aggregate: completed courses and current selections are private state. */
public class Student extends Person {
    private final String major;
    private final int maxCredits;
    private final Set<String> completedCourseCodes = new LinkedHashSet<String>();
    private final Set<String> selectedCourseCodes = new LinkedHashSet<String>();

    public Student(String id, String name, String major, int maxCredits) {
        super(id, name);
        if (major == null || major.trim().isEmpty() || maxCredits <= 0) {
            throw new IllegalArgumentException("invalid student profile");
        }
        this.major = major.trim();
        this.maxCredits = maxCredits;
    }

    public String getMajor() {
        return major;
    }

    public int getMaxCredits() {
        return maxCredits;
    }

    public Set<String> getCompletedCourseCodes() {
        return Collections.unmodifiableSet(completedCourseCodes);
    }

    public Set<String> getSelectedCourseCodes() {
        return Collections.unmodifiableSet(selectedCourseCodes);
    }

    public void addCompletedCourse(String courseCode) {
        completedCourseCodes.add(courseCode);
    }

    public void addSelectedCourse(String courseCode) {
        selectedCourseCodes.add(courseCode);
    }

    public void removeSelectedCourse(String courseCode) {
        selectedCourseCodes.remove(courseCode);
    }
}
