package com.attendance;

import com.attendance.server.AttendanceHttpServer;
import com.attendance.service.AttendanceService;
import com.attendance.storage.DataStore;

import java.io.File;

/**
 * Main application entry point for the Student Attendance Management System.
 */
public class Main {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        } else {
            String envPort = System.getenv("PORT");
            if (envPort != null && !envPort.isEmpty()) {
                try {
                    port = Integer.parseInt(envPort);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Determine working directories
        String projectRoot = System.getProperty("user.dir");
        String dataDir = new File(projectRoot, "data").getAbsolutePath();
        String webDir = new File(projectRoot, "web").getAbsolutePath();

        // If running from bin or out directory, fallback to check web directory
        if (!new File(webDir).exists()) {
            File parentWeb = new File(new File(projectRoot).getParentFile(), "web");
            if (parentWeb.exists()) {
                webDir = parentWeb.getAbsolutePath();
            }
        }

        try {
            DataStore dataStore = new DataStore(dataDir);
            AttendanceService service = new AttendanceService(dataStore);
            AttendanceHttpServer server = new AttendanceHttpServer(port, service, webDir);
            server.start();

            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[Attendance System] Shutting down server gracefully...");
                server.stop();
                System.out.println("[Attendance System] Server stopped.");
            }));

        } catch (Exception e) {
            System.err.println("Failed to start Attendance Management System: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
