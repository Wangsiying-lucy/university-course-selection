package com.course.service;

import com.course.model.Course;
import com.course.model.Student;
import com.course.repository.CourseRepository;
import com.course.rule.EnrollmentRule;
import com.course.rule.RuleResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Coordinates enrollment transactions and delegates policy decisions to rule objects. */
public class EnrollmentService {
    private final CourseRepository courseRepository;
    private final List<EnrollmentRule> rules;

    public EnrollmentService(CourseRepository courseRepository, EnrollmentRule... rules) {
        this.courseRepository = courseRepository;
        this.rules = new ArrayList<EnrollmentRule>(Arrays.asList(rules));
    }

    public EnrollmentResult enroll(Student student, String courseCode) {
        Course course = courseRepository.findByCode(courseCode).orElse(null);
        if (course == null) return EnrollmentResult.failure("选课失败：课程编号不存在。");
        if (student.getSelectedCourseCodes().contains(course.getCode())) {
            return EnrollmentResult.failure("选课失败：你已经选择了该课程。");
        }
        for (EnrollmentRule rule : rules) {
            RuleResult result = rule.validate(student, course, courseRepository);
            if (!result.isPassed()) return EnrollmentResult.failure(result.getMessage());
        }
        course.enroll(student.getId());
        student.addSelectedCourse(course.getCode());
        return EnrollmentResult.success("选课成功：" + course.getCode() + " " + course.getName());
    }

    public EnrollmentResult drop(Student student, String courseCode) {
        Course course = courseRepository.findByCode(courseCode).orElse(null);
        if (course == null) return EnrollmentResult.failure("退课失败：课程编号不存在。");
        if (!student.getSelectedCourseCodes().contains(course.getCode())) {
            return EnrollmentResult.failure("退课失败：你没有选择该课程。");
        }
        student.removeSelectedCourse(course.getCode());
        course.removeStudent(student.getId());
        return EnrollmentResult.success("退课成功：" + course.getCode() + " " + course.getName());
    }
}
