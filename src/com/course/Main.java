package com.course;

import com.course.bootstrap.SampleData;
import com.course.model.Student;
import com.course.repository.InMemoryCourseRepository;
import com.course.repository.InMemoryStudentRepository;
import com.course.service.CourseQueryService;
import com.course.service.EnrollmentService;
import com.course.rule.CapacityRule;
import com.course.rule.CreditLimitRule;
import com.course.rule.PrerequisiteRule;
import com.course.rule.ScheduleConflictRule;
import com.course.ui.ConsoleApplication;
import java.util.Scanner;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        InMemoryCourseRepository courseRepository = new InMemoryCourseRepository();
        InMemoryStudentRepository studentRepository = new InMemoryStudentRepository();
        SampleData.load(courseRepository, studentRepository);

        Scanner scanner = new Scanner(System.in, "UTF-8");
        System.out.println("欢迎使用大学选课系统");
        System.out.println("演示账号：2023001（汪思颖，人工智能，已修 CS101），2023002（未修先修课）");
        Student student = null;
        while (student == null) {
            System.out.print("请输入学号：");
            String id = scanner.nextLine().trim();
            student = studentRepository.findById(id).orElse(null);
            if (student == null) System.out.println("未找到该学生，请重新输入。\n");
        }

        EnrollmentService enrollmentService = new EnrollmentService(courseRepository,
                new CapacityRule(), new PrerequisiteRule(),
                new ScheduleConflictRule(), new CreditLimitRule());
        new ConsoleApplication(scanner, courseRepository, new CourseQueryService(courseRepository),
                enrollmentService, student).run();
        scanner.close();
    }
}
