package com.attendance.server;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.Course;
import com.attendance.model.Student;
import com.attendance.model.User;
import com.attendance.service.AttendanceService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, high-performance embedded HTTP server for the Attendance Management System.
 * Serves both the REST API (with Admin & Authentication support) and the HTML/CSS/JS frontend.
 */
public class AttendanceHttpServer {
    private final int port;
    private final AttendanceService service;
    private final Path webRoot;
    private HttpServer server;

    public AttendanceHttpServer(int port, AttendanceService service, String webRootPath) {
        this.port = port;
        this.service = service;
        this.webRoot = Paths.get(webRootPath).toAbsolutePath().normalize();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor() != null 
            ? Executors.newCachedThreadPool() 
            : Executors.newFixedThreadPool(10));

        // Authentication & Admin Endpoints
        server.createContext("/api/auth/login", this::handleLogin);
        server.createContext("/api/auth/signup", this::handleSignup);
        server.createContext("/api/admin/users", this::handleAdminUsers);
        server.createContext("/api/admin/settings", this::handleAdminSettings);
        server.createContext("/api/admin/reset", this::handleAdminReset);
        server.createContext("/api/admin/demo-data", this::handleAdminDemoData);

        // Standard Attendance API Endpoints
        server.createContext("/api/stats", this::handleStats);
        server.createContext("/api/students", this::handleStudents);
        server.createContext("/api/courses", this::handleCourses);
        server.createContext("/api/attendance", this::handleAttendance);
        server.createContext("/api/defaulters", this::handleDefaulters);
        server.createContext("/api/export/csv", this::handleExportCsv);

        // Static files handler (serves HTML, CSS, JS)
        server.createContext("/", this::handleStatic);

        server.start();
        System.out.println("==============================================================");
        System.out.println("   STUDENT ATTENDANCE SYSTEM (ADMIN & FACULTY PORTAL)         ");
        System.out.println("==============================================================");
        System.out.println("-> Server started successfully on: http://localhost:" + port + "/");
        System.out.println("-> Web root directory: " + webRoot);
        System.out.println("-> Ready to accept requests.");
        System.out.println("==============================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
            return;
        }

        String body = readRequestBody(exchange);
        String username = extractJsonString(body, "username");
        String password = extractJsonString(body, "password");

        User user = service.authenticate(username, password);
        if (user != null) {
            String token = "sess_" + UUID.randomUUID().toString();
            String res = String.format(
                "{\"success\":true,\"token\":\"%s\",\"user\":%s}",
                token, user.toJson()
            );
            sendResponse(exchange, 200, "application/json", res.getBytes(StandardCharsets.UTF_8));
        } else {
            sendResponse(exchange, 401, "application/json", "{\"success\":false,\"error\":\"Invalid username or password\"}".getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleSignup(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
            return;
        }

        String body = readRequestBody(exchange);
        String username = extractJsonString(body, "username");
        String password = extractJsonString(body, "password");
        String name = extractJsonString(body, "name");
        String email = extractJsonString(body, "email");
        String department = extractJsonString(body, "department");
        String role = extractJsonString(body, "role");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            sendResponse(exchange, 400, "application/json", "{\"success\":false,\"error\":\"Username, password, and name are required.\"}".getBytes(StandardCharsets.UTF_8));
            return;
        }

        if (service.userExists(username.trim())) {
            sendResponse(exchange, 400, "application/json", "{\"success\":false,\"error\":\"Username already exists. Please choose another username.\"}".getBytes(StandardCharsets.UTF_8));
            return;
        }

        if (role == null || role.trim().isEmpty()) {
            role = "FACULTY";
        }

        User newUser = new User(
            "u_" + UUID.randomUUID().toString().substring(0, 8),
            username.trim(),
            password.trim(),
            name.trim(),
            role.trim().toUpperCase(),
            email != null ? email.trim() : "",
            department != null ? department.trim() : "General"
        );

        User saved = service.addUser(newUser);
        String token = "sess_" + UUID.randomUUID().toString();
        String res = String.format(
            "{\"success\":true,\"token\":\"%s\",\"user\":%s}",
            token, saved.toJson()
        );
        sendResponse(exchange, 201, "application/json", res.getBytes(StandardCharsets.UTF_8));
    }

    private void handleAdminUsers(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String method = exchange.getRequestMethod().toUpperCase();

        if ("GET".equals(method)) {
            List<User> users = service.getAllUsers();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < users.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(users.get(i).toJson());
            }
            sb.append("]");
            sendResponse(exchange, 200, "application/json", sb.toString().getBytes(StandardCharsets.UTF_8));
        } else if ("POST".equals(method)) {
            String body = readRequestBody(exchange);
            User u = new User();
            u.setUsername(extractJsonString(body, "username"));
            u.setPassword(extractJsonString(body, "password"));
            u.setName(extractJsonString(body, "name"));
            u.setRole(extractJsonString(body, "role"));
            u.setEmail(extractJsonString(body, "email"));
            u.setDepartment(extractJsonString(body, "department"));

            if (u.getUsername() == null || u.getName() == null) {
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Username and name required\"}".getBytes(StandardCharsets.UTF_8));
                return;
            }
            User saved = service.addUser(u);
            sendResponse(exchange, 201, "application/json", saved.toJson().getBytes(StandardCharsets.UTF_8));
        } else if ("DELETE".equals(method)) {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String username = params.get("username");
            if (username != null && service.deleteUser(username)) {
                sendResponse(exchange, 200, "application/json", "{\"success\":true}".getBytes(StandardCharsets.UTF_8));
            } else {
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Cannot delete user or not found\"}".getBytes(StandardCharsets.UTF_8));
            }
        } else {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleAdminSettings(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String method = exchange.getRequestMethod().toUpperCase();

        if ("GET".equals(method)) {
            Map<String, String> settings = service.getSettings();
            StringBuilder sb = new StringBuilder("{");
            int c = 0;
            for (Map.Entry<String, String> e : settings.entrySet()) {
                if (c > 0) sb.append(",");
                sb.append(String.format("\"%s\":\"%s\"", escape(e.getKey()), escape(e.getValue())));
                c++;
            }
            sb.append("}");
            sendResponse(exchange, 200, "application/json", sb.toString().getBytes(StandardCharsets.UTF_8));
        } else if ("POST".equals(method)) {
            String body = readRequestBody(exchange);
            Map<String, String> newSettings = new HashMap<>();
            String thresh = extractJsonString(body, "minAttendanceThreshold");
            String inst = extractJsonString(body, "institutionName");
            String term = extractJsonString(body, "academicTerm");

            if (thresh != null) newSettings.put("minAttendanceThreshold", thresh);
            if (inst != null) newSettings.put("institutionName", inst);
            if (term != null) newSettings.put("academicTerm", term);

            service.updateSettings(newSettings);
            sendResponse(exchange, 200, "application/json", "{\"success\":true}".getBytes(StandardCharsets.UTF_8));
        } else {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleAdminReset(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
            return;
        }
        service.resetToDefaultData();
        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Database cleared to clean initial state\"}".getBytes(StandardCharsets.UTF_8));
    }

    private void handleAdminDemoData(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
            return;
        }
        service.loadDemoSampleData();
        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Demo data loaded successfully\"}".getBytes(StandardCharsets.UTF_8));
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        Map<String, Object> stats = service.getOverallStats();
        Map<?, ?> settings = (Map<?, ?>) stats.get("settings");
        String instName = settings != null && settings.containsKey("institutionName") ? settings.get("institutionName").toString() : "Vishwa Vishwani Institute Of System And Management";

        String json = String.format(
            "{\"totalStudents\":%d,\"totalCourses\":%d,\"totalSessions\":%d,\"totalFaculty\":%d,\"overallAttendanceRate\":%.1f,\"defaulterCount\":%d,\"institutionName\":\"%s\"}",
            (int) stats.get("totalStudents"),
            (int) stats.get("totalCourses"),
            (int) stats.get("totalSessions"),
            (long) stats.get("totalFaculty"),
            (double) stats.get("overallAttendanceRate"),
            (int) stats.get("defaulterCount"),
            escape(instName)
        );
        sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    private void handleStudents(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String method = exchange.getRequestMethod().toUpperCase();

        if ("GET".equals(method)) {
            List<Student> students = service.getAllStudents();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < students.size(); i++) {
                Student s = students.get(i);
                Map<String, Object> summary = service.getStudentAttendanceSummary(s.getId(), null);
                double pct = (double) summary.get("percentage");
                if (i > 0) sb.append(",");
                sb.append(String.format(
                    "{\"id\":\"%s\",\"rollNo\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"department\":\"%s\",\"semester\":%d,\"attendancePct\":%.1f,\"totalClasses\":%d,\"attended\":%d}",
                    escape(s.getId()), escape(s.getRollNo()), escape(s.getName()), escape(s.getEmail()),
                    escape(s.getDepartment()), s.getSemester(), pct, (int) summary.get("totalClasses"), (int) summary.get("present")
                ));
            }
            sb.append("]");
            sendResponse(exchange, 200, "application/json", sb.toString().getBytes(StandardCharsets.UTF_8));
        } else if ("POST".equals(method)) {
            String body = readRequestBody(exchange);
            Student s = parseStudent(body);
            if (s.getName() == null || s.getRollNo() == null) {
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Name and Roll No are required\"}".getBytes(StandardCharsets.UTF_8));
                return;
            }
            Student saved = service.addStudent(s);
            sendResponse(exchange, 201, "application/json", saved.toJson().getBytes(StandardCharsets.UTF_8));
        } else if ("DELETE".equals(method)) {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String id = params.get("id");
            if (id != null && service.deleteStudent(id)) {
                sendResponse(exchange, 200, "application/json", "{\"success\":true}".getBytes(StandardCharsets.UTF_8));
            } else {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Student not found\"}".getBytes(StandardCharsets.UTF_8));
            }
        } else {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleCourses(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String method = exchange.getRequestMethod().toUpperCase();

        if ("GET".equals(method)) {
            List<Course> courses = service.getAllCourses();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < courses.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(courses.get(i).toJson());
            }
            sb.append("]");
            sendResponse(exchange, 200, "application/json", sb.toString().getBytes(StandardCharsets.UTF_8));
        } else if ("POST".equals(method)) {
            String body = readRequestBody(exchange);
            Course c = parseCourse(body);
            Course saved = service.addCourse(c);
            sendResponse(exchange, 201, "application/json", saved.toJson().getBytes(StandardCharsets.UTF_8));
        } else {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleAttendance(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String method = exchange.getRequestMethod().toUpperCase();

        if ("GET".equals(method)) {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String courseId = params.get("courseId");
            String date = params.get("date");

            List<AttendanceRecord> records = service.getAllRecords();
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (AttendanceRecord r : records) {
                if (courseId != null && !courseId.isEmpty() && !r.getCourseId().equals(courseId)) continue;
                if (date != null && !date.isEmpty() && !r.getDate().equals(date)) continue;

                if (!first) sb.append(",");
                sb.append(r.toJson());
                first = false;
            }
            sb.append("]");
            sendResponse(exchange, 200, "application/json", sb.toString().getBytes(StandardCharsets.UTF_8));
        } else if ("POST".equals(method)) {
            String body = readRequestBody(exchange);
            AttendanceRecord rec = parseAttendanceRecord(body);
            AttendanceRecord saved = service.saveRecord(rec);
            sendResponse(exchange, 200, "application/json", saved.toJson().getBytes(StandardCharsets.UTF_8));
        } else {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleDefaulters(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        double threshold = 75.0;
        if (params.containsKey("threshold")) {
            try { threshold = Double.parseDouble(params.get("threshold")); } catch (Exception ignored) {}
        }
        String courseId = params.get("courseId");

        List<Map<String, Object>> defaulters = service.getDefaulters(threshold, courseId);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < defaulters.size(); i++) {
            Map<String, Object> d = defaulters.get(i);
            Student s = (Student) d.get("student");
            if (i > 0) sb.append(",");
            sb.append(String.format(
                "{\"student\":%s,\"totalClasses\":%d,\"attended\":%d,\"percentage\":%.1f,\"severity\":\"%s\",\"classesNeeded\":%d}",
                s.toJson(), (int) d.get("totalClasses"), (int) d.get("attended"),
                (double) d.get("percentage"), (String) d.get("severity"), (int) d.get("classesNeededToRecover")
            ));
        }
        sb.append("]");
        sendResponse(exchange, 200, "application/json", sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void handleExportCsv(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String courseId = params.get("courseId");
        String csv = service.generateCsvReport(courseId);

        exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"attendance_report.csv\"");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        String uriPath = exchange.getRequestURI().getPath();
        if (uriPath == null || uriPath.equals("/") || uriPath.isEmpty()) {
            uriPath = "/index.html";
        }

        Path target = webRoot.resolve(uriPath.substring(1)).normalize();
        if (!target.startsWith(webRoot) || !Files.exists(target) || Files.isDirectory(target)) {
            target = webRoot.resolve("index.html");
        }

        if (Files.exists(target)) {
            String mime = getMimeType(target.getFileName().toString());
            byte[] bytes = Files.readAllBytes(target);
            sendResponse(exchange, 200, mime, bytes);
        } else {
            String msg = "404 Not Found";
            sendResponse(exchange, 404, "text/plain", msg.getBytes(StandardCharsets.UTF_8));
        }
    }

    private boolean handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int r;
            while ((r = is.read(buf)) != -1) {
                bos.write(buf, 0, r);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                try {
                    String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    map.put(k, v);
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private Student parseStudent(String json) {
        Student s = new Student();
        s.setId(extractJsonString(json, "id"));
        s.setRollNo(extractJsonString(json, "rollNo"));
        s.setName(extractJsonString(json, "name"));
        s.setEmail(extractJsonString(json, "email"));
        s.setDepartment(extractJsonString(json, "department"));
        String sem = extractJsonValue(json, "semester");
        if (sem != null) {
            try { s.setSemester(Integer.parseInt(sem.trim())); } catch (Exception ignored) {}
        }
        return s;
    }

    private Course parseCourse(String json) {
        Course c = new Course();
        c.setId(extractJsonString(json, "id"));
        c.setCode(extractJsonString(json, "code"));
        c.setName(extractJsonString(json, "name"));
        c.setDepartment(extractJsonString(json, "department"));
        c.setFaculty(extractJsonString(json, "faculty"));
        return c;
    }

    private AttendanceRecord parseAttendanceRecord(String json) {
        AttendanceRecord r = new AttendanceRecord();
        r.setId(extractJsonString(json, "id"));
        r.setCourseId(extractJsonString(json, "courseId"));
        r.setDate(extractJsonString(json, "date"));
        r.setTimeSlot(extractJsonString(json, "timeSlot"));
        r.setNotes(extractJsonString(json, "notes"));

        int recIdx = json.indexOf("\"records\"");
        if (recIdx != -1) {
            int braceStart = json.indexOf("{", recIdx);
            int braceEnd = json.indexOf("}", braceStart);
            if (braceStart != -1 && braceEnd != -1) {
                String inner = json.substring(braceStart + 1, braceEnd);
                Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
                Matcher m = pattern.matcher(inner);
                while (m.find()) {
                    r.setStudentStatus(m.group(1), m.group(2));
                }
            }
        }
        return r;
    }

    private String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1);
        return null;
    }

    private String extractJsonValue(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([^,}\\]]+)");
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1).replace("\"", "");
        return null;
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html; charset=UTF-8";
        if (fileName.endsWith(".css")) return "text/css; charset=UTF-8";
        if (fileName.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (fileName.endsWith(".json")) return "application/json; charset=UTF-8";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".ico")) return "image/x-icon";
        return "text/plain; charset=UTF-8";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
