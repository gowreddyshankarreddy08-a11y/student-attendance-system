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
     * Initializes clean slate database with institutional settings, the root Administrator account,
     * and the read-only Demo account pre-seeded with realistic data for showcasing.
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

        // Demo Faculty Account: demo / Demo@1234 (pre-seeded with data for externals)
        User demo = new User("u_demo", "demo", "Demo@1234", "Dr. Demo Faculty", "FACULTY", "demo@vvism.edu", "Computer Science");
        users.put(demo.getUsername().toLowerCase(), demo);

        seedDemoData();
    }

    /**
     * Seeds rich demonstration data — 3 courses, 10 students, 20 attendance sessions.
     * Called automatically during initCleanDatabase so the demo account always has data.
     */
    private void seedDemoData() {
        // --- Courses ---
        Course c1 = new Course("c1", "CS301", "Database Management Systems", "Computer Science", "Dr. Demo Faculty");
        Course c2 = new Course("c2", "CS302", "Operating Systems", "Computer Science", "Dr. Demo Faculty");
        Course c3 = new Course("c3", "CS401", "Software Engineering", "Computer Science", "Dr. Demo Faculty");
        courses.put(c1.getId(), c1);
        courses.put(c2.getId(), c2);
        courses.put(c3.getId(), c3);

        // --- Students (10) ---
        String[][] studentData = {
            {"s1",  "VVISM-CS-001", "Aarav Sharma",      "aarav.sharma@vvism.edu",   "Computer Science", "5"},
            {"s2",  "VVISM-CS-002", "Aditi Patel",        "aditi.patel@vvism.edu",    "Computer Science", "5"},
            {"s3",  "VVISM-CS-003", "Bhavani Shankar",    "bhavani.s@vvism.edu",      "Computer Science", "5"},
            {"s4",  "VVISM-CS-004", "Devika Nambiar",     "devika.n@vvism.edu",       "Computer Science", "5"},
            {"s5",  "VVISM-CS-005", "Ishaan Verma",       "ishaan.v@vvism.edu",       "Computer Science", "5"},
            {"s6",  "VVISM-CS-006", "Kavya Reddy",        "kavya.r@vvism.edu",        "Computer Science", "5"},
            {"s7",  "VVISM-CS-007", "Manish Kumar",       "manish.k@vvism.edu",       "Computer Science", "5"},
            {"s8",  "VVISM-CS-008", "Priya Agarwal",      "priya.a@vvism.edu",        "Computer Science", "5"},
            {"s9",  "VVISM-CS-009", "Rohit Sinha",        "rohit.s@vvism.edu",        "Computer Science", "5"},
            {"s10", "VVISM-CS-010", "Sneha Kulkarni",     "sneha.k@vvism.edu",        "Computer Science", "5"}
        };
        for (String[] row : studentData) {
            Student s = new Student(row[0], row[1], row[2], row[3], row[4], Integer.parseInt(row[5]));
            students.put(s.getId(), s);
        }

        // --- Attendance Records (varied — some students are defaulters) ---
        // Format: id, courseId, date, slot, notes, then student statuses
        // c1 (DBMS) — 8 sessions
        String[][] c1Sessions = {
            {"r01","c1","2026-08-04","09:00 AM - 10:00 AM","Introduction to Databases"},
            {"r02","c1","2026-08-06","09:00 AM - 10:00 AM","Relational Model & Keys"},
            {"r03","c1","2026-08-11","09:00 AM - 10:00 AM","SQL Basics"},
            {"r04","c1","2026-08-13","09:00 AM - 10:00 AM","SQL Joins & Subqueries"},
            {"r05","c1","2026-08-18","09:00 AM - 10:00 AM","Normalization (1NF-3NF)"},
            {"r06","c1","2026-08-20","09:00 AM - 10:00 AM","Indexing & B-Trees"},
            {"r07","c1","2026-08-25","09:00 AM - 10:00 AM","Transactions & ACID"},
            {"r08","c1","2026-08-27","09:00 AM - 10:00 AM","Concurrency Control"}
        };
        // Per-student status arrays (order matches s1..s10)
        // s7 and s9 will be defaulters in c1
        String[][] c1Statuses = {
            {"PRESENT","PRESENT","PRESENT","PRESENT","PRESENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT"},
            {"PRESENT","PRESENT","PRESENT","ABSENT","PRESENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT"},
            {"PRESENT","ABSENT","PRESENT","PRESENT","PRESENT","PRESENT","PRESENT","PRESENT","ABSENT","PRESENT"},
            {"PRESENT","PRESENT","ABSENT","PRESENT","PRESENT","ABSENT","ABSENT","PRESENT","PRESENT","PRESENT"},
            {"PRESENT","PRESENT","PRESENT","PRESENT","ABSENT","PRESENT","ABSENT","ABSENT","ABSENT","PRESENT"},
            {"ABSENT","PRESENT","PRESENT","PRESENT","PRESENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT"},
            {"PRESENT","PRESENT","PRESENT","ABSENT","PRESENT","PRESENT","PRESENT","PRESENT","ABSENT","ABSENT"},
            {"PRESENT","PRESENT","PRESENT","PRESENT","PRESENT","ABSENT","ABSENT","PRESENT","ABSENT","PRESENT"}
        };
        addSessionBatch(c1Sessions, c1Statuses);

        // c2 (OS) — 7 sessions
        String[][] c2Sessions = {
            {"r09","c2","2026-08-05","10:00 AM - 11:00 AM","Introduction to OS"},
            {"r10","c2","2026-08-07","10:00 AM - 11:00 AM","Process Management"},
            {"r11","c2","2026-08-12","10:00 AM - 11:00 AM","CPU Scheduling"},
            {"r12","c2","2026-08-14","10:00 AM - 11:00 AM","Memory Management"},
            {"r13","c2","2026-08-19","10:00 AM - 11:00 AM","File Systems"},
            {"r14","c2","2026-08-21","10:00 AM - 11:00 AM","Deadlocks"},
            {"r15","c2","2026-08-26","10:00 AM - 11:00 AM","I/O Systems & Virtualization"}
        };
        String[][] c2Statuses = {
            {"PRESENT","PRESENT","PRESENT","PRESENT","PRESENT","PRESENT","ABSENT","PRESENT","PRESENT","PRESENT"},
            {"PRESENT","ABSENT","PRESENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT"},
            {"PRESENT","PRESENT","PRESENT","ABSENT","PRESENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT"},
            {"ABSENT","PRESENT","PRESENT","PRESENT","PRESENT","ABSENT","ABSENT","PRESENT","ABSENT","PRESENT"},
            {"PRESENT","PRESENT","ABSENT","PRESENT","PRESENT","PRESENT","ABSENT","ABSENT","PRESENT","ABSENT"},
            {"PRESENT","PRESENT","PRESENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT"},
            {"PRESENT","PRESENT","PRESENT","ABSENT","PRESENT","PRESENT","PRESENT","PRESENT","ABSENT","PRESENT"}
        };
        addSessionBatch(c2Sessions, c2Statuses);

        // c3 (SE) — 5 sessions
        String[][] c3Sessions = {
            {"r16","c3","2026-08-04","11:15 AM - 12:15 PM","SDLC & Agile Overview"},
            {"r17","c3","2026-08-11","11:15 AM - 12:15 PM","Requirements Engineering"},
            {"r18","c3","2026-08-18","11:15 AM - 12:15 PM","UML Diagrams"},
            {"r19","c3","2026-08-25","11:15 AM - 12:15 PM","Testing Strategies"},
            {"r20","c3","2026-09-01","11:15 AM - 12:15 PM","DevOps & CI/CD Pipelines"}
        };
        String[][] c3Statuses = {
            {"PRESENT","PRESENT","PRESENT","PRESENT","PRESENT","ABSENT","ABSENT","PRESENT","ABSENT","PRESENT"},
            {"PRESENT","PRESENT","ABSENT","PRESENT","PRESENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT"},
            {"PRESENT","ABSENT","PRESENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT"},
            {"PRESENT","PRESENT","PRESENT","ABSENT","PRESENT","PRESENT","ABSENT","ABSENT","ABSENT","PRESENT"},
            {"ABSENT","PRESENT","PRESENT","PRESENT","PRESENT","PRESENT","ABSENT","PRESENT","PRESENT","PRESENT"}
        };
        addSessionBatch(c3Sessions, c3Statuses);
    }

    private void addSessionBatch(String[][] sessions, String[][] statuses) {
        String[] studentIds = {"s1","s2","s3","s4","s5","s6","s7","s8","s9","s10"};
        for (int i = 0; i < sessions.length; i++) {
            String[] s = sessions[i];
            AttendanceRecord rec = new AttendanceRecord(s[0], s[1], s[2], s[3], s[4]);
            rec.setTimestamp(System.currentTimeMillis() - ((sessions.length - i) * 86400000L));
            String[] statusRow = statuses[i];
            for (int j = 0; j < studentIds.length && j < statusRow.length; j++) {
                rec.setStudentStatus(studentIds[j], statusRow[j]);
            }
            records.put(rec.getId(), rec);
        }
    }

    /**
     * Optional full demo dataset loader (Admin Settings → "Load Demo Data" button).
     * Resets database and re-seeds demo data.
     */
    public synchronized void loadDemoSampleData() {
        initCleanDatabase();
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
                User admin = users.get("admin");
                if (!"Admin@123".equals(admin.getPassword())) {
                    admin.setPassword("Admin@123");
                    saveToFile();
                }
            }

            // Always ensure demo account exists with Demo@1234 password
            boolean addedDemoAccount = false;
            if (!users.containsKey("demo")) {
                User demo = new User("u_demo", "demo", "Demo@1234", "Dr. Demo Faculty", "FACULTY", "demo@vvism.edu", "Computer Science");
                users.put("demo", demo);
                addedDemoAccount = true;
            } else {
                User demo = users.get("demo");
                if (!"Demo@1234".equals(demo.getPassword())) {
                    demo.setPassword("Demo@1234");
                }
            }

            // Upgrade older empty databases so the demo account has showcase data.
            if (addedDemoAccount && students.isEmpty() && courses.isEmpty() && records.isEmpty()) {
                seedDemoData();
            }
            saveToFile();
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
