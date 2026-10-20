package com.course.rule;

import com.course.model.Course;
import com.course.model.Student;
import com.course.model.TimeSlot;
import com.course.repository.CourseRepository;

public class ScheduleConflictRule implements EnrollmentRule {
    @Override
    public RuleResult validate(Student student, Course course, CourseRepository courseRepository) {
        for (String selectedCode : student.getSelectedCourseCodes()) {
            Course selected = courseRepository.findByCode(selectedCode).orElse(null);
            if (selected == null) continue;
            for (TimeSlot newSlot : course.getTimeSlots()) {
                for (TimeSlot oldSlot : selected.getTimeSlots()) {
                    if (newSlot.overlaps(oldSlot)) {
                        return RuleResult.fail("选课失败：与已选课程 " + selected.getCode()
                                + " " + selected.getName() + " 时间冲突。");
                    }
                }
            }
        }
        return RuleResult.pass();
    }
}
