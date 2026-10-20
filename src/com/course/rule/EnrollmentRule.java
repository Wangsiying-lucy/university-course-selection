package com.course.rule;

import com.course.model.Course;
import com.course.model.Student;
import com.course.repository.CourseRepository;

/** Pluggable rule used by the enrollment service. */
public interface EnrollmentRule {
    RuleResult validate(Student student, Course course, CourseRepository courseRepository);
}
