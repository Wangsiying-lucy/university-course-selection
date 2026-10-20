package com.course.rule;

import com.course.model.Course;
import com.course.model.Student;
import com.course.repository.CourseRepository;

public class CapacityRule implements EnrollmentRule {
    @Override
    public RuleResult validate(Student student, Course course, CourseRepository courseRepository) {
        return course.isFull() ? RuleResult.fail("选课失败：该课程已满员。") : RuleResult.pass();
    }
}
