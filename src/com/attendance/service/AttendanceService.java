package com.attendance.service;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.Course;
import com.attendance.model.Student;
import com.attendance.storage.DataStore;

import java.util.*;

/**
 * Business logic service for Attendance analytics, stats calculation,
 * defaulter detection, and CSV export.
 */
public class AttendanceService {
    private final DataStore dataStore;

    public AttendanceService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Student> getAllStudents() {
        return dataStore.getStudents();
    }

    public Student addStudent(Student s) {
        return dataStore.addStudent(s);
    }

    public boolean deleteStudent(String id) {
        return dataStore.deleteStudent(id);
    }

    public List<Course> getAllCourses() {
        return dataStore.getCourses();
    }

    public Course addCourse(Course c) {
        return dataStore.addCourse(c);
    }

    public List<AttendanceRecord> getAllRecords() {
        return dataStore.getRecords();
    }

    public AttendanceRecord saveRecord(AttendanceRecord record) {
        return dataStore.saveRecord(record);
    }

    /**
     * Calculates attendance stats for a specific student across all sessions or by course.
     */
    public Map<String, Object> getStudentAttendanceSummary(String studentId, String courseIdFilter) {
        List<AttendanceRecord> records = dataStore.getRecords();
        int total = 0;
        int present = 0;
        int late = 0;
        int absent = 0;
        int excused = 0;

        for (AttendanceRecord rec : records) {
            if (courseIdFilter != null && !courseIdFilter.isEmpty() && !rec.getCourseId().equals(courseIdFilter)) {
                continue;
            }
            if (rec.getRecords().containsKey(studentId)) {
                total++;
                String status = rec.getRecords().get(studentId);
                switch (status) {
                    case "PRESENT":
                        present++;
                        break;
                    case "LATE":
                        late++;
                        present++; // Late is counted with present or partial
                        break;
                    case "EXCUSED":
                        excused++;
                        break;
                    case "ABSENT":
                    default:
                        absent++;
                        break;
                }
            }
        }

        double percentage = (total == 0) ? 100.0 : ((double) present / total) * 100.0;
        Map<String, Object> map = new HashMap<>();
        map.put("studentId", studentId);
        map.put("totalClasses", total);
        map.put("present", present);
        map.put("late", late);
        map.put("absent", absent);
        map.put("excused", excused);
        map.put("percentage", Math.round(percentage * 10.0) / 10.0);
        return map;
    }

    /**
     * Finds students whose overall attendance percentage is below the threshold (e.g. 75%).
     */
    public List<Map<String, Object>> getDefaulters(double threshold, String courseId) {
        List<Student> students = dataStore.getStudents();
        List<Map<String, Object>> defaulters = new ArrayList<>();

        for (Student s : students) {
            Map<String, Object> summary = getStudentAttendanceSummary(s.getId(), courseId);
            double pct = (double) summary.get("percentage");
            int total = (int) summary.get("totalClasses");

            if (total > 0 && pct < threshold) {
                Map<String, Object> item = new HashMap<>();
                item.put("student", s);
                item.put("totalClasses", total);
                item.put("attended", summary.get("present"));
                item.put("percentage", pct);
                item.put("severity", pct < 65.0 ? "CRITICAL" : "WARNING");
                // Classes needed to reach 75%
                // (attended + x) / (total + x) >= 0.75  =>  x >= (0.75*total - attended) / 0.25
                int attended = (int) summary.get("present");
                int needed = (int) Math.ceil(Math.max(0, (0.75 * total - attended) / 0.25));
                item.put("classesNeededToRecover", needed);
                defaulters.add(item);
            }
        }

        defaulters.sort((a, b) -> Double.compare((double) a.get("percentage"), (double) b.get("percentage")));
        return defaulters;
    }

    /**
     * Calculates overall system KPI stats for the faculty dashboard.
     */
    public Map<String, Object> getOverallStats() {
        List<Student> students = dataStore.getStudents();
        List<Course> courses = dataStore.getCourses();
        List<AttendanceRecord> records = dataStore.getRecords();

        long totalMarkings = 0;
        long totalPresent = 0;

        for (AttendanceRecord rec : records) {
            for (String status : rec.getRecords().values()) {
                totalMarkings++;
                if ("PRESENT".equals(status) || "LATE".equals(status)) {
                    totalPresent++;
                }
            }
        }

        double overallRate = (totalMarkings == 0) ? 0.0 : ((double) totalPresent / totalMarkings) * 100.0;
        List<Map<String, Object>> defaulters = getDefaulters(75.0, null);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", students.size());
        stats.put("totalCourses", courses.size());
        stats.put("totalSessions", records.size());
        stats.put("overallAttendanceRate", Math.round(overallRate * 10.0) / 10.0);
        stats.put("defaulterCount", defaulters.size());

        return stats;
    }

    /**
     * Generates a comprehensive CSV export report for faculty.
     */
    public String generateCsvReport(String courseId) {
        StringBuilder csv = new StringBuilder();
        Course c = (courseId != null && !courseId.isEmpty()) ? dataStore.getCourse(courseId) : null;
        String courseTitle = (c != null) ? c.getCode() + " - " + c.getName() : "All Courses";

        csv.append("Student Attendance Report\n");
        csv.append("Course:,\"").append(courseTitle).append("\"\n");
        csv.append("Generated On:,\"").append(new Date()).append("\"\n\n");

        csv.append("Roll No,Student Name,Email,Department,Semester,Total Classes,Attended,Absent,Late,Percentage (%),Eligibility Status\n");

        for (Student s : dataStore.getStudents()) {
            Map<String, Object> summary = getStudentAttendanceSummary(s.getId(), courseId);
            double pct = (double) summary.get("percentage");
            String status = pct >= 75.0 ? "ELIGIBLE" : (pct >= 65.0 ? "WARNING (<75%)" : "DEBARRED (<65%)");

            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,%d,%d,%d,%.1f%%,\"%s\"\n",
                s.getRollNo(),
                s.getName(),
                s.getEmail(),
                s.getDepartment(),
                s.getSemester(),
                summary.get("totalClasses"),
                summary.get("present"),
                summary.get("absent"),
                summary.get("late"),
                pct,
                status
            ));
        }

        return csv.toString();
    }
}
