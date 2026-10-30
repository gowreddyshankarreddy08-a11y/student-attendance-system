/**
 * AttendSmart - Faculty Student Attendance Management System
 * Full Frontend Controller with Smart Dual-Mode (Java API + GitHub Pages LocalStorage)
 */

class AttendanceApp {
    constructor() {
        this.apiBase = '';
        this.isBackendConnected = false;
        this.currentTab = 'dashboard';
        
        // In-memory data store
        this.students = [];
        this.courses = [];
        this.records = [];
        this.currentAttendanceState = {}; // studentId -> status

        this.init();
    }

    async init() {
        this.initEventListeners();
        this.setupCurrentDate();
        await this.detectBackend();
        await this.loadAllData();
        this.renderAll();
    }

    /* ------------------------------------------------------------------------
       Backend Detection & Storage Abstraction (Dual-Mode)
       ------------------------------------------------------------------------ */
    async detectBackend() {
        const badge = document.getElementById('backendStatusBadge');
        const text = document.getElementById('backendStatusText');

        // Check if we can reach the Java API
        try {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 1500);
            const res = await fetch('/api/stats', { signal: controller.signal });
            clearTimeout(timeoutId);

            if (res.ok) {
                this.isBackendConnected = true;
                this.apiBase = '';
                badge.className = 'mode-badge connected';
                text.textContent = 'Connected: Java Backend';
                return;
            }
        } catch (e) {
            // Localhost fallback check if opening file directly
            try {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 1200);
                const res = await fetch('http://localhost:8080/api/stats', { signal: controller.signal });
                clearTimeout(timeoutId);
                if (res.ok) {
                    this.isBackendConnected = true;
                    this.apiBase = 'http://localhost:8080';
                    badge.className = 'mode-badge connected';
                    text.textContent = 'Connected: Java (Port 8080)';
                    return;
                }
            } catch (err) {}
        }

        // Running standalone (e.g. GitHub Pages or offline)
        this.isBackendConnected = false;
        badge.className = 'mode-badge demo';
        text.textContent = 'Live Web App (Local Storage)';
    }

    async loadAllData() {
        if (this.isBackendConnected) {
            try {
                const [studRes, courseRes, recRes] = await Promise.all([
                    fetch(`${this.apiBase}/api/students`).then(r => r.json()),
                    fetch(`${this.apiBase}/api/courses`).then(r => r.json()),
                    fetch(`${this.apiBase}/api/attendance`).then(r => r.json())
                ]);
                this.students = studRes;
                this.courses = courseRes;
                this.records = recRes;
                return;
            } catch (e) {
                console.warn('Backend fetch failed, falling back to local storage', e);
            }
        }

        // Local Storage Mode
        this.loadLocalData();
    }

    loadLocalData() {
        const storedStudents = localStorage.getItem('attendsmart_students');
        const storedCourses = localStorage.getItem('attendsmart_courses');
        const storedRecords = localStorage.getItem('attendsmart_records');

        if (storedStudents && storedCourses && storedRecords) {
            this.students = JSON.parse(storedStudents);
            this.courses = JSON.parse(storedCourses);
            this.records = JSON.parse(storedRecords);
        } else {
            // Pre-seed sample realistic data for instant demonstration
            this.seedLocalData();
        }
    }

    saveLocalData() {
        localStorage.setItem('attendsmart_students', JSON.stringify(this.students));
        localStorage.setItem('attendsmart_courses', JSON.stringify(this.courses));
        localStorage.setItem('attendsmart_records', JSON.stringify(this.records));
    }

    seedLocalData() {
        this.courses = [
            { id: 'c1', code: 'CS301', name: 'Database Management Systems', department: 'Computer Science', faculty: 'Dr. Alan Turing' },
            { id: 'c2', code: 'CS302', name: 'Operating Systems', department: 'Computer Science', faculty: 'Prof. Linus Torvalds' },
            { id: 'c3', code: 'CS303', name: 'Computer Networks', department: 'Computer Science', faculty: 'Dr. Radia Perlman' },
            { id: 'c4', code: 'CS304', name: 'Software Engineering', department: 'Computer Science', faculty: 'Prof. Margaret Hamilton' }
        ];

        this.students = [
            { id: 's1', rollNo: 'CS-101', name: 'Aarav Sharma', email: 'aarav.sharma@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's2', rollNo: 'CS-102', name: 'Aditi Patel', email: 'aditi.patel@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's3', rollNo: 'CS-103', name: 'Bhavani Shankar', email: 'bhavani.shankar@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's4', rollNo: 'CS-104', name: 'Devika Nambiar', email: 'devika.n@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's5', rollNo: 'CS-105', name: 'Ishaan Verma', email: 'ishaan.verma@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's6', rollNo: 'CS-106', name: 'Kavya Subramanian', email: 'kavya.s@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's7', rollNo: 'CS-107', name: 'Manish Reddy', email: 'manish.reddy@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's8', rollNo: 'CS-108', name: 'Neha Gupta', email: 'neha.gupta@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's9', rollNo: 'CS-109', name: 'Pranav Rao', email: 'pranav.rao@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's10', rollNo: 'CS-110', name: 'Pooja Hegde', email: 'pooja.hegde@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's11', rollNo: 'CS-111', name: 'Rahul Nair', email: 'rahul.nair@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's12', rollNo: 'CS-112', name: 'Riya Sen', email: 'riya.sen@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's13', rollNo: 'CS-113', name: 'Siddharth Joshi', email: 'siddharth.j@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's14', rollNo: 'CS-114', name: 'Tanvi Kulkarni', email: 'tanvi.k@college.edu', department: 'Computer Science', semester: 5 },
            { id: 's15', rollNo: 'CS-115', name: 'Vikram Rathore', email: 'vikram.r@college.edu', department: 'Computer Science', semester: 5 }
        ];

        this.records = [];
        const today = new Date();
        let counter = 1;

        // Generate realistic past sessions for each course
        this.courses.forEach(course => {
            for (let daysAgo = 10; daysAgo >= 0; daysAgo -= 2) {
                const sessionDate = new Date(today);
                sessionDate.setDate(today.getDate() - daysAgo);
                const dateStr = sessionDate.toISOString().split('T')[0];

                const recordMap = {};
                this.students.forEach(student => {
                    const hash = Math.abs((student.rollNo + course.id + dateStr).split('').reduce((a, b) => ((a << 5) - a) + b.charCodeAt(0), 0));
                    let status = 'PRESENT';
                    if (student.rollNo === 'CS-105' || student.rollNo === 'CS-111') {
                        status = (hash % 3 === 0) ? 'PRESENT' : 'ABSENT'; // ~33% defaulter
                    } else if (student.rollNo === 'CS-108' || student.rollNo === 'CS-115') {
                        status = (hash % 10 < 7) ? 'PRESENT' : 'ABSENT'; // ~70% borderline
                    } else {
                        if (hash % 12 === 0) status = 'ABSENT';
                        else if (hash % 15 === 0) status = 'LATE';
                        else status = 'PRESENT';
                    }
                    recordMap[student.id] = status;
                });

                this.records.push({
                    id: 'rec_' + (counter++),
                    courseId: course.id,
                    date: dateStr,
                    timeSlot: '09:00 AM - 10:00 AM',
                    notes: `Lecture: ${course.name} - Unit ${(daysAgo % 3) + 1}`,
                    timestamp: sessionDate.getTime(),
                    records: recordMap
                });
            }
        });

        this.saveLocalData();
    }

    /* ------------------------------------------------------------------------
       UI Navigation & Events
       ------------------------------------------------------------------------ */
    initEventListeners() {
        // Tab switching
        document.querySelectorAll('.sidebar-nav .nav-item').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const tab = btn.getAttribute('data-tab');
                this.showTab(tab);
            });
        });

        // Mobile drawer toggle
        const toggle = document.getElementById('mobileToggle');
        const sidebar = document.getElementById('sidebar');
        if (toggle && sidebar) {
            toggle.addEventListener('click', () => {
                sidebar.classList.toggle('open');
            });
        }
    }

    setupCurrentDate() {
        const now = new Date();
        const options = { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' };
        const formatted = now.toLocaleDateString('en-US', options);
        document.getElementById('currentDateDisplay').textContent = formatted;

        // Set attendance date picker to today
        const dateInput = document.getElementById('attendanceDateInput');
        if (dateInput) {
            dateInput.value = now.toISOString().split('T')[0];
        }
    }

    showTab(tabId) {
        this.currentTab = tabId;
        document.querySelectorAll('.sidebar-nav .nav-item').forEach(b => {
            b.classList.toggle('active', b.getAttribute('data-tab') === tabId);
        });
        document.querySelectorAll('.tab-pane').forEach(p => {
            p.classList.toggle('active', p.id === `tab-${tabId}`);
        });

        // Update Page Title
        const titleMap = {
            'dashboard': 'Dashboard Overview',
            'take-attendance': 'Take Student Attendance',
            'students': 'Student Directory',
            'defaulters': 'Defaulter Alert (<75%)',
            'reports': 'Attendance Reports & Analytics',
            'courses': 'Course Roster'
        };
        document.getElementById('pageTitle').textContent = titleMap[tabId] || 'Attendance System';

        // Close mobile drawer if open
        const sidebar = document.getElementById('sidebar');
        if (sidebar) sidebar.classList.remove('open');

        // Refresh relevant views
        if (tabId === 'take-attendance') {
            this.initAttendanceMarkingView();
        } else if (tabId === 'students') {
            this.renderStudentDirectory();
        } else if (tabId === 'defaulters') {
            this.renderDefaultersTable();
        } else if (tabId === 'reports') {
            this.renderReportTable();
        } else if (tabId === 'courses') {
            this.renderCoursesTable();
        } else if (tabId === 'dashboard') {
            this.renderDashboard();
        }
    }

    showTakeAttendanceTab() {
        this.showTab('take-attendance');
    }

    /* ------------------------------------------------------------------------
       Calculations & Statistics Helpers
       ------------------------------------------------------------------------ */
    getStudentStats(studentId, courseIdFilter = null) {
        let total = 0;
        let present = 0;
        let absent = 0;
        let late = 0;
        let excused = 0;

        this.records.forEach(rec => {
            if (courseIdFilter && rec.courseId !== courseIdFilter) return;
            if (rec.records && rec.records[studentId]) {
                total++;
                const status = rec.records[studentId];
                if (status === 'PRESENT') present++;
                else if (status === 'LATE') { late++; present++; }
                else if (status === 'EXCUSED') excused++;
                else absent++;
            }
        });

        const pct = total === 0 ? 100.0 : Math.round((present / total) * 1000) / 10;
        return { total, present, absent, late, excused, percentage: pct };
    }

    getCourseHealth() {
        return this.courses.map(course => {
            const courseRecords = this.records.filter(r => r.courseId === course.id);
            let totalMarks = 0;
            let totalPresent = 0;

            courseRecords.forEach(r => {
                Object.values(r.records || {}).forEach(status => {
                    totalMarks++;
                    if (status === 'PRESENT' || status === 'LATE') totalPresent++;
                });
            });

            const pct = totalMarks === 0 ? 0 : Math.round((totalPresent / totalMarks) * 1000) / 10;
            return {
                course,
                sessions: courseRecords.length,
                attendancePct: pct
            };
        });
    }

    getDefaulters(threshold = 75.0, courseId = null) {
        const defaulters = [];
        this.students.forEach(s => {
            const stats = this.getStudentStats(s.id, courseId);
            if (stats.total > 0 && stats.percentage < threshold) {
                // Classes needed to reach 75%: (present + x)/(total + x) >= 0.75 => x >= (0.75*total - present)/0.25
                const needed = Math.max(0, Math.ceil((0.75 * stats.total - stats.present) / 0.25));
                defaulters.push({
                    student: s,
                    stats,
                    neededClasses: needed,
                    severity: stats.percentage < 65.0 ? 'CRITICAL' : 'WARNING'
                });
            }
        });
        return defaulters.sort((a, b) => a.stats.percentage - b.stats.percentage);
    }

    /* ------------------------------------------------------------------------
       Rendering: Dashboard
       ------------------------------------------------------------------------ */
    renderAll() {
        this.populateCourseSelects();
        this.renderDashboard();
    }

    renderDashboard() {
        // Compute Overall Stats
        const totalStudents = this.students.length;
        const totalSessions = this.records.length;
        const defaulters = this.getDefaulters(75.0);

        let totalMarks = 0;
        let totalPresent = 0;
        this.records.forEach(r => {
            Object.values(r.records || {}).forEach(status => {
                totalMarks++;
                if (status === 'PRESENT' || status === 'LATE') totalPresent++;
            });
        });
        const overallRate = totalMarks === 0 ? 0 : Math.round((totalPresent / totalMarks) * 1000) / 10;

        document.getElementById('statTotalStudents').textContent = totalStudents;
        document.getElementById('statTotalSessions').textContent = totalSessions;
        document.getElementById('statOverallRate').textContent = `${overallRate}%`;
        document.getElementById('statDefaulters').textContent = defaulters.length;
        document.getElementById('navDefaulterCount').textContent = defaulters.length;

        // Render Course Health Bars
        const healthList = document.getElementById('courseHealthList');
        const courseHealth = this.getCourseHealth();
        healthList.innerHTML = courseHealth.map(item => {
            let colorClass = 'good';
            if (item.attendancePct < 65) colorClass = 'critical';
            else if (item.attendancePct < 75) colorClass = 'warning';

            return `
                <div class="course-health-item">
                    <div class="health-header">
                        <span><strong>${item.course.code}:</strong> ${item.course.name}</span>
                        <span><strong>${item.attendancePct}%</strong> (${item.sessions} sessions)</span>
                    </div>
                    <div class="progress-track">
                        <div class="progress-bar ${colorClass}" style="width: ${item.attendancePct}%;"></div>
                    </div>
                </div>
            `;
        }).join('');

        // Render Recent Sessions
        const recentBody = document.getElementById('recentSessionsTableBody');
        const sortedRecords = [...this.records].sort((a, b) => b.date.localeCompare(a.date)).slice(0, 5);

        if (sortedRecords.length === 0) {
            recentBody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 2rem;">No attendance sessions recorded yet. Click "Take Attendance" to begin!</td></tr>`;
            return;
        }

        recentBody.innerHTML = sortedRecords.map(rec => {
            const course = this.courses.find(c => c.id === rec.courseId) || { code: 'N/A', name: 'Unknown' };
            const statuses = Object.values(rec.records || {});
            const presentCount = statuses.filter(s => s === 'PRESENT' || s === 'LATE').length;
            const totalCount = statuses.length;
            const pct = totalCount === 0 ? 0 : Math.round((presentCount / totalCount) * 100);

            return `
                <tr>
                    <td><strong>${rec.date}</strong></td>
                    <td><span class="roll-badge">${course.code}</span> ${course.name}</td>
                    <td>${rec.timeSlot}</td>
                    <td><strong>${presentCount}</strong> / ${totalCount}</td>
                    <td>
                        <span class="badge ${pct >= 75 ? 'badge-success' : 'badge-danger'}">${pct}%</span>
                    </td>
                    <td>
                        <button class="btn btn-secondary btn-sm" onclick="app.loadSpecificSession('${rec.id}')">Edit</button>
                    </td>
                </tr>
            `;
        }).join('');
    }

    populateCourseSelects() {
        const attendanceSelect = document.getElementById('attendanceCourseSelect');
        const reportSelect = document.getElementById('reportCourseSelect');

        const courseOptions = this.courses.map(c => `<option value="${c.id}">${c.code} - ${c.name}</option>`).join('');
        if (attendanceSelect) attendanceSelect.innerHTML = courseOptions;
        if (reportSelect) reportSelect.innerHTML = `<option value="">All Courses</option>` + courseOptions;
    }

    /* ------------------------------------------------------------------------
       Faculty Attendance Marking View (Fast & Efficient)
       ------------------------------------------------------------------------ */
    initAttendanceMarkingView() {
        this.populateCourseSelects();
        this.checkExistingSession();
    }

    onAttendanceCourseChange() {
        this.checkExistingSession();
    }

    checkExistingSession() {
        const courseId = document.getElementById('attendanceCourseSelect').value;
        const date = document.getElementById('attendanceDateInput').value;

        // Check if a session already exists for this course and date
        const existing = this.records.find(r => r.courseId === courseId && r.date === date);

        this.currentAttendanceState = {};
        if (existing) {
            this.currentAttendanceState = { ...existing.records };
            document.getElementById('attendanceTimeSlot').value = existing.timeSlot || '09:00 AM - 10:00 AM';
            document.getElementById('attendanceTopicInput').value = existing.notes || '';
            this.showToast('Loaded existing attendance session for this date.', 'info');
        } else {
            // Default: Mark all present to save faculty time!
            this.students.forEach(s => {
                this.currentAttendanceState[s.id] = 'PRESENT';
            });
            document.getElementById('attendanceTopicInput').value = '';
        }

        this.renderAttendanceMarkingTable();
        this.updateAttendanceLiveCounters();
    }

    loadSpecificSession(recId) {
        const rec = this.records.find(r => r.id === recId);
        if (!rec) return;

        this.showTab('take-attendance');
        document.getElementById('attendanceCourseSelect').value = rec.courseId;
        document.getElementById('attendanceDateInput').value = rec.date;
        document.getElementById('attendanceTimeSlot').value = rec.timeSlot;
        document.getElementById('attendanceTopicInput').value = rec.notes || '';

        this.currentAttendanceState = { ...rec.records };
        this.renderAttendanceMarkingTable();
        this.updateAttendanceLiveCounters();
    }

    renderAttendanceMarkingTable(filterQuery = '') {
        const tbody = document.getElementById('attendanceMarkingBody');
        const query = filterQuery.toLowerCase().trim();

        const filteredStudents = this.students.filter(s => {
            if (!query) return true;
            return s.rollNo.toLowerCase().includes(query) || s.name.toLowerCase().includes(query);
        });

        if (filteredStudents.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 2rem;">No matching students found.</td></tr>`;
            return;
        }

        tbody.innerHTML = filteredStudents.map(student => {
            const currentStatus = this.currentAttendanceState[student.id] || 'PRESENT';
            const stats = this.getStudentStats(student.id);

            let statusBadge = 'badge-success';
            if (stats.percentage < 65) statusBadge = 'badge-danger';
            else if (stats.percentage < 75) statusBadge = 'badge-warning';

            return `
                <tr data-student-id="${student.id}">
                    <td><span class="roll-badge">${student.rollNo}</span></td>
                    <td><strong>${student.name}</strong></td>
                    <td>${student.department}</td>
                    <td>
                        <span class="badge ${statusBadge}">${stats.percentage}%</span>
                    </td>
                    <td style="text-align: center;">
                        <div class="status-btn-group">
                            <button type="button" class="status-btn ${currentStatus === 'PRESENT' ? 'active' : ''}" 
                                data-status="PRESENT" onclick="app.setStudentStatus('${student.id}', 'PRESENT')">
                                Present
                            </button>
                            <button type="button" class="status-btn ${currentStatus === 'ABSENT' ? 'active' : ''}" 
                                data-status="ABSENT" onclick="app.setStudentStatus('${student.id}', 'ABSENT')">
                                Absent
                            </button>
                            <button type="button" class="status-btn ${currentStatus === 'LATE' ? 'active' : ''}" 
                                data-status="LATE" onclick="app.setStudentStatus('${student.id}', 'LATE')">
                                Late
                            </button>
                            <button type="button" class="status-btn ${currentStatus === 'EXCUSED' ? 'active' : ''}" 
                                data-status="EXCUSED" onclick="app.setStudentStatus('${student.id}', 'EXCUSED')">
                                Leave
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    setStudentStatus(studentId, status) {
        this.currentAttendanceState[studentId] = status;
        
        // Update DOM row quickly without re-rendering entire table
        const row = document.querySelector(`tr[data-student-id="${studentId}"]`);
        if (row) {
            row.querySelectorAll('.status-btn').forEach(btn => {
                btn.classList.toggle('active', btn.getAttribute('data-status') === status);
            });
        }

        this.updateAttendanceLiveCounters();
    }

    markAllStatus(status) {
        this.students.forEach(s => {
            this.currentAttendanceState[s.id] = status;
        });
        this.renderAttendanceMarkingTable(document.getElementById('attendanceFilterInput').value);
        this.updateAttendanceLiveCounters();
        this.showToast(`Marked all students as ${status}!`, 'info');
    }

    filterAttendanceList() {
        const q = document.getElementById('attendanceFilterInput').value;
        this.renderAttendanceMarkingTable(q);
    }

    updateAttendanceLiveCounters() {
        let present = 0;
        let absent = 0;
        let late = 0;
        let excused = 0;
        const total = this.students.length;

        Object.values(this.currentAttendanceState).forEach(st => {
            if (st === 'PRESENT') present++;
            else if (st === 'ABSENT') absent++;
            else if (st === 'LATE') late++;
            else if (st === 'EXCUSED') excused++;
        });

        const attendedTotal = present + late;
        const turnoutPct = total === 0 ? 0 : Math.round((attendedTotal / total) * 100);

        document.getElementById('counterPresent').textContent = present;
        document.getElementById('counterAbsent').textContent = absent;
        document.getElementById('counterLate').textContent = late;
        document.getElementById('counterExcused').textContent = excused;
        document.getElementById('counterTurnout').textContent = `${turnoutPct}% (${attendedTotal}/${total})`;
    }

    async submitAttendanceSession() {
        const courseId = document.getElementById('attendanceCourseSelect').value;
        const date = document.getElementById('attendanceDateInput').value;
        const timeSlot = document.getElementById('attendanceTimeSlot').value;
        const notes = document.getElementById('attendanceTopicInput').value;

        if (!courseId || !date) {
            this.showToast('Please select course and date.', 'danger');
            return;
        }

        const sessionRecord = {
            id: 'rec_' + Date.now().toString(36),
            courseId,
            date,
            timeSlot,
            notes,
            timestamp: Date.now(),
            records: { ...this.currentAttendanceState }
        };

        if (this.isBackendConnected) {
            try {
                const res = await fetch(`${this.apiBase}/api/attendance`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(sessionRecord)
                });
                if (res.ok) {
                    const saved = await res.json();
                    const existingIdx = this.records.findIndex(r => r.courseId === courseId && r.date === date);
                    if (existingIdx !== -1) {
                        this.records[existingIdx] = saved;
                    } else {
                        this.records.unshift(saved);
                    }
                    this.showToast('Attendance recorded and synced with Java server!', 'success');
                    this.renderAll();
                    return;
                }
            } catch (e) {
                console.error('API save error, saving locally:', e);
            }
        }

        // Save locally
        const existingIndex = this.records.findIndex(r => r.courseId === courseId && r.date === date);
        if (existingIndex !== -1) {
            sessionRecord.id = this.records[existingIndex].id;
            this.records[existingIndex] = sessionRecord;
        } else {
            this.records.unshift(sessionRecord);
        }
        this.saveLocalData();

        this.showToast('Attendance recorded successfully!', 'success');
        this.renderAll();
    }

    /* ------------------------------------------------------------------------
       Students Directory Tab
       ------------------------------------------------------------------------ */
    renderStudentDirectory(filterQuery = '') {
        const tbody = document.getElementById('studentDirectoryBody');
        const q = filterQuery.toLowerCase().trim();

        const filtered = this.students.filter(s => {
            if (!q) return true;
            return s.rollNo.toLowerCase().includes(q) || s.name.toLowerCase().includes(q) || s.email.toLowerCase().includes(q);
        });

        if (filtered.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; padding: 2rem;">No students found.</td></tr>`;
            return;
        }

        tbody.innerHTML = filtered.map(student => {
            const stats = this.getStudentStats(student.id);
            let badgeClass = 'badge-success';
            let statusLabel = 'Eligible (>=75%)';

            if (stats.percentage < 65) {
                badgeClass = 'badge-danger';
                statusLabel = 'Debarred (<65%)';
            } else if (stats.percentage < 75) {
                badgeClass = 'badge-warning';
                statusLabel = 'Shortage Warning';
            }

            return `
                <tr>
                    <td><span class="roll-badge">${student.rollNo}</span></td>
                    <td><strong>${student.name}</strong></td>
                    <td>${student.email}</td>
                    <td>${student.department}</td>
                    <td>Sem ${student.semester}</td>
                    <td><strong>${stats.present}</strong> / ${stats.total}</td>
                    <td><strong>${stats.percentage}%</strong></td>
                    <td><span class="badge ${badgeClass}">${statusLabel}</span></td>
                    <td>
                        <div style="display:flex; gap: 0.4rem;">
                            <button class="btn btn-secondary btn-sm" onclick="app.viewStudentDetail('${student.id}')">Profile</button>
                            <button class="btn btn-outline-danger btn-sm" onclick="app.deleteStudent('${student.id}')">Delete</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    filterStudents() {
        const q = document.getElementById('studentSearchInput').value;
        this.renderStudentDirectory(q);
    }

    openAddStudentModal() {
        document.getElementById('addStudentForm').reset();
        document.getElementById('addStudentModal').classList.add('active');
    }

    async saveNewStudent(e) {
        e.preventDefault();
        const rollNo = document.getElementById('newRollNo').value.trim();
        const name = document.getElementById('newName').value.trim();
        const email = document.getElementById('newEmail').value.trim();
        const department = document.getElementById('newDept').value.trim();
        const semester = parseInt(document.getElementById('newSem').value) || 5;

        const newStudent = {
            id: 's_' + Date.now().toString(36),
            rollNo,
            name,
            email,
            department,
            semester
        };

        if (this.isBackendConnected) {
            try {
                const res = await fetch(`${this.apiBase}/api/students`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(newStudent)
                });
                if (res.ok) {
                    const saved = await res.json();
                    this.students.push(saved);
                    this.closeModal('addStudentModal');
                    this.showToast('Student added successfully!', 'success');
                    this.renderAll();
                    return;
                }
            } catch (err) {
                console.error('Failed to post student to API:', err);
            }
        }

        this.students.push(newStudent);
        this.saveLocalData();
        this.closeModal('addStudentModal');
        this.showToast('Student added successfully!', 'success');
        this.renderAll();
    }

    async deleteStudent(studentId) {
        if (!confirm('Are you sure you want to delete this student record?')) return;

        if (this.isBackendConnected) {
            try {
                const res = await fetch(`${this.apiBase}/api/students?id=${studentId}`, { method: 'DELETE' });
                if (res.ok) {
                    this.students = this.students.filter(s => s.id !== studentId);
                    this.showToast('Student deleted.', 'info');
                    this.renderAll();
                    return;
                }
            } catch (err) {}
        }

        this.students = this.students.filter(s => s.id !== studentId);
        // Also remove from records
        this.records.forEach(r => {
            if (r.records) delete r.records[studentId];
        });
        this.saveLocalData();
        this.showToast('Student deleted.', 'info');
        this.renderAll();
    }

    viewStudentDetail(studentId) {
        const student = this.students.find(s => s.id === studentId);
        if (!student) return;

        document.getElementById('modalStudentName').textContent = student.name;
        document.getElementById('modalStudentRoll').textContent = `${student.rollNo} • ${student.department} (Sem ${student.semester})`;

        const overallStats = this.getStudentStats(student.id);
        const summaryStrip = document.getElementById('modalStudentSummary');
        summaryStrip.innerHTML = `
            <div class="student-summary-item">
                <span>Total Lectures</span>
                <strong>${overallStats.total}</strong>
            </div>
            <div class="student-summary-item">
                <span>Attended Lectures</span>
                <strong class="text-success">${overallStats.present}</strong>
            </div>
            <div class="student-summary-item">
                <span>Overall Percentage</span>
                <strong class="${overallStats.percentage >= 75 ? 'text-success' : 'text-danger'}">${overallStats.percentage}%</strong>
            </div>
        `;

        const coursesBody = document.getElementById('modalStudentCoursesBody');
        coursesBody.innerHTML = this.courses.map(c => {
            const cStats = this.getStudentStats(student.id, c.id);
            const isEligible = cStats.percentage >= 75;
            return `
                <tr>
                    <td><strong>${c.code}</strong> - ${c.name}</td>
                    <td>${cStats.present} / ${cStats.total}</td>
                    <td><strong>${cStats.percentage}%</strong></td>
                    <td>
                        <span class="badge ${isEligible ? 'badge-success' : 'badge-danger'}">
                            ${isEligible ? 'Eligible for Exam' : 'Attendance Shortage'}
                        </span>
                    </td>
                </tr>
            `;
        }).join('');

        document.getElementById('studentDetailModal').classList.add('active');
    }

    /* ------------------------------------------------------------------------
       Defaulters Alert (<75%) Tab
       ------------------------------------------------------------------------ */
    renderDefaultersTable() {
        const tbody = document.getElementById('defaulterTableBody');
        const defaulters = this.getDefaulters(75.0);

        if (defaulters.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 2.5rem; color: #10b981;">
                <strong>All students meet the 75% attendance threshold! No defaulters.</strong>
            </td></tr>`;
            return;
        }

        tbody.innerHTML = defaulters.map(d => {
            const isCritical = d.severity === 'CRITICAL';
            return `
                <tr>
                    <td><span class="roll-badge">${d.student.rollNo}</span></td>
                    <td><strong>${d.student.name}</strong></td>
                    <td>${d.student.email}</td>
                    <td>${d.stats.present} / ${d.stats.total}</td>
                    <td><strong class="text-danger">${d.stats.percentage}%</strong></td>
                    <td>
                        <span class="badge ${isCritical ? 'badge-danger' : 'badge-warning'}">
                            ${isCritical ? 'CRITICAL (<65%)' : 'WARNING (<75%)'}
                        </span>
                    </td>
                    <td>
                        <span class="badge badge-info">+${d.neededClasses} consecutive classes</span>
                    </td>
                </tr>
            `;
        }).join('');
    }

    copyDefaulterList() {
        const defaulters = this.getDefaulters(75.0);
        if (defaulters.length === 0) {
            this.showToast('No defaulters to copy.', 'info');
            return;
        }

        let notice = `ATTENDANCE SHORTAGE NOTICE (<75%)\n`;
        notice += `Date: ${new Date().toLocaleDateString()}\n\n`;
        notice += `The following students have fallen below the mandatory 75% attendance requirement for exam eligibility:\n\n`;

        defaulters.forEach((d, idx) => {
            notice += `${idx + 1}. [${d.student.rollNo}] ${d.student.name} - ${d.stats.percentage}% (${d.stats.present}/${d.stats.total} classes attended)\n`;
            notice += `   Required: Must attend next ${d.neededClasses} consecutive classes.\n`;
        });

        navigator.clipboard.writeText(notice).then(() => {
            this.showToast('Notice text copied to clipboard!', 'success');
        }).catch(() => {
            this.showToast('Failed to copy. Please copy manually.', 'danger');
        });
    }

    exportDefaultersCsv() {
        const defaulters = this.getDefaulters(75.0);
        let csv = 'Roll No,Student Name,Email,Attended,Total Classes,Attendance %,Severity,Classes Needed To Reach 75%\n';

        defaulters.forEach(d => {
            csv += `"${d.student.rollNo}","${d.student.name}","${d.student.email}",${d.stats.present},${d.stats.total},${d.stats.percentage}%,${d.severity},${d.neededClasses}\n`;
        });

        this.downloadCsv(csv, `defaulters_list_${new Date().toISOString().split('T')[0]}.csv`);
    }

    /* ------------------------------------------------------------------------
       Reports & Export Tab
       ------------------------------------------------------------------------ */
    renderReportTable() {
        const tbody = document.getElementById('reportTableBody');
        const courseId = document.getElementById('reportCourseSelect').value;

        tbody.innerHTML = this.students.map(student => {
            const stats = this.getStudentStats(student.id, courseId);
            const isEligible = stats.percentage >= 75;

            return `
                <tr>
                    <td><span class="roll-badge">${student.rollNo}</span></td>
                    <td><strong>${student.name}</strong></td>
                    <td>${student.department}</td>
                    <td>${stats.total}</td>
                    <td class="text-success">${stats.present}</td>
                    <td class="text-danger">${stats.absent}</td>
                    <td class="text-warning">${stats.late}</td>
                    <td><strong>${stats.percentage}%</strong></td>
                    <td>
                        <span class="badge ${isEligible ? 'badge-success' : 'badge-danger'}">
                            ${isEligible ? 'Eligible' : 'Debarred / Shortage'}
                        </span>
                    </td>
                </tr>
            `;
        }).join('');
    }

    exportAttendanceCsv() {
        const courseId = document.getElementById('reportCourseSelect').value;
        const course = this.courses.find(c => c.id === courseId);
        const courseTitle = course ? `${course.code} - ${course.name}` : 'All Courses';

        let csv = `Student Attendance Report\n`;
        csv += `Course:,"${courseTitle}"\n`;
        csv += `Generated On:,"${new Date().toLocaleString()}"\n\n`;
        csv += `Roll No,Student Name,Department,Total Classes,Attended,Absent,Late,Attendance %,Eligibility\n`;

        this.students.forEach(s => {
            const stats = this.getStudentStats(s.id, courseId);
            const status = stats.percentage >= 75 ? 'ELIGIBLE' : (stats.percentage >= 65 ? 'WARNING (<75%)' : 'DEBARRED (<65%)');
            csv += `"${s.rollNo}","${s.name}","${s.department}",${stats.total},${stats.present},${stats.absent},${stats.late},${stats.percentage}%,"${status}"\n`;
        });

        this.downloadCsv(csv, `attendance_report_${course ? course.code : 'all'}_${new Date().toISOString().split('T')[0]}.csv`);
    }

    downloadCsv(csvContent, fileName) {
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', fileName);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        this.showToast(`Downloaded: ${fileName}`, 'success');
    }

    /* ------------------------------------------------------------------------
       Courses Tab
       ------------------------------------------------------------------------ */
    renderCoursesTable() {
        const tbody = document.getElementById('coursesTableBody');
        tbody.innerHTML = this.courses.map(c => {
            const sessionCount = this.records.filter(r => r.courseId === c.id).length;
            return `
                <tr>
                    <td><span class="roll-badge">${c.code}</span></td>
                    <td><strong>${c.name}</strong></td>
                    <td>${c.department}</td>
                    <td>${c.faculty}</td>
                    <td><strong>${sessionCount}</strong> sessions</td>
                    <td>
                        <button class="btn btn-secondary btn-sm" onclick="app.filterByCourseAndTakeAttendance('${c.id}')">
                            Take Attendance
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    }

    openAddCourseModal() {
        document.getElementById('addCourseForm').reset();
        document.getElementById('addCourseModal').classList.add('active');
    }

    async saveNewCourse(e) {
        e.preventDefault();
        const code = document.getElementById('newCourseCode').value.trim();
        const name = document.getElementById('newCourseName').value.trim();
        const department = document.getElementById('newCourseDept').value.trim();
        const faculty = document.getElementById('newCourseFaculty').value.trim();

        const newCourse = {
            id: 'c_' + Date.now().toString(36),
            code,
            name,
            department,
            faculty
        };

        if (this.isBackendConnected) {
            try {
                const res = await fetch(`${this.apiBase}/api/courses`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(newCourse)
                });
                if (res.ok) {
                    const saved = await res.json();
                    this.courses.push(saved);
                    this.closeModal('addCourseModal');
                    this.showToast('Course created successfully!', 'success');
                    this.renderAll();
                    return;
                }
            } catch (err) {}
        }

        this.courses.push(newCourse);
        this.saveLocalData();
        this.closeModal('addCourseModal');
        this.showToast('Course created successfully!', 'success');
        this.renderAll();
    }

    filterByCourseAndTakeAttendance(courseId) {
        this.showTab('take-attendance');
        document.getElementById('attendanceCourseSelect').value = courseId;
        this.checkExistingSession();
    }

    /* ------------------------------------------------------------------------
       Modal & Toast Utilities
       ------------------------------------------------------------------------ */
    closeModal(modalId) {
        document.getElementById(modalId).classList.remove('active');
    }

    showToast(message, type = 'info') {
        const container = document.getElementById('toastContainer');
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `
            <span>${message}</span>
        `;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(12px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    }
}

// Global instance
const app = new AttendanceApp();
window.app = app;
