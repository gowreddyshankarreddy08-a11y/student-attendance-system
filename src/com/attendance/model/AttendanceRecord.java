package com.attendance.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single lecture's attendance record session.
 */
public class AttendanceRecord {
    private String id;
    private String courseId;
    private String date; // Format: YYYY-MM-DD
    private String timeSlot;
    private String notes;
    private long timestamp;
    private Map<String, String> records; // studentId -> status (PRESENT, ABSENT, LATE, EXCUSED)

    public AttendanceRecord() {
        this.records = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    public AttendanceRecord(String id, String courseId, String date, String timeSlot, String notes) {
        this.id = id;
        this.courseId = courseId;
        this.date = date;
        this.timeSlot = timeSlot;
        this.notes = notes;
        this.timestamp = System.currentTimeMillis();
        this.records = new HashMap<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<String, String> getRecords() { return records; }
    public void setRecords(Map<String, String> records) { this.records = records; }

    public void setStudentStatus(String studentId, String status) {
        this.records.put(studentId, status);
    }

    public String getStudentStatus(String studentId) {
        return this.records.getOrDefault(studentId, "ABSENT");
    }

    public String toJson() {
        StringBuilder recJson = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : records.entrySet()) {
            if (!first) recJson.append(",");
            recJson.append(String.format("\"%s\":\"%s\"", escape(entry.getKey()), escape(entry.getValue())));
            first = false;
        }
        recJson.append("}");

        return String.format(
            "{\"id\":\"%s\",\"courseId\":\"%s\",\"date\":\"%s\",\"timeSlot\":\"%s\",\"notes\":\"%s\",\"timestamp\":%d,\"records\":%s}",
            escape(id), escape(courseId), escape(date), escape(timeSlot), escape(notes), timestamp, recJson.toString()
        );
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
