package com.course.ui;

import com.course.model.Course;
import com.course.model.Student;
import com.course.repository.CourseRepository;
import com.course.service.CourseQueryService;
import com.course.service.EnrollmentResult;
import com.course.service.EnrollmentService;
import java.util.List;
import java.util.Scanner;

/** Text UI. It validates input and keeps the application usable after every failed operation. */
public class ConsoleApplication {
    private final Scanner scanner;
    private final CourseRepository courseRepository;
    private final CourseQueryService queryService;
    private final EnrollmentService enrollmentService;
    private final Student student;

    public ConsoleApplication(Scanner scanner, CourseRepository courseRepository,
                              CourseQueryService queryService, EnrollmentService enrollmentService,
                              Student student) {
        this.scanner = scanner;
        this.courseRepository = courseRepository;
        this.queryService = queryService;
        this.enrollmentService = enrollmentService;
        this.student = student;
    }

    public void run() {
        System.out.println("\n========================================");
        System.out.println("       大学选课系统 - 当前学生：" + student.getName());
        System.out.println("========================================");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": showCourses(queryService.search("")); break;
                case "2": searchCourses(); break;
                case "3": showCourseDetail(); break;
                case "4": enroll(); break;
                case "5": drop(); break;
                case "6": showSchedule(); break;
                case "7": showStudentProfile(); break;
                case "0": running = false; System.out.println("感谢使用，再见！"); break;
                default: System.out.println("无效菜单，请输入 0-7。\n");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--------------- 主菜单 ---------------");
        System.out.println("1. 查看全部课程");
        System.out.println("2. 搜索课程");
        System.out.println("3. 查看课程详情");
        System.out.println("4. 选课");
        System.out.println("5. 退课");
        System.out.println("6. 查看我的课表");
        System.out.println("7. 查看学生信息");
        System.out.println("0. 退出系统");
        System.out.print("请选择：");
    }

    private void searchCourses() {
        System.out.print("请输入课程编号、名称或教师关键词（直接回车查看全部）：");
        showCourses(queryService.search(scanner.nextLine()));
    }

    private void showCourses(List<Course> courses) {
        if (courses.isEmpty()) { System.out.println("没有找到匹配课程。\n"); return; }
        System.out.println("\n课程列表：");
        for (Course course : courses) {
            System.out.println("- " + course);
        }
    }

    private void showCourseDetail() {
        Course course = findCourseByPrompt("请输入课程编号：");
        if (course == null) return;
        System.out.println("\n课程详情");
        System.out.println("编号：" + course.getCode());
        System.out.println("名称：" + course.getName());
        System.out.println("教师：" + course.getTeacher());
        System.out.println("学分：" + course.getCredits());
        System.out.println("时间：" + course.getTimeSlots());
        System.out.println("先修：" + (course.getPrerequisiteCodes().isEmpty() ? "无" : course.getPrerequisiteCodes()));
        System.out.println("容量：" + course.getRemainingCapacity() + "/" + course.getCapacity());
        System.out.println("简介：" + course.getDescription());
    }

    private void enroll() {
        Course course = findCourseByPrompt("请输入要选择的课程编号：");
        if (course == null) return;
        printResult(enrollmentService.enroll(student, course.getCode()));
    }

    private void drop() {
        Course course = findCourseByPrompt("请输入要退选的课程编号：");
        if (course == null) return;
        printResult(enrollmentService.drop(student, course.getCode()));
    }

    private void showSchedule() {
        System.out.println("\n我的课表（共 " + student.getSelectedCourseCodes().size() + " 门）：");
        if (student.getSelectedCourseCodes().isEmpty()) { System.out.println("暂无已选课程。"); return; }
        int credits = 0;
        for (String code : student.getSelectedCourseCodes()) {
            Course course = courseRepository.findByCode(code).orElse(null);
            if (course != null) {
                credits += course.getCredits();
                System.out.println("- " + course.getCode() + " " + course.getName()
                        + " | " + course.getTimeSlots() + " | " + course.getCredits() + "学分");
            }
        }
        System.out.println("总学分：" + credits + "/" + student.getMaxCredits());
    }

    private void showStudentProfile() {
        System.out.println("\n学生信息");
        System.out.println("学号：" + student.getId());
        System.out.println("姓名：" + student.getName());
        System.out.println("专业：" + student.getMajor());
        System.out.println("已修课程：" + (student.getCompletedCourseCodes().isEmpty() ? "无" : student.getCompletedCourseCodes()));
        System.out.println("本学期已选：" + (student.getSelectedCourseCodes().isEmpty() ? "无" : student.getSelectedCourseCodes()));
    }

    private Course findCourseByPrompt(String prompt) {
        System.out.print(prompt);
        String code = scanner.nextLine().trim();
        Course course = courseRepository.findByCode(code).orElse(null);
        if (course == null) System.out.println("课程不存在，请先通过课程列表确认编号。\n");
        return course;
    }

    private void printResult(EnrollmentResult result) {
        System.out.println(result.getMessage());
    }
}
