package com.course.rule;

import com.course.model.Course;
import com.course.model.Student;
import com.course.repository.CourseRepository;
import java.util.LinkedHashSet;
import java.util.Set;

public class PrerequisiteRule implements EnrollmentRule {
    @Override
    public RuleResult validate(Student student, Course course, CourseRepository courseRepository) {
        Set<String> missing = new LinkedHashSet<String>(course.getPrerequisiteCodes());
        missing.removeAll(student.getCompletedCourseCodes());
        if (missing.isEmpty()) return RuleResult.pass();
        return RuleResult.fail("选课失败：缺少先修课程 " + missing + "。请先完成相关课程。");
    }
}
