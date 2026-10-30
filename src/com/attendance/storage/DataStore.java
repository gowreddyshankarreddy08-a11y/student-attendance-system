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
 * Uses zero third-party dependencies for maximum portability.
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
                seedSampleData();
                saveToFile();
            }
        } catch (Exception e) {
            System.err.println("[DataStore] Failed to load data from file: " + e.getMessage() + ". Seeding defaults...");
            seedSampleData();
        }
    }

    public synchronized void seedSampleData() {
        students.clear();
        courses.clear();
        records.clear();
        users.clear();
        settings.clear();

        // 0. Default Settings
        settings.put("institutionName", "Apex Institute of Science & Technology");
        settings.put("minAttendanceThreshold", "75.0");
        settings.put("academicTerm", "Academic Year 2026-27 (Fall Sem)");

        // 1. Seed Users (Admin & Faculty)
        User admin = new User("u1", "admin", "admin123", "Dean of Academic Affairs", "ADMIN", "dean@college.edu", "Administration");
        User f1 = new User("u2", "faculty", "faculty123", "Dr. Alan Turing", "FACULTY", "turing@college.edu", "Computer Science");
        User f2 = new User("u3", "linus", "faculty123", "Prof. Linus Torvalds", "FACULTY", "linus@college.edu", "Computer Science");
        User f3 = new User("u4", "radia", "faculty123", "Dr. Radia Perlman", "FACULTY", "radia@college.edu", "Computer Science");
        User f4 = new User("u5", "margaret", "faculty123", "Prof. Margaret Hamilton", "FACULTY", "margaret@college.edu", "Computer Science");

        users.put(admin.getUsername(), admin);
        users.put(f1.getUsername(), f1);
        users.put(f2.getUsername(), f2);
        users.put(f3.getUsername(), f3);
        users.put(f4.getUsername(), f4);

        // 2. Seed Courses
        Course c1 = new Course("c1", "CS301", "Database Management Systems", "Computer Science", "Dr. Alan Turing");
        Course c2 = new Course("c2", "CS302", "Operating Systems", "Computer Science", "Prof. Linus Torvalds");
        Course c3 = new Course("c3", "CS303", "Computer Networks", "Computer Science", "Dr. Radia Perlman");
        Course c4 = new Course("c4", "CS304", "Software Engineering", "Computer Science", "Prof. Margaret Hamilton");

        courses.put(c1.getId(), c1);
        courses.put(c2.getId(), c2);
        courses.put(c3.getId(), c3);
        courses.put(c4.getId(), c4);

        // 3. Seed Students
        String[][] studentData = {
            {"s1", "CS-101", "Aarav Sharma", "aarav.sharma@college.edu", "Computer Science", "5"},
            {"s2", "CS-102", "Aditi Patel", "aditi.patel@college.edu", "Computer Science", "5"},
            {"s3", "CS-103", "Bhavani Shankar", "bhavani.shankar@college.edu", "Computer Science", "5"},
            {"s4", "CS-104", "Devika Nambiar", "devika.n@college.edu", "Computer Science", "5"},
            {"s5", "CS-105", "Ishaan Verma", "ishaan.verma@college.edu", "Computer Science", "5"},
            {"s6", "CS-106", "Kavya Subramanian", "kavya.s@college.edu", "Computer Science", "5"},
            {"s7", "CS-107", "Manish Reddy", "manish.reddy@college.edu", "Computer Science", "5"},
            {"s8", "CS-108", "Neha Gupta", "neha.gupta@college.edu", "Computer Science", "5"},
            {"s9", "CS-109", "Pranav Rao", "pranav.rao@college.edu", "Computer Science", "5"},
            {"s10", "CS-110", "Pooja Hegde", "pooja.hegde@college.edu", "Computer Science", "5"},
            {"s11", "CS-111", "Rahul Nair", "rahul.nair@college.edu", "Computer Science", "5"},
            {"s12", "CS-112", "Riya Sen", "riya.sen@college.edu", "Computer Science", "5"},
            {"s13", "CS-113", "Siddharth Joshi", "siddharth.j@college.edu", "Computer Science", "5"},
            {"s14", "CS-114", "Tanvi Kulkarni", "tanvi.k@college.edu", "Computer Science", "5"},
            {"s15", "CS-115", "Vikram Rathore", "vikram.r@college.edu", "Computer Science", "5"}
        };

        for (String[] row : studentData) {
            Student s = new Student(row[0], row[1], row[2], row[3], row[4], Integer.parseInt(row[5]));
            students.put(s.getId(), s);
        }

        // 4. Seed Past 6 Attendance Sessions for each course
        LocalDate today = LocalDate.now();
        int recordCounter = 1;

        for (Course c : courses.values()) {
            for (int daysAgo = 10; daysAgo >= 0; daysAgo -= 2) {
                LocalDate date = today.minusDays(daysAgo);
                String recId = "rec_" + (recordCounter++);
                AttendanceRecord rec = new AttendanceRecord(
                    recId,
                    c.getId(),
                    date.toString(),
                    "09:30 AM - 10:30 AM",
                    "Lecture: " + c.getName() + " - Unit " + ((daysAgo % 3) + 1)
                );

                for (Student s : students.values()) {
                    String status = "PRESENT";
                    int rollHash = Math.abs((s.getRollNo() + c.getId() + date).hashCode());

                    if (s.getRollNo().equals("CS-105") || s.getRollNo().equals("CS-111")) {
                        status = (rollHash % 3 == 0) ? "PRESENT" : "ABSENT";
                    } else if (s.getRollNo().equals("CS-108") || s.getRollNo().equals("CS-115")) {
                        status = (rollHash % 10 < 7) ? "PRESENT" : "ABSENT";
                    } else {
                        if (rollHash % 12 == 0) status = "ABSENT";
                        else if (rollHash % 15 == 0) status = "LATE";
                        else status = "PRESENT";
                    }
                    rec.setStudentStatus(s.getId(), status);
                }
                records.put(rec.getId(), rec);
            }
        }
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
                seedSampleData();
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
                users.put(u.getUsername(), u);
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

            // If users or courses are missing, reseed
            if (users.isEmpty() || courses.isEmpty() || students.isEmpty()) {
                seedSampleData();
                saveToFile();
            }
        } catch (Exception e) {
            System.err.println("[DataStore] Parse error: " + e.getMessage() + ". Reseeding defaults.");
            seedSampleData();
            saveToFile();
        }
    }

    public User authenticate(String username, String password) {
        if (username == null || password == null) return null;
        User u = users.get(username.trim());
        if (u != null && u.getPassword().equals(password.trim())) {
            return u;
        }
        return null;
    }

    public List<User> getUsers() {
        return new ArrayList<>(users.values());
    }

    public synchronized User addUser(User u) {
        if (u.getId() == null || u.getId().isEmpty()) {
            u.setId("u_" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (u.getPassword() == null || u.getPassword().isEmpty()) {
            u.setPassword("faculty123");
        }
        users.put(u.getUsername(), u);
        saveToFile();
        return u;
    }

    public synchronized boolean deleteUser(String username) {
        if ("admin".equalsIgnoreCase(username)) {
            return false; // Prevent deleting root admin
        }
        User removed = users.remove(username);
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
