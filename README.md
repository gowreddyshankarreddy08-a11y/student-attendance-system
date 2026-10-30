# 🎓 AttendSmart - Student Attendance Management System

> **A high-productivity attendance management system designed for academic institutions to eliminate manual attendance bottlenecks, simplify record keeping, and reduce faculty workload.**

Built with **Java (Backend REST API & Server)**, **HTML5**, **CSS3**, and **Modern JavaScript (Responsive Faculty UI)**.

---

## 🌟 Key Features (Engineered for Faculty Efficiency)

1. **⚡ Rapid Attendance Marking (15-Minute Daily Faculty Time-Saver)**
   - **1-Click "Mark All Present"**: Mark an entire lecture of 60+ students present in a single tap, then just tap the 2-3 absentees!
   - Instant search filter by student Roll Number or Name.
   - Status toggle chips: `Present` (Green), `Absent` (Red), `Late` (Amber), and `Leave/Excused` (Sky Blue).
   - Real-time live counter showing present turnout, absent counts, and class percentage.

2. **⚠️ Automated Defaulter Detection (<75% Eligibility Cutoff)**
   - Flags students whose overall attendance falls below university examination eligibility requirements.
   - Automatic recovery calculation: Shows exactly how many consecutive classes a student must attend to reach 75%.
   - **1-Click "Copy Notice Text"**: Generates a formatted message ready to paste into faculty emails or notice boards.

3. **📊 Reports & Official Exports**
   - Filter reports by course or overall semester.
   - 1-Click **"Export CSV Report"** compatible with Microsoft Excel and Google Sheets.
   - Print-optimized layout (**"Print / Save PDF"**) for department submissions.

4. **👥 Student & Course Directory**
   - Track students by Roll Number, Department, Semester, and individual attendance history.
   - Student modal view with per-subject breakdown and exam eligibility status.
   - Add/delete students and courses directly from the portal.

5. **🔄 Smart Dual-Mode Architecture**
   - **Local Java Server Mode**: Connects to the lightweight Java backend (`com.sun.net.httpserver`) and persists all records to local JSON storage.
   - **Online Public Web Mode (GitHub Pages)**: Runs directly in any web browser with local storage persistence and pre-populated sample records so anyone with your public link can test it immediately without needing a server!

---

## 🚀 Quick Start (Running Locally with Java)

### Prerequisites
- **Java 17 or higher** (Tested on Java 21, 25, and Java 27)
- Any modern web browser (Chrome, Edge, Firefox, Safari)

### Option 1: One-Click Launcher (Windows)
Double-click `build_and_run.bat` or open PowerShell in the project directory:
```powershell
.\run.ps1
```

### Option 2: Manual Terminal Commands
```bash
# 1. Compile all Java sources into bin/
javac -encoding UTF-8 -d bin src/com/attendance/model/*.java src/com/attendance/storage/*.java src/com/attendance/service/*.java src/com/attendance/server/*.java src/com/attendance/*.java

# 2. Run the Attendance Server
java -cp bin com.attendance.Main
```

Open your browser and visit:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 🌐 How to Push to GitHub & Create a Public Website Link

Follow these simple steps to push this project to your GitHub account and publish the public link:

### Step 1: Create a New Repository on GitHub
1. Log in to [GitHub](https://github.com/).
2. Click the **`+`** icon in the top right corner and select **New repository**.
3. Name your repository (e.g., `student-attendance-system`).
4. Set visibility to **Public** (important for free GitHub Pages).
5. **Do not** check "Initialize this repository with a README" (since we already have one).
6. Click **Create repository**.

### Step 2: Push Your Local Code to GitHub
Open your terminal / PowerShell in this project folder and run:

```bash
# Initialize git (if not already initialized)
git init
git branch -M main

# Add all files and make the first commit
git add .
git commit -m "Initial commit: Student Attendance Management System"

# Connect to your GitHub repository (replace with your GitHub username)
git remote add origin https://github.com/<YOUR-GITHUB-USERNAME>/student-attendance-system.git

# Push code to GitHub
git push -u origin main
```

### Step 3: Turn on GitHub Pages (Instant Public Link)
1. Go to your repository on GitHub.
2. Click **Settings** (tab at the top right).
3. In the left navigation menu, click **Pages** (under the "Code and automation" section).
4. Under **Build and deployment**:
   - **Source**: Select `Deploy from a branch`
   - **Branch**: Select `main` and set the folder to `/ (root)` or `/docs`
   - Click **Save**.
5. Wait 30–60 seconds for GitHub to finish building.
6. Your public website link will be displayed at the top:
   ```
   https://<YOUR-GITHUB-USERNAME>.github.io/student-attendance-system/
   ```

Anyone can open this link in their phone, tablet, or laptop and interact with your Student Attendance Management System immediately!

---

## 📁 Project Structure

```text
student-attendance-system/
│
├── .github/
│   └── workflows/
│       └── deploy.yml          # GitHub Actions automated Pages deployment
│
├── src/
│   └── com/attendance/
│       ├── Main.java           # Entry point: initializes data store, service & HTTP server
│       ├── model/
│       │   ├── Student.java    # Student entity (Roll No, Name, Email, Semester)
│       │   ├── Course.java     # Course entity (Code, Name, Faculty)
│       │   └── AttendanceRecord.java # Session record (Date, Slot, Student Status Map)
│       ├── storage/
│       │   └── DataStore.java  # Thread-safe persistent JSON store with sample seeder
│       ├── service/
│       │   └── AttendanceService.java # Analytics, defaulter calculation, CSV generator
│       └── server/
│           └── AttendanceHttpServer.java # Pure Java REST API & static file web server
│
├── web/ & docs/ & root
│   ├── index.html              # Responsive faculty portal layout
│   ├── css/
│   │   └── style.css           # Modern, accessible styling with print stylesheets
│   └── js/
│       └── app.js              # Interactive UI controller with Dual-Mode storage
│
├── data/
│   └── attendance_database.json # Auto-generated persistent JSON database
│
├── build_and_run.bat           # 1-Click Windows batch script to compile and launch
├── run.ps1                     # PowerShell runner script
├── .gitignore                  # Git ignore rules
└── README.md                   # Complete documentation and setup guide
```

---

## 🔌 REST API Endpoints (Java Backend)

When running the Java server, the following RESTful endpoints are available:

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/stats` | `GET` | Overall dashboard metrics (total students, classes, attendance %, defaulters) |
| `/api/students` | `GET` | List all students with attendance percentages |
| `/api/students` | `POST` | Add a new student record |
| `/api/students?id=...` | `DELETE`| Remove a student record |
| `/api/courses` | `GET` | List all registered courses |
| `/api/courses` | `POST` | Add a new course |
| `/api/attendance` | `GET` | Get attendance records (filter by `courseId` & `date`) |
| `/api/attendance` | `POST` | Save/submit an attendance session |
| `/api/defaulters` | `GET` | List students below attendance cutoff (default `75%`) |
| `/api/export/csv` | `GET` | Downloadable CSV report |

---

## 📄 License
This project is open source and available under the [MIT License](LICENSE).
