package com.attendance.model;

/**
 * Represents a student enrolled in the institution.
 */
public class Student {
    private String id;
    private String rollNo;
    private String name;
    private String email;
    private String department;
    private int semester;

    public Student() {}

    public Student(String id, String rollNo, String name, String email, String department, int semester) {
        this.id = id;
        this.rollNo = rollNo;
        this.name = name;
        this.email = email;
        this.department = department;
        this.semester = semester;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"rollNo\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"department\":\"%s\",\"semester\":%d}",
            escape(id), escape(rollNo), escape(name), escape(email), escape(department), semester
        );
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
