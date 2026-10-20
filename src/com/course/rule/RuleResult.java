package com.course.rule;

public final class RuleResult {
    private final boolean passed;
    private final String message;

    private RuleResult(boolean passed, String message) {
        this.passed = passed;
        this.message = message;
    }

    public static RuleResult pass() { return new RuleResult(true, ""); }
    public static RuleResult fail(String message) { return new RuleResult(false, message); }
    public boolean isPassed() { return passed; }
    public String getMessage() { return message; }
}
