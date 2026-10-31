package com.attendance.storage;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.Course;
import com.attendance.model.Student;
import com.attendance.model.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles persistent in-memory data store with JSON file backing.
 * Fresh default state with secure administrator credentials (admin / Admin@123)
 * and zero pre-populated dummy data so users create their own records.
 */
public class DataStore {
    private final Path filePath;
    private final Map<String, Student> students = new ConcurrentHashMap<>();
    private final Map<String, Course> courses = new ConcurrentHashMap<>();
    private final Map<String, AttendanceRecord> records = new ConcurrentHashMap<>();
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> settings = new ConcurrentHashMap<>();

    public DataStore(String dataDirPath) {
        this.filePath = Paths.get(dataDirPath, "attendance_database.json");
        init();
    }

    private synchronized void init() {
        try {
            if (Files.exists(filePath)) {
                loadFromFile();
            } else {
                Files.createDirectories(filePath.getParent());
                initCleanDatabase();
                saveToFile();
            }
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to load data from file: " + e.getMessage() + ". Initializing clean database...");
            initCleanDatabase();
            saveToFile();
        }
    }

    /**
     * Initializes clean slate database with institutional settings and the root Administrator account.
     * No pre-populated dummy students, courses, or records.
     */
    public synchronized void initCleanDatabase() {
        students.clear();
        courses.clear();
        records.clear();
        users.clear();
        settings.clear();

        // Institutional Defaults
        settings.put("institutionName", "Vishwa Vishwani Institute Of System And Management");
        settings.put("minAttendanceThreshold", "75.0");
        settings.put("academicTerm", "Academic Year 2026-27");

        // Root Administrator: admin / Admin@123
        User admin = new User("u_admin", "admin", "Admin@123", "System Administrator", "ADMIN", "admin@institution.edu", "Administration");
        users.put(admin.getUsername().toLowerCase(), admin);
    }

    /**
     * Optional sample dataset loader (can be triggered manually by admin if desired).
     */
    public synchronized void loadDemoSampleData() {
        initCleanDatabase();

        Course c1 = new Course("c1", "CS301", "Database Management Systems", "Computer Science", "Prof. Alan Turing");
        Course c2 = new Course("c2", "CS302", "Operating Systems", "Computer Science", "Prof. Linus Torvalds");
        courses.put(c1.getId(), c1);
        courses.put(c2.getId(), c2);

        String[][] studentData = {
            {"s1", "CS-101", "Aarav Sharma", "aarav@institution.edu", "Computer Science", "5"},
            {"s2", "CS-102", "Aditi Patel", "aditi@institution.edu", "Computer Science", "5"},
            {"s3", "CS-103", "Bhavani Shankar", "bhavani@institution.edu", "Computer Science", "5"},
            {"s4", "CS-104", "Devika Nambiar", "devika@institution.edu", "Computer Science", "5"},
            {"s5", "CS-105", "Ishaan Verma", "ishaan@institution.edu", "Computer Science", "5"}
        };

        for (String[] row : studentData) {
            Student s = new Student(row[0], row[1], row[2], row[3], row[4], Integer.parseInt(row[5]));
            students.put(s.getId(), s);
        }

        saveToFile();
    }

    public synchronized void saveToFile() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");

            // Settings
            sb.append("  \"settings\": {\n");
            int sCount = 0;
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                if (sCount > 0) sb.append(",\n");
                sb.append(String.format("    \"%s\": \"%s\"", escape(entry.getKey()), escape(entry.getValue())));
                sCount++;
            }
            sb.append("\n  },\n");

            // Users
            sb.append("  \"users\": [\n");
            List<User> userList = new ArrayList<>(users.values());
            for (int i = 0; i < userList.size(); i++) {
                User u = userList.get(i);
                sb.append(String.format(
                    "    {\"id\":\"%s\",\"username\":\"%s\",\"password\":\"%s\",\"name\":\"%s\",\"role\":\"%s\",\"email\":\"%s\",\"department\":\"%s\"}",
                    escape(u.getId()), escape(u.getUsername()), escape(u.getPassword()), escape(u.getName()),
                    escape(u.getRole()), escape(u.getEmail()), escape(u.getDepartment())
                ));
                if (i < userList.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n");

            // Students
            sb.append("  \"students\": [\n");
            List<Student> studentList = new ArrayList<>(students.values());
            for (int i = 0; i < studentList.size(); i++) {
                sb.append("    ").append(studentList.get(i).toJson());
                if (i < studentList.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n");

            // Courses
            sb.append("  \"courses\": [\n");
            List<Course> courseList = new ArrayList<>(courses.values());
            for (int i = 0; i < courseList.size(); i++) {
                sb.append("    ").append(courseList.get(i).toJson());
                if (i < courseList.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n");

            // Records
            sb.append("  \"records\": [\n");
            List<AttendanceRecord> recList = new ArrayList<>(records.values());
            for (int i = 0; i < recList.size(); i++) {
                sb.append("    ").append(recList.get(i).toJson());
                if (i < recList.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ]\n");
            sb.append("}");

            Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[DataStore] Error saving database: " + e.getMessage());
        }
    }

    private synchronized void loadFromFile() {
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            if (content == null || content.trim().isEmpty()) {
                initCleanDatabase();
                saveToFile();
                return;
            }

            // Extract users
            Pattern uPattern = Pattern.compile("\\{\"id\":\"([^\"]+)\",\"username\":\"([^\"]+)\",\"password\":\"([^\"]+)\",\"name\":\"([^\"]+)\",\"role\":\"([^\"]+)\",\"email\":\"([^\"]+)\",\"department\":\"([^\"]+)\"\\}");
            Matcher uMatcher = uPattern.matcher(content);
            while (uMatcher.find()) {
                User u = new User(
                    uMatcher.group(1),
                    uMatcher.group(2),
                    uMatcher.group(3),
                    uMatcher.group(4),
                    uMatcher.group(5),
                    uMatcher.group(6),
                    uMatcher.group(7)
                );
                users.put(u.getUsername().toLowerCase(), u);
            }

            // Extract students
            Pattern sPattern = Pattern.compile("\\{\"id\":\"([^\"]+)\",\"rollNo\":\"([^\"]+)\",\"name\":\"([^\"]+)\",\"email\":\"([^\"]+)\",\"department\":\"([^\"]+)\",\"semester\":(\\d+)\\}");
            Matcher sMatcher = sPattern.matcher(content);
            while (sMatcher.find()) {
                Student s = new Student(
                    sMatcher.group(1),
                    sMatcher.group(2),
                    sMatcher.group(3),
                    sMatcher.group(4),
                    sMatcher.group(5),
                    Integer.parseInt(sMatcher.group(6))
                );
                students.put(s.getId(), s);
            }

            // Extract courses
            Pattern cPattern = Pattern.compile("\\{\"id\":\"([^\"]+)\",\"code\":\"([^\"]+)\",\"name\":\"([^\"]+)\",\"department\":\"([^\"]+)\",\"faculty\":\"([^\"]+)\"\\}");
            Matcher cMatcher = cPattern.matcher(content);
            while (cMatcher.find()) {
                Course c = new Course(
                    cMatcher.group(1),
                    cMatcher.group(2),
                    cMatcher.group(3),
                    cMatcher.group(4),
                    cMatcher.group(5)
                );
                courses.put(c.getId(), c);
            }

            // Extract records
            Pattern rPattern = Pattern.compile("\\{\"id\":\"([^\"]+)\",\"courseId\":\"([^\"]+)\",\"date\":\"([^\"]+)\",\"timeSlot\":\"([^\"]+)\",\"notes\":\"([^\"]*)\",\"timestamp\":(\\d+),\"records\":\\{([^\\}]*)\\}\\}");
            Matcher rMatcher = rPattern.matcher(content);
            while (rMatcher.find()) {
                AttendanceRecord r = new AttendanceRecord(
                    rMatcher.group(1),
                    rMatcher.group(2),
                    rMatcher.group(3),
                    rMatcher.group(4),
                    rMatcher.group(5)
                );
                r.setTimestamp(Long.parseLong(rMatcher.group(6)));
                String innerMap = rMatcher.group(7);
                if (!innerMap.isEmpty()) {
                    Pattern mapEntry = Pattern.compile("\"([^\"]+)\":\"([^\"]+)\"");
                    Matcher mEntry = mapEntry.matcher(innerMap);
                    while (mEntry.find()) {
                        r.setStudentStatus(mEntry.group(1), mEntry.group(2));
                    }
                }
                records.put(r.getId(), r);
            }

            // Always ensure root admin exists with Admin@123 password
            if (!users.containsKey("admin")) {
                User admin = new User("u_admin", "admin", "Admin@123", "System Administrator", "ADMIN", "admin@institution.edu", "Administration");
                users.put("admin", admin);
                saveToFile();
            } else {
                // Ensure admin password is Admin@123 as requested
                User admin = users.get("admin");
                if (!"Admin@123".equals(admin.getPassword())) {
                    admin.setPassword("Admin@123");
                    saveToFile();
                }
            }
        } catch (Exception e) {
            System.err.println("[DataStore] Parse error: " + e.getMessage() + ". Initializing clean database.");
            initCleanDatabase();
            saveToFile();
        }
    }

    public User authenticate(String username, String password) {
        if (username == null || password == null) return null;
        User u = users.get(username.trim().toLowerCase());
        if (u != null && u.getPassword().equals(password.trim())) {
            return u;
        }
        return null;
    }

    public boolean userExists(String username) {
        if (username == null) return false;
        return users.containsKey(username.trim().toLowerCase());
    }

    public List<User> getUsers() {
        return new ArrayList<>(users.values());
    }

    public synchronized User addUser(User u) {
        if (u.getId() == null || u.getId().isEmpty()) {
            u.setId("u_" + UUID.randomUUID().toString().substring(0, 8));
        }
        users.put(u.getUsername().toLowerCase(), u);
        saveToFile();
        return u;
    }

    public synchronized boolean deleteUser(String username) {
        if ("admin".equalsIgnoreCase(username)) {
            return false; // Root admin cannot be deleted
        }
        User removed = users.remove(username.toLowerCase());
        if (removed != null) {
            saveToFile();
            return true;
        }
        return false;
    }

    public Map<String, String> getSettings() {
        return new HashMap<>(settings);
    }

    public synchronized void updateSettings(Map<String, String> newSettings) {
        settings.putAll(newSettings);
        saveToFile();
    }

    public List<Student> getStudents() {
        List<Student> list = new ArrayList<>(students.values());
        list.sort(Comparator.comparing(Student::getRollNo));
        return list;
    }

    public Student getStudent(String id) {
        return students.get(id);
    }

    public synchronized Student addStudent(Student s) {
        if (s.getId() == null || s.getId().trim().isEmpty()) {
            s.setId("s_" + UUID.randomUUID().toString().substring(0, 8));
        }
        students.put(s.getId(), s);
        saveToFile();
        return s;
    }

    public synchronized boolean deleteStudent(String id) {
        Student removed = students.remove(id);
        if (removed != null) {
            for (AttendanceRecord rec : records.values()) {
                rec.getRecords().remove(id);
            }
            saveToFile();
            return true;
        }
        return false;
    }

    public List<Course> getCourses() {
        List<Course> list = new ArrayList<>(courses.values());
        list.sort(Comparator.comparing(Course::getCode));
        return list;
    }

    public Course getCourse(String id) {
        return courses.get(id);
    }

    public synchronized Course addCourse(Course c) {
        if (c.getId() == null || c.getId().trim().isEmpty()) {
            c.setId("c_" + UUID.randomUUID().toString().substring(0, 8));
        }
        courses.put(c.getId(), c);
        saveToFile();
        return c;
    }

    public List<AttendanceRecord> getRecords() {
        List<AttendanceRecord> list = new ArrayList<>(records.values());
        list.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return list;
    }

    public synchronized AttendanceRecord saveRecord(AttendanceRecord rec) {
        if (rec.getId() == null || rec.getId().trim().isEmpty()) {
            for (AttendanceRecord existing : records.values()) {
                if (existing.getCourseId().equals(rec.getCourseId()) && existing.getDate().equals(rec.getDate())) {
                    rec.setId(existing.getId());
                    break;
                }
            }
            if (rec.getId() == null || rec.getId().trim().isEmpty()) {
                rec.setId("rec_" + UUID.randomUUID().toString().substring(0, 8));
            }
        }
        records.put(rec.getId(), rec);
        saveToFile();
        return rec;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
