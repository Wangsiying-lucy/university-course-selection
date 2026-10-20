package com.course.bootstrap;

import com.course.model.Course;
import com.course.model.Student;
import com.course.model.TimeSlot;
import com.course.repository.InMemoryCourseRepository;
import com.course.repository.InMemoryStudentRepository;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collections;

/** Builds deterministic demo data so the project can run immediately after compilation. */
public final class SampleData {
    private SampleData() { }

    public static void load(InMemoryCourseRepository courses, InMemoryStudentRepository students) {
        courses.save(new Course("CS101", "Java程序设计", "张老师", 3, 2,
                "Java基础语法、面向对象编程与异常处理。",
                Arrays.asList(new TimeSlot(DayOfWeek.MONDAY, 8 * 60, 10 * 60)),
                Collections.<String>emptySet()));
        courses.save(new Course("CS201", "数据结构", "王老师", 3, 2,
                "线性表、树、图与常用算法。",
                Arrays.asList(new TimeSlot(DayOfWeek.TUESDAY, 10 * 60, 12 * 60)),
                Collections.singleton("CS101")));
        courses.save(new Course("CS202", "面向对象分析与设计", "刘老师", 3, 3,
                "面向对象分析、设计原则与设计模式。",
                Arrays.asList(new TimeSlot(DayOfWeek.TUESDAY, 10 * 60, 12 * 60)),
                Collections.<String>emptySet()));
        courses.save(new Course("CS301", "数据库系统", "陈老师", 3, 2,
                "关系模型、SQL、事务与数据库设计。",
                Arrays.asList(new TimeSlot(DayOfWeek.WEDNESDAY, 14 * 60, 16 * 60)),
                Collections.singleton("CS201")));
        courses.save(new Course("MA101", "高等数学", "赵老师", 4, 2,
                "微积分与线性代数基础。",
                Arrays.asList(new TimeSlot(DayOfWeek.MONDAY, 10 * 60, 12 * 60)),
                Collections.<String>emptySet()));
        courses.save(new Course("PE101", "大学体育", "周老师", 1, 5,
                "体能训练与专项运动。",
                Arrays.asList(new TimeSlot(DayOfWeek.THURSDAY, 16 * 60, 18 * 60)),
                Collections.<String>emptySet()));

        Student first = new Student("2023001", "汪思颖", "人工智能", 15);
        first.addCompletedCourse("CS101");
        students.save(first);
        students.save(new Student("2023002", "陈悦", "计算机科学与技术", 12));
    }
}
