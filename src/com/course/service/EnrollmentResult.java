package com.course.service;

public final class EnrollmentResult {
    private final boolean success;
    private final String message;

    private EnrollmentResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static EnrollmentResult success(String message) { return new EnrollmentResult(true, message); }
    public static EnrollmentResult failure(String message) { return new EnrollmentResult(false, message); }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
