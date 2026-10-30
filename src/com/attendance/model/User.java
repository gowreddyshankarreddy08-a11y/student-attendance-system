package com.attendance.model;

/**
 * Represents a system user (Administrator or Faculty).
 */
public class User {
    private String id;
    private String username;
    private String password;
    private String name;
    private String role; // "ADMIN" or "FACULTY"
    private String email;
    private String department;

    public User() {}

    public User(String id, String username, String password, String name, String role, String email, String department) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
        this.email = email;
        this.department = department;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"username\":\"%s\",\"name\":\"%s\",\"role\":\"%s\",\"email\":\"%s\",\"department\":\"%s\"}",
            escape(id), escape(username), escape(name), escape(role), escape(email), escape(department)
        );
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
