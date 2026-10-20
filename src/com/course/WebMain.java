package com.course;

import com.course.bootstrap.SampleData;
import com.course.model.Student;
import com.course.repository.InMemoryCourseRepository;
import com.course.repository.InMemoryStudentRepository;
import com.course.rule.CapacityRule;
import com.course.rule.CreditLimitRule;
import com.course.rule.PrerequisiteRule;
import com.course.rule.ScheduleConflictRule;
import com.course.service.CourseQueryService;
import com.course.service.EnrollmentService;
import com.course.web.WebApplication;
import java.awt.Desktop;
import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class WebMain {
    private static final int DEFAULT_PORT = 8080;

    private WebMain() { }

    public static void main(String[] args) throws Exception {
        InMemoryCourseRepository courseRepository = new InMemoryCourseRepository();
        InMemoryStudentRepository studentRepository = new InMemoryStudentRepository();
        SampleData.load(courseRepository, studentRepository);

        Scanner scanner = new Scanner(System.in, "UTF-8");
        Student student = resolveStudent(studentRepository, scanner, optionValue(args, "--student="));

        EnrollmentService enrollmentService = new EnrollmentService(courseRepository,
                new CapacityRule(), new PrerequisiteRule(),
                new ScheduleConflictRule(), new CreditLimitRule());
        CourseQueryService courseQueryService = new CourseQueryService(courseRepository);
        WebApplication webApplication = createApplication(
                optionPort(args), courseRepository, courseQueryService, enrollmentService, student);

        webApplication.start();
        String url = webApplication.getHomeUrl();
        System.out.println("网页版已启动：" + url);
        System.out.println("当前学生：" + student.getId() + " " + student.getName() + "（" + student.getMajor() + "）");

        if (!hasOption(args, "--no-browser") && openBrowser(url)) {
            System.out.println("浏览器已自动打开。");
        } else {
            System.out.println("如果浏览器没有自动打开，请复制上面的地址到浏览器。");
        }

        System.out.println("保持此终端打开即可使用网页；按 Enter 停止 Web 服务。");
        try {
            scanner.nextLine();
            webApplication.stop();
            System.out.println("Web 服务已停止。");
        } catch (NoSuchElementException ex) {
            keepAliveUntilInterrupted(webApplication);
        }
    }

    private static Student resolveStudent(InMemoryStudentRepository studentRepository,
                                          Scanner scanner,
                                          String initialStudentId) {
        Student student = findStudent(studentRepository, initialStudentId);
        while (student == null) {
            if (initialStudentId != null && !initialStudentId.trim().isEmpty()) {
                System.out.println("未找到学号 " + initialStudentId + "，请重新输入。");
            }
            System.out.print("请输入学号，直接回车默认 2023001：");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) input = "2023001";
            student = findStudent(studentRepository, input);
            initialStudentId = input;
            if (student == null) System.out.println("未找到该学生，请重新输入。\n");
        }
        return student;
    }

    private static Student findStudent(InMemoryStudentRepository studentRepository, String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) return null;
        return studentRepository.findById(studentId.trim()).orElse(null);
    }

    private static WebApplication createApplication(int preferredPort,
                                                    InMemoryCourseRepository courseRepository,
                                                    CourseQueryService courseQueryService,
                                                    EnrollmentService enrollmentService,
                                                    Student student) throws IOException {
        IOException lastFailure = null;
        for (int port = preferredPort; port < preferredPort + 20; port++) {
            try {
                return new WebApplication(port, courseRepository, courseQueryService, enrollmentService, student);
            } catch (BindException ex) {
                lastFailure = ex;
            }
        }
        throw lastFailure == null ? new IOException("无法启动 Web 服务") : lastFailure;
    }

    private static int optionPort(String[] args) {
        String value = optionValue(args, "--port=");
        if (value == null || value.trim().isEmpty()) return DEFAULT_PORT;
        try {
            int port = Integer.parseInt(value.trim());
            if (port < 1 || port > 65535) throw new NumberFormatException("port out of range");
            return port;
        } catch (NumberFormatException ex) {
            System.out.println("端口参数无效，已使用默认端口 " + DEFAULT_PORT + "。");
            return DEFAULT_PORT;
        }
    }

    private static String optionValue(String[] args, String prefix) {
        for (String arg : args) {
            if (arg != null && arg.startsWith(prefix)) return arg.substring(prefix.length());
        }
        return null;
    }

    private static boolean hasOption(String[] args, String option) {
        for (String arg : args) {
            if (option.equals(arg)) return true;
        }
        return false;
    }

    private static boolean openBrowser(String url) {
        try {
            if (!Desktop.isDesktopSupported()) return false;
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) return false;
            desktop.browse(new URI(url));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static void keepAliveUntilInterrupted(WebApplication webApplication) throws InterruptedException {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                webApplication.stop();
            }
        }));
        Object lock = new Object();
        synchronized (lock) {
            lock.wait();
        }
    }
}
