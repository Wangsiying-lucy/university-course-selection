package com.course.web;

import com.course.model.Course;
import com.course.model.Student;
import com.course.repository.CourseRepository;
import com.course.service.CourseQueryService;
import com.course.service.EnrollmentResult;
import com.course.service.EnrollmentService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

public class WebApplication {
    private final HttpServer server;
    private final CourseRepository courseRepository;
    private final CourseQueryService courseQueryService;
    private final EnrollmentService enrollmentService;
    private final Student student;
    private final int port;

    public WebApplication(int port,
                          CourseRepository courseRepository,
                          CourseQueryService courseQueryService,
                          EnrollmentService enrollmentService,
                          Student student) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.courseRepository = courseRepository;
        this.courseQueryService = courseQueryService;
        this.enrollmentService = enrollmentService;
        this.student = student;
        this.port = server.getAddress().getPort();
        configureRoutes();
    }

    public void start() {
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public String getHomeUrl() {
        return "http://localhost:" + port + "/";
    }

    private void configureRoutes() {
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                handleHome(exchange);
            }
        });
        server.createContext("/course", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                handleCourse(exchange);
            }
        });
        server.createContext("/enroll", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                handleEnrollment(exchange, true);
            }
        });
        server.createContext("/drop", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                handleEnrollment(exchange, false);
            }
        });
    }

    private void handleHome(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String keyword = valueOrEmpty(query.get("q"));
        List<Course> courses = courseQueryService.search(keyword);
        sendHtml(exchange, 200, renderHome(keyword, courses, query.get("message"), query.get("status")));
    }

    private void handleCourse(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String code = valueOrEmpty(query.get("code"));
        Optional<Course> course = courseRepository.findByCode(code);
        if (!course.isPresent()) {
            sendHtml(exchange, 404, renderNotFound(code));
            return;
        }
        sendHtml(exchange, 200, renderCourseDetail(course.get(), query.get("message"), query.get("status")));
    }

    private void handleEnrollment(HttpExchange exchange, boolean enroll) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        Map<String, String> form = parseForm(exchange);
        String courseCode = valueOrEmpty(form.get("courseCode"));
        EnrollmentResult result;
        synchronized (student) {
            result = enroll ? enrollmentService.enroll(student, courseCode) : enrollmentService.drop(student, courseCode);
        }
        String target = valueOrEmpty(form.get("returnTo"));
        if (target.isEmpty() || !target.startsWith("/") || target.startsWith("//")) target = "/";
        String separator = target.contains("?") ? "&" : "?";
        redirect(exchange, target + separator + "status=" + (result.isSuccess() ? "success" : "error")
                + "&message=" + encode(result.getMessage()));
    }

    private String renderHome(String keyword, List<Course> courses, String message, String status) {
        StringBuilder html = new StringBuilder();
        appendPageStart(html, "大学选课系统");
        appendHeader(html);
        html.append("<main class=\"shell\">");
        appendNotice(html, message, status);
        html.append("<section class=\"summary-grid\">");
        appendStudentPanel(html);
        appendSchedulePanel(html);
        html.append("</section>");
        html.append("<section class=\"panel course-panel\">");
        html.append("<div class=\"panel-title-row\">");
        html.append("<div><h2>课程列表</h2><p>").append(courses.size()).append(" 门课程</p></div>");
        html.append("<form class=\"search\" method=\"get\" action=\"/\">");
        html.append("<input type=\"search\" name=\"q\" value=\"").append(escape(keyword)).append("\" placeholder=\"课程编号、名称或教师\">");
        html.append("<button type=\"submit\">搜索</button>");
        html.append("<a class=\"ghost-button\" href=\"/\">重置</a>");
        html.append("</form>");
        html.append("</div>");
        appendCourseTable(html, courses, "/");
        html.append("</section>");
        html.append("</main>");
        appendPageEnd(html);
        return html.toString();
    }

    private String renderCourseDetail(Course course, String message, String status) {
        StringBuilder html = new StringBuilder();
        appendPageStart(html, course.getCode() + " " + course.getName());
        appendHeader(html);
        html.append("<main class=\"shell narrow\">");
        appendNotice(html, message, status);
        html.append("<a class=\"back-link\" href=\"/\">返回课程列表</a>");
        html.append("<section class=\"panel detail\">");
        html.append("<div class=\"detail-title\">");
        html.append("<div><p class=\"eyebrow\">").append(escape(course.getCode())).append("</p>");
        html.append("<h2>").append(escape(course.getName())).append("</h2></div>");
        appendActionForm(html, course, "/course?code=" + encode(course.getCode()));
        html.append("</div>");
        html.append("<dl class=\"detail-list\">");
        appendTerm(html, "授课教师", course.getTeacher());
        appendTerm(html, "课程学分", course.getCredits() + " 学分");
        appendTerm(html, "上课时间", joinObjects(course.getTimeSlots()));
        appendTerm(html, "先修课程", course.getPrerequisiteCodes().isEmpty() ? "无" : joinObjects(course.getPrerequisiteCodes()));
        appendTerm(html, "课程容量", course.getEnrolledStudentIds().size() + "/" + course.getCapacity()
                + "，剩余 " + course.getRemainingCapacity());
        appendTerm(html, "课程简介", course.getDescription());
        html.append("</dl>");
        html.append("</section>");
        html.append("</main>");
        appendPageEnd(html);
        return html.toString();
    }

    private String renderNotFound(String code) {
        StringBuilder html = new StringBuilder();
        appendPageStart(html, "课程不存在");
        appendHeader(html);
        html.append("<main class=\"shell narrow\"><section class=\"panel empty-state\">");
        html.append("<h2>没有找到课程</h2>");
        html.append("<p>课程编号 ").append(escape(code)).append(" 不存在。</p>");
        html.append("<a class=\"primary-link\" href=\"/\">返回首页</a>");
        html.append("</section></main>");
        appendPageEnd(html);
        return html.toString();
    }

    private void appendPageStart(StringBuilder html, String title) {
        html.append("<!doctype html><html lang=\"zh-CN\"><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>").append(escape(title)).append("</title>");
        html.append("<style>").append(styles()).append("</style>");
        html.append("</head><body>");
    }

    private void appendHeader(StringBuilder html) {
        html.append("<header class=\"topbar\"><div>");
        html.append("<p class=\"eyebrow\">University Course Selection</p>");
        html.append("<h1>大学选课系统</h1>");
        html.append("</div><div class=\"student-badge\">");
        html.append("<span>").append(escape(student.getName())).append("</span>");
        html.append("<strong>").append(escape(student.getMajor())).append("</strong>");
        html.append("</div></header>");
    }

    private void appendPageEnd(StringBuilder html) {
        html.append("</body></html>");
    }

    private void appendNotice(StringBuilder html, String message, String status) {
        if (message == null || message.trim().isEmpty()) return;
        String className = "success".equals(status) ? "notice success" : "notice error";
        html.append("<div class=\"").append(className).append("\">").append(escape(message)).append("</div>");
    }

    private void appendStudentPanel(StringBuilder html) {
        html.append("<section class=\"panel profile-panel\"><h2>学生信息</h2>");
        html.append("<div class=\"profile-name\">").append(escape(student.getName())).append("</div>");
        html.append("<dl class=\"compact-list\">");
        appendTerm(html, "学号", student.getId());
        appendTerm(html, "专业", student.getMajor());
        appendTerm(html, "已修课程", student.getCompletedCourseCodes().isEmpty()
                ? "无" : joinObjects(student.getCompletedCourseCodes()));
        appendTerm(html, "学分上限", student.getMaxCredits() + " 学分");
        html.append("</dl></section>");
    }

    private void appendSchedulePanel(StringBuilder html) {
        List<Course> selected = selectedCourses();
        int totalCredits = totalCredits(selected);
        html.append("<section class=\"panel schedule-panel\"><div class=\"panel-title-row\">");
        html.append("<div><h2>我的课表</h2><p>").append(totalCredits).append(" / ")
                .append(student.getMaxCredits()).append(" 学分</p></div>");
        html.append("</div>");
        if (selected.isEmpty()) {
            html.append("<p class=\"muted\">本学期暂未选课。</p>");
        } else {
            html.append("<div class=\"schedule-list\">");
            for (Course course : selected) {
                html.append("<article class=\"schedule-item\"><div><strong>")
                        .append(escape(course.getCode())).append(" ").append(escape(course.getName()))
                        .append("</strong><p>").append(escape(joinObjects(course.getTimeSlots())))
                        .append("</p></div>");
                appendActionForm(html, course, "/");
                html.append("</article>");
            }
            html.append("</div>");
        }
        html.append("</section>");
    }

    private void appendCourseTable(StringBuilder html, List<Course> courses, String returnTo) {
        html.append("<div class=\"table-wrap\"><table><thead><tr>");
        html.append("<th>课程</th><th>教师</th><th>时间</th><th>先修课</th><th>余量</th><th>操作</th>");
        html.append("</tr></thead><tbody>");
        if (courses.isEmpty()) {
            html.append("<tr><td colspan=\"6\" class=\"empty-cell\">没有匹配的课程。</td></tr>");
        }
        for (Course course : courses) {
            boolean chosen = student.getSelectedCourseCodes().contains(course.getCode());
            html.append("<tr");
            if (chosen) html.append(" class=\"selected-row\"");
            html.append("><td><a class=\"course-link\" href=\"/course?code=")
                    .append(encode(course.getCode())).append("\">")
                    .append(escape(course.getCode())).append(" ").append(escape(course.getName())).append("</a>");
            if (chosen) html.append("<span class=\"tag chosen\">已选</span>");
            html.append("<p>").append(escape(course.getDescription())).append("</p></td>");
            html.append("<td>").append(escape(course.getTeacher())).append("</td>");
            html.append("<td>").append(escape(joinObjects(course.getTimeSlots()))).append("</td>");
            html.append("<td>").append(escape(course.getPrerequisiteCodes().isEmpty()
                    ? "无" : joinObjects(course.getPrerequisiteCodes()))).append("</td>");
            html.append("<td>").append(course.getRemainingCapacity()).append("/").append(course.getCapacity()).append("</td>");
            html.append("<td>");
            appendActionForm(html, course, returnTo);
            html.append("</td></tr>");
        }
        html.append("</tbody></table></div>");
    }

    private void appendActionForm(StringBuilder html, Course course, String returnTo) {
        boolean chosen = student.getSelectedCourseCodes().contains(course.getCode());
        html.append("<form class=\"inline-form\" method=\"post\" action=\"")
                .append(chosen ? "/drop" : "/enroll").append("\">");
        html.append("<input type=\"hidden\" name=\"courseCode\" value=\"").append(escape(course.getCode())).append("\">");
        html.append("<input type=\"hidden\" name=\"returnTo\" value=\"").append(escape(returnTo)).append("\">");
        html.append("<button class=\"").append(chosen ? "danger" : "primary").append("\" type=\"submit\">")
                .append(chosen ? "退课" : "选课").append("</button>");
        html.append("</form>");
    }

    private void appendTerm(StringBuilder html, String term, String value) {
        html.append("<dt>").append(escape(term)).append("</dt><dd>").append(escape(value)).append("</dd>");
    }

    private List<Course> selectedCourses() {
        List<Course> result = new ArrayList<Course>();
        for (String courseCode : student.getSelectedCourseCodes()) {
            Optional<Course> course = courseRepository.findByCode(courseCode);
            if (course.isPresent()) result.add(course.get());
        }
        return result;
    }

    private int totalCredits(List<Course> courses) {
        int total = 0;
        for (Course course : courses) total += course.getCredits();
        return total;
    }

    private Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        return parseQuery(new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8));
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        if (rawQuery == null || rawQuery.trim().isEmpty()) return values;
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int index = pair.indexOf('=');
            String key = index >= 0 ? pair.substring(0, index) : pair;
            String value = index >= 0 ? pair.substring(index + 1) : "";
            values.put(decode(key), decode(value));
        }
        return values;
    }

    private byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int count;
        while ((count = inputStream.read(data)) != -1) {
            buffer.write(data, 0, count);
        }
        return buffer.toByteArray();
    }

    private void sendHtml(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        OutputStream outputStream = exchange.getResponseBody();
        try {
            outputStream.write(response);
        } finally {
            outputStream.close();
        }
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Allow", "GET, POST");
        sendHtml(exchange, 405, "Method Not Allowed");
    }

    private String joinObjects(Iterable<?> values) {
        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            if (builder.length() > 0) builder.append("，");
            builder.append(value);
        }
        return builder.toString();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(valueOrEmpty(value), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return valueOrEmpty(value);
        }
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(valueOrEmpty(value), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return valueOrEmpty(value);
        }
    }

    private String escape(String value) {
        String source = valueOrEmpty(value);
        StringBuilder builder = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            switch (ch) {
                case '&': builder.append("&amp;"); break;
                case '<': builder.append("&lt;"); break;
                case '>': builder.append("&gt;"); break;
                case '"': builder.append("&quot;"); break;
                case '\'': builder.append("&#39;"); break;
                default: builder.append(ch);
            }
        }
        return builder.toString();
    }

    private String styles() {
        return "body{margin:0;background:#f4f7fb;color:#1f2937;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',Arial,sans-serif;}"
                + ".topbar{background:#15324f;color:#fff;padding:28px 44px;display:flex;justify-content:space-between;align-items:center;gap:24px;}"
                + ".topbar h1{margin:4px 0 0;font-size:28px;font-weight:700;}.eyebrow{margin:0;color:#7da8ce;font-size:13px;text-transform:uppercase;}"
                + ".student-badge{background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.22);border-radius:8px;padding:10px 14px;min-width:190px;}"
                + ".student-badge span,.student-badge strong{display:block;}.student-badge span{font-size:17px}.student-badge strong{font-size:13px;color:#cfe0ef;margin-top:3px;}"
                + ".shell{max-width:1180px;margin:28px auto 48px;padding:0 22px;}.shell.narrow{max-width:900px}.summary-grid{display:grid;grid-template-columns:310px minmax(0,1fr);gap:18px;margin-bottom:18px;}"
                + ".panel{background:#fff;border:1px solid #dbe4ef;border-radius:8px;box-shadow:0 12px 28px rgba(25,45,70,.07);padding:22px;}.panel h2{margin:0 0 12px;font-size:21px;color:#15324f;}"
                + ".panel-title-row{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:14px;}.panel-title-row p{margin:3px 0 0;color:#64748b;}"
                + ".profile-name{font-size:28px;font-weight:700;color:#15324f;margin:6px 0 14px;}.compact-list,.detail-list{display:grid;grid-template-columns:92px minmax(0,1fr);gap:9px 14px;margin:0;}"
                + "dt{color:#64748b;}dd{margin:0;color:#111827;}.schedule-list{display:grid;gap:10px;}.schedule-item{display:flex;align-items:center;justify-content:space-between;gap:12px;border:1px solid #e2e8f0;border-radius:8px;padding:12px;background:#f8fafc;}"
                + ".schedule-item p{margin:4px 0 0;color:#64748b;}.muted{color:#64748b;margin:0}.search{display:flex;gap:8px;align-items:center;flex-wrap:wrap;}.search input{width:260px;max-width:100%;border:1px solid #cbd5e1;border-radius:7px;padding:10px 12px;font-size:14px;}"
                + "button,.ghost-button,.primary-link{border:0;border-radius:7px;padding:10px 14px;font-size:14px;text-decoration:none;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;min-height:38px;box-sizing:border-box;}"
                + "button{background:#15324f;color:#fff;}button.primary{background:#256f63;}button.danger{background:#b42318;}.ghost-button{background:#edf3f8;color:#15324f;border:1px solid #cbd5e1;}.primary-link{background:#15324f;color:#fff;}"
                + ".table-wrap{overflow:auto;border:1px solid #dbe4ef;border-radius:8px;}table{width:100%;border-collapse:collapse;min-width:820px;}th,td{padding:13px 14px;text-align:left;border-bottom:1px solid #e2e8f0;vertical-align:top;}th{background:#eef5fb;color:#15324f;font-weight:700;}"
                + "td p{margin:6px 0 0;color:#64748b;line-height:1.45}.course-link{color:#15324f;font-weight:700;text-decoration:none;}.course-link:hover{text-decoration:underline}.tag{display:inline-block;margin-left:8px;border-radius:999px;padding:3px 8px;font-size:12px;}.chosen{background:#dff3ed;color:#14604f;}.selected-row{background:#fbfdfc;}"
                + ".inline-form{margin:0;}.notice{margin-bottom:18px;border-radius:8px;padding:12px 14px;font-weight:600;}.notice.success{background:#e7f6ee;color:#17614f;border:1px solid #b8e2cf;}.notice.error{background:#fff0ed;color:#9f2418;border:1px solid #f2c0b8;}"
                + ".back-link{display:inline-block;margin-bottom:14px;color:#15324f;text-decoration:none;font-weight:700;}.detail-title{display:flex;align-items:flex-start;justify-content:space-between;gap:18px;border-bottom:1px solid #e2e8f0;padding-bottom:18px;margin-bottom:18px;}.detail-title h2{font-size:28px;margin:4px 0 0;}.empty-cell,.empty-state{text-align:center;color:#64748b;}"
                + "@media (max-width:800px){.topbar{padding:22px;display:block}.student-badge{margin-top:18px}.summary-grid{grid-template-columns:1fr}.panel-title-row,.detail-title{display:block}.search input{width:100%}.shell{margin-top:18px;padding:0 14px}.panel{padding:18px}.compact-list,.detail-list{grid-template-columns:1fr;gap:4px}.schedule-item{display:block}.schedule-item form{margin-top:10px}}";
    }
}
