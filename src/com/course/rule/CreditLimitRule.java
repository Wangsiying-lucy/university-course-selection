package com.course.rule;

import com.course.model.Course;
import com.course.model.Student;
import com.course.repository.CourseRepository;

public class CreditLimitRule implements EnrollmentRule {
    @Override
    public RuleResult validate(Student student, Course course, CourseRepository courseRepository) {
        int currentCredits = 0;
        for (String selectedCode : student.getSelectedCourseCodes()) {
            Course selected = courseRepository.findByCode(selectedCode).orElse(null);
            if (selected != null) currentCredits += selected.getCredits();
        }
        if (currentCredits + course.getCredits() > student.getMaxCredits()) {
            return RuleResult.fail("选课失败：超过本学期 " + student.getMaxCredits() + " 学分上限。");
        }
        return RuleResult.pass();
    }
}
