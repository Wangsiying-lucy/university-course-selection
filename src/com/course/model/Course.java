package com.course.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Course entity with capacity, prerequisite, schedule and enrollment state. */
public class Course {
    private final String code;
    private final String name;
    private final String teacher;
    private final int credits;
    private final int capacity;
    private final String description;
    private final List<TimeSlot> timeSlots;
    private final Set<String> prerequisiteCodes;
    private final Set<String> enrolledStudentIds = new LinkedHashSet<String>();

    public Course(String code, String name, String teacher, int credits, int capacity,
                  String description, List<TimeSlot> timeSlots, Set<String> prerequisiteCodes) {
        if (code == null || code.trim().isEmpty() || name == null || name.trim().isEmpty()
                || teacher == null || teacher.trim().isEmpty() || credits <= 0 || capacity <= 0
                || timeSlots == null || timeSlots.isEmpty()) {
            throw new IllegalArgumentException("invalid course information");
        }
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.teacher = teacher.trim();
        this.credits = credits;
        this.capacity = capacity;
        this.description = description == null ? "" : description.trim();
        this.timeSlots = Collections.unmodifiableList(new ArrayList<TimeSlot>(timeSlots));
        this.prerequisiteCodes = prerequisiteCodes == null
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<String>(prerequisiteCodes));
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getTeacher() { return teacher; }
    public int getCredits() { return credits; }
    public int getCapacity() { return capacity; }
    public String getDescription() { return description; }
    public List<TimeSlot> getTimeSlots() { return timeSlots; }
    public Set<String> getPrerequisiteCodes() { return prerequisiteCodes; }
    public Set<String> getEnrolledStudentIds() { return Collections.unmodifiableSet(enrolledStudentIds); }
    public int getRemainingCapacity() { return capacity - enrolledStudentIds.size(); }
    public boolean isFull() { return enrolledStudentIds.size() >= capacity; }
    public boolean hasStudent(String studentId) { return enrolledStudentIds.contains(studentId); }

    public void enroll(String studentId) {
        if (isFull()) throw new IllegalStateException("course is full");
        enrolledStudentIds.add(studentId);
    }

    public void removeStudent(String studentId) {
        enrolledStudentIds.remove(studentId);
    }

    @Override
    public String toString() {
        return code + " | " + name + " | " + credits + "学分 | " + teacher
                + " | 余量 " + getRemainingCapacity() + "/" + capacity;
    }
}
