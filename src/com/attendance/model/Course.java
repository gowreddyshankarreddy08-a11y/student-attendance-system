package com.attendance.model;

/**
 * Represents an academic course/subject.
 */
public class Course {
    private String id;
    private String code;
    private String name;
    private String department;
    private String faculty;

    public Course() {}

    public Course(String id, String code, String name, String department, String faculty) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.department = department;
        this.faculty = faculty;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"code\":\"%s\",\"name\":\"%s\",\"department\":\"%s\",\"faculty\":\"%s\"}",
            escape(id), escape(code), escape(name), escape(department), escape(faculty)
        );
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
