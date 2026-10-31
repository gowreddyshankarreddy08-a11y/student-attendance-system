/**
 * AttendSmart - Student Attendance Management & Admin System
 * Full Frontend Controller with Admin Authentication & Dual-Mode (Java API + GitHub Pages LocalStorage)
 */

class AttendanceApp {
    constructor() {
        this.apiBase = '';
        this.isBackendConnected = false;
        this.currentTab = 'dashboard';
        this.currentUser = null;
        
        // In-memory data store
        this.students = [];
        this.courses = [];
        this.records = [];
        this.users = [];
        this.settings = {
            institutionName: 'Vishwa Vishwani Institute Of System And Management',
            minAttendanceThreshold: 75.0,
            academicTerm: 'Academic Year 2026-27 (Fall Sem)'
        };
        this.currentAttendanceState = {}; // studentId -> status

        this.init();
    }

    async init() {
        this.initEventListeners();
        this.setupCurrentDate();
        await this.detectBackend();
        await this.loadAllData();
        this.checkAuthSession();
    }

    /* ------------------------------------------------------------------------
       Authentication & Role-Based Session
       ------------------------------------------------------------------------ */
    checkAuthSession() {
        const storedUser = sessionStorage.getItem('attendsmart_user') || localStorage.getItem('attendsmart_user');
        if (storedUser) {
            try {
                this.currentUser = JSON.parse(storedUser);
                this.showAppLayout();
                return;
            } catch (e) {
                this.currentUser = null;
            }
        }
        this.showLoginScreen();
    }

    showLoginScreen() {
        document.getElementById('loginScreen').style.display = 'flex';
        document.getElementById('mainAppLayout').style.display = 'none';
        
        const instTag = document.getElementById('loginInstitutionTag');
        if (instTag && this.settings.institutionName) {
            instTag.textContent = `${this.settings.institutionName} • Attendance System`;
        }
    }

    showAppLayout() {
        document.getElementById('loginScreen').style.display = 'none';
        document.getElementById('mainAppLayout').style.display = 'flex';

        // Apply User Meta & Permissions
        const role = (this.currentUser && this.currentUser.role) ? this.currentUser.role.toUpperCase() : 'FACULTY';
        const isAdmin = role === 'ADMIN';

        // Update Sidebar User Info
        document.getElementById('sidebarUserName').textContent = this.currentUser ? this.currentUser.name : 'User';
        document.getElementById('sidebarUserRole').textContent = isAdmin ? 'System Administrator' : 'Faculty Member';
        document.getElementById('sidebarAvatar').textContent = this.currentUser && this.currentUser.name ? this.currentUser.name.split(' ').map(n=>n[0]).join('').slice(0, 2).toUpperCase() : 'US';
        
        const roleBadge = document.getElementById('sidebarRoleBadge');
        roleBadge.textContent = isAdmin ? 'Administrator' : 'Faculty Portal';
        roleBadge.className = `role-badge ${isAdmin ? 'admin-role' : ''}`;

        const topbarBadge = document.getElementById('topbarRoleIndicator');
        topbarBadge.textContent = isAdmin ? 'Admin' : 'Faculty';
        topbarBadge.className = `badge ${isAdmin ? 'badge-info' : 'badge-success'}`;

        // Toggle Admin Nav Group Visibility
        const adminNav = document.getElementById('adminNavGroup');
        if (adminNav) {
            adminNav.style.display = isAdmin ? 'flex' : 'none';
        }

        this.renderAll();
    }

    switchAuthTab(tab) {
        const btnSignIn = document.getElementById('tabBtnSignIn');
        const btnSignUp = document.getElementById('tabBtnSignUp');
        const loginForm = document.getElementById('loginForm');
        const signupForm = document.getElementById('signupForm');
        const loginAlert = document.getElementById('loginAlert');
        const signupAlert = document.getElementById('signupAlert');

        if (loginAlert) loginAlert.style.display = 'none';
        if (signupAlert) signupAlert.style.display = 'none';

        if (tab === 'signup') {
            btnSignUp.classList.add('active');
            btnSignIn.classList.remove('active');
            loginForm.style.display = 'none';
            signupForm.style.display = 'flex';
        } else {
            btnSignIn.classList.add('active');
            btnSignUp.classList.remove('active');
            loginForm.style.display = 'flex';
            signupForm.style.display = 'none';
        }
    }

    async handleLoginFormSubmit(e) {
        e.preventDefault();
        const username = document.getElementById('loginUsername').value.trim();
        const password = document.getElementById('loginPassword').value.trim();
        const alertBox = document.getElementById('loginAlert');
        alertBox.style.display = 'none';

        if (this.isBackendConnected) {
            try {
                const res = await fetch(`${this.apiBase}/api/auth/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });

                if (res.ok) {
                    const data = await res.json();
                    if (data.success && data.user) {
                        this.currentUser = data.user;
                        sessionStorage.setItem('attendsmart_user', JSON.stringify(data.user));
                        this.showToast(`Welcome back, ${data.user.name}!`, 'success');
                        this.showAppLayout();
                        return;
                    }
                } else {
                    const data = await res.json().catch(() => ({}));
                    alertBox.textContent = data.error || 'Invalid credentials. Please verify your username and password.';
                    alertBox.style.display = 'block';
                    return;
                }
            } catch (err) {
                console.warn('API login failed, checking local credentials:', err);
            }
        }

        // Local Storage / Client Fallback
        const user = this.users.find(u => u.username.toLowerCase() === username.toLowerCase() && u.password === password);
        if (user) {
            this.currentUser = user;
            sessionStorage.setItem('attendsmart_user', JSON.stringify(user));
            this.showToast(`Signed in as ${user.name}`, 'success');
            this.showAppLayout();
        } else {
            alertBox.textContent = 'Invalid username or password. Please verify your credentials.';
            alertBox.style.display = 'block';
        }
    }

    async handleSignupFormSubmit(e) {
        e.preventDefault();
        const name = document.getElementById('signupName').value.trim();
        const email = document.getElementById('signupEmail').value.trim();
        const department = document.getElementById('signupDept').value.trim();
        const role = document.getElementById('signupRole').value;
        const username = document.getElementById('signupUsername').value.trim();
        const password = document.getElementById('signupPassword').value.trim();
        const alertBox = document.getElementById('signupAlert');
        alertBox.style.display = 'none';

        if (!name || !username || !password) {
            alertBox.textContent = 'Please fill out all required fields.';
            alertBox.style.display = 'block';
            return;
        }

        if (this.isBackendConnected) {
            try {
                const res = await fetch(`${this.apiBase}/api/auth/signup`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, email, department, role, username, password })
                });

                if (res.ok) {
                    const data = await res.json();
                    if (data.success && data.user) {
                        this.currentUser = data.user;
                        sessionStorage.setItem('attendsmart_user', JSON.stringify(data.user));
                        this.showToast(`Account created! Welcome, ${data.user.name}!`, 'success');
                        this.showAppLayout();
                        return;
                    }
                } else {
                    const data = await res.json().catch(() => ({}));
                    alertBox.textContent = data.error || 'Sign up failed. Username may already be taken.';
                    alertBox.style.display = 'block';
                    return;
                }
            } catch (err) {
                console.warn('Backend signup error:', err);
            }
        }

        // Local Storage Fallback
        const exists = this.users.some(u => u.username.toLowerCase() === username.toLowerCase());
        if (exists) {
            alertBox.textContent = 'Username already exists. Please choose a different username.';
            alertBox.style.display = 'block';
            return;
        }

        const newUser = {
            id: 'u_' + Date.now().toString(36),
            username,
            password,
            name,
            email,
            department: department || 'General',
            role: role || 'FACULTY'
        };

        this.users.push(newUser);
        this.saveLocalData();
        this.currentUser = newUser;
        sessionStorage.setItem('attendsmart_user', JSON.stringify(newUser));
        this.showToast(`Account created! Welcome, ${newUser.name}!`, 'success');
        this.showAppLayout();
    }

    logout() {
        this.currentUser = null;
        sessionStorage.removeItem('attendsmart_user');
        localStorage.removeItem('attendsmart_user');
        this.showToast('You have been logged out.', 'info');
        this.showLoginScreen();
    }

    /* ------------------------------------------------------------------------
       Backend Detection & Data Loading (Dual-Mode)
       ------------------------------------------------------------------------ */
    async detectBackend() {
        const badge = document.getElementById('backendStatusBadge');
        const text = document.getElementById('backendStatusText');

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

        this.isBackendConnected = false;
        badge.className = 'mode-badge demo';
        text.textContent = 'Live Web App (Local Storage)';
    }

    async loadAllData() {
        if (this.isBackendConnected) {
            try {
                const [studRes, courseRes, recRes, userRes, settRes] = await Promise.all([
                    fetch(`${this.apiBase}/api/students`).then(r => r.json()),
                    fetch(`${this.apiBase}/api/courses`).then(r => r.json()),
                    fetch(`${this.apiBase}/api/attendance`).then(r => r.json()),
                    fetch(`${this.apiBase}/api/admin/users`).then(r => r.json()).catch(() => []),
                    fetch(`${this.apiBase}/api/admin/settings`).then(r => r.json()).catch(() => ({}))
                ]);
                this.students = studRes;
                this.courses = courseRes;
                this.records = recRes;
                if (userRes && userRes.length) this.users = userRes;
                if (settRes && settRes.minAttendanceThreshold) {
                    this.settings = {
                        institutionName: settRes.institutionName || 'Vishwa Vishwani Institute Of System And Management',
                        minAttendanceThreshold: parseFloat(settRes.minAttendanceThreshold) || 75.0,
                        academicTerm: settRes.academicTerm || 'Fall 2026'
                    };
                }
                return;
            } catch (e) {
                console.warn('Backend fetch failed, falling back to local storage', e);
            }
        }

        this.loadLocalData();
    }

    loadLocalData() {
        const storedStudents = localStorage.getItem('attendsmart_students');
        const storedCourses = localStorage.getItem('attendsmart_courses');
        const storedRecords = localStorage.getItem('attendsmart_records');
        const storedUsers = localStorage.getItem('attendsmart_users');
        const storedSettings = localStorage.getItem('attendsmart_settings');

        if (storedUsers) {
            try {
                const users = JSON.parse(storedUsers);
                const adminUser = users.find(u => u.username && u.username.toLowerCase() === 'admin');
                // Ensure admin password is updated to Admin@123
                if (!adminUser || adminUser.password !== 'Admin@123') {
                    this.seedLocalData();
                    return;
                }
            } catch (e) {
                this.seedLocalData();
                return;
            }
        } else {
            this.seedLocalData();
            return;
        }

        this.students = storedStudents ? JSON.parse(storedStudents) : [];
        this.courses = storedCourses ? JSON.parse(storedCourses) : [];
        this.records = storedRecords ? JSON.parse(storedRecords) : [];
        this.users = storedUsers ? JSON.parse(storedUsers) : [];
        if (storedSettings) this.settings = JSON.parse(storedSettings);
    }

    saveLocalData() {
        localStorage.setItem('attendsmart_students', JSON.stringify(this.students));
        localStorage.setItem('attendsmart_courses', JSON.stringify(this.courses));
        localStorage.setItem('attendsmart_records', JSON.stringify(this.records));
        localStorage.setItem('attendsmart_users', JSON.stringify(this.users));
        localStorage.setItem('attendsmart_settings', JSON.stringify(this.settings));
    }

    seedLocalData() {
        this.settings = {
            institutionName: 'Vishwa Vishwani Institute Of System And Management',
            minAttendanceThreshold: 75.0,
            academicTerm: 'Academic Year 2026-27'
        };

        // Root Administrator: admin / Admin@123
        this.users = [
            { id: 'u_admin', username: 'admin', password: 'Admin@123', name: 'System Administrator', role: 'ADMIN', email: 'admin@institution.edu', department: 'Administration' }
        ];

        // Clean initial state: zero dummy students, courses, or records
        this.courses = [];
        this.students = [];
        this.records = [];

        this.saveLocalData();
    }

    async loadDemoData() {
        if (!confirm('Load demonstration sample courses and students?')) return;

        if (this.isBackendConnected) {
            try {
                await fetch(`${this.apiBase}/api/admin/demo-data`, { method: 'POST' });
                await this.loadAllData();
                this.showToast('Demo data loaded successfully.', 'info');
                this.renderAll();
                return;
            } catch (err) {}
        }

        // Local storage demo data
        this.courses = [
            { id: 'c1', code: 'CS301', name: 'Database Management Systems', department: 'Computer Science', faculty: 'Dr. Jane Smith' },
            { id: 'c2', code: 'CS302', name: 'Operating Systems', department: 'Computer Science', faculty: 'Prof. Alan Brown' }
        ];

        this.students = [
            { id: 's1', rollNo: 'CS-101', name: 'Aarav Sharma', email: 'aarav@institution.edu', department: 'Computer Science', semester: 5 },
            { id: 's2', rollNo: 'CS-102', name: 'Aditi Patel', email: 'aditi@institution.edu', department: 'Computer Science', semester: 5 },
            { id: 's3', rollNo: 'CS-103', name: 'Bhavani Shankar', email: 'bhavani@institution.edu', department: 'Computer Science', semester: 5 },
            { id: 's4', rollNo: 'CS-104', name: 'Devika Nambiar', email: 'devika@institution.edu', department: 'Computer Science', semester: 5 },
            { id: 's5', rollNo: 'CS-105', name: 'Ishaan Verma', email: 'ishaan@institution.edu', department: 'Computer Science', semester: 5 }
        ];

        this.records = [];
        this.saveLocalData();
        this.showToast('Demo courses and students loaded.', 'info');
        this.renderAll();
    }

    /* ------------------------------------------------------------------------
       UI Navigation & Events
       ------------------------------------------------------------------------ */
    initEventListeners() {
        document.querySelectorAll('.sidebar-nav .nav-item').forEach(btn => {
            btn.addEventListener('click', () => {
                const tab = btn.getAttribute('data-tab');
                this.showTab(tab);
            });
        });

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
        document.getElementById('currentDateDisplay').textContent = now.toLocaleDateString('en-US', options);

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

        const titleMap = {
            'dashboard': 'Dashboard Overview',
            'take-attendance': 'Take Student Attendance',
            'students': 'Student Directory',
            'defaulters': `Defaulters Alert (<${this.settings.minAttendanceThreshold}%)`,
            'reports': 'Attendance Reports & Analytics',
            'courses': 'Course Roster',
            'admin-faculty': 'Faculty & Instructor Management',
            'admin-settings': 'Institutional System Settings'
        };
        document.getElementById('pageTitle').textContent = titleMap[tabId] || 'Attendance System';

        const sidebar = document.getElementById('sidebar');
        if (sidebar) sidebar.classList.remove('open');

        if (tabId === 'take-attendance') this.initAttendanceMarkingView();
        else if (tabId === 'students') this.renderStudentDirectory();
        else if (tabId === 'defaulters') this.renderDefaultersTable();
        else if (tabId === 'reports') this.renderReportTable();
        else if (tabId === 'courses') this.renderCoursesTable();
        else if (tabId === 'admin-faculty') this.renderAdminFacultyTable();
        else if (tabId === 'admin-settings') this.renderAdminSettingsForm();
        else if (tabId === 'dashboard') this.renderDashboard();
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

    getDefaulters(threshold = null, courseId = null) {
        if (threshold === null) threshold = this.settings.minAttendanceThreshold || 75.0;
        const defaulters = [];
        this.students.forEach(s => {
            const stats = this.getStudentStats(s.id, courseId);
            if (stats.total > 0 && stats.percentage < threshold) {
                const needed = Math.max(0, Math.ceil((threshold / 100 * stats.total - stats.present) / (1 - threshold / 100)));
                defaulters.push({
                    student: s,
                    stats,
                    neededClasses: needed,
                    severity: stats.percentage < (threshold - 10) ? 'CRITICAL' : 'WARNING'
                });
            }
        });
        return defaulters.sort((a, b) => a.stats.percentage - b.stats.percentage);
    }

    /* ------------------------------------------------------------------------
       Rendering Views
       ------------------------------------------------------------------------ */
    renderAll() {
        this.populateCourseSelects();
        this.renderDashboard();
    }

    renderDashboard() {
        const totalStudents = this.students.length;
        const totalSessions = this.records.length;
        const defaulters = this.getDefaulters();

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
        document.getElementById('statThresholdSub').textContent = `Target: ${this.settings.minAttendanceThreshold}%+`;
        document.getElementById('healthTermBadge').textContent = this.settings.academicTerm || 'Current Term';

        // Course Health
        const healthList = document.getElementById('courseHealthList');
        const courseHealth = this.getCourseHealth();
        if (courseHealth.length === 0) {
            healthList.innerHTML = `
                <div class="empty-state-card" style="padding: 1.75rem 1rem;">
                    <p class="empty-state-desc" style="margin-bottom: 0.75rem;">No courses registered yet. Create your courses to monitor attendance health.</p>
                    <button class="btn btn-primary btn-sm" onclick="app.showTab('courses')">Add Your First Course</button>
                </div>
            `;
        } else {
            healthList.innerHTML = courseHealth.map(item => {
                let colorClass = 'good';
                if (item.attendancePct < 65) colorClass = 'critical';
                else if (item.attendancePct < this.settings.minAttendanceThreshold) colorClass = 'warning';

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
        }

        // Recent Sessions
        const recentBody = document.getElementById('recentSessionsTableBody');
        const sortedRecords = [...this.records].sort((a, b) => b.date.localeCompare(a.date)).slice(0, 5);

        if (sortedRecords.length === 0) {
            recentBody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 2.5rem; color: var(--text-secondary);">No attendance sessions recorded yet. Click "Take Attendance" once courses and students are added.</td></tr>`;
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
                        <span class="badge ${pct >= this.settings.minAttendanceThreshold ? 'badge-success' : 'badge-danger'}">${pct}%</span>
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

        if (this.courses.length === 0) {
            if (attendanceSelect) attendanceSelect.innerHTML = `<option value="">-- No Courses Created Yet --</option>`;
            if (reportSelect) reportSelect.innerHTML = `<option value="">All Courses</option>`;
            return;
        }

        const courseOptions = this.courses.map(c => `<option value="${c.id}">${c.code} - ${c.name}</option>`).join('');
        if (attendanceSelect) attendanceSelect.innerHTML = courseOptions;
        if (reportSelect) reportSelect.innerHTML = `<option value="">All Courses</option>` + courseOptions;
    }

    /* ------------------------------------------------------------------------
       Attendance Taking
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

        const existing = this.records.find(r => r.courseId === courseId && r.date === date);

        this.currentAttendanceState = {};
        if (existing) {
            this.currentAttendanceState = { ...existing.records };
            document.getElementById('attendanceTimeSlot').value = existing.timeSlot || '09:00 AM - 10:00 AM';
            document.getElementById('attendanceTopicInput').value = existing.notes || '';
            this.showToast('Loaded existing attendance session for this date.', 'info');
        } else {
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

        if (this.courses.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 2.5rem; color: var(--text-secondary);">No courses created yet. <a href="javascript:void(0)" onclick="app.showTab('courses')">Create your first course</a> to take attendance.</td></tr>`;
            return;
        }

        if (this.students.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 2.5rem; color: var(--text-secondary);">No students enrolled yet. <a href="javascript:void(0)" onclick="app.showTab('students')">Add students</a> to begin recording attendance.</td></tr>`;
            return;
        }

        const filteredStudents = this.students.filter(s => {
            if (!query) return true;
            return s.rollNo.toLowerCase().includes(query) || s.name.toLowerCase().includes(query);
        });

        if (filteredStudents.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 2rem;">No matching students found for "${filterQuery}".</td></tr>`;
            return;
        }

        tbody.innerHTML = filteredStudents.map(student => {
            const currentStatus = this.currentAttendanceState[student.id] || 'PRESENT';
            const stats = this.getStudentStats(student.id);

            let statusBadge = 'badge-success';
            if (stats.percentage < 65) statusBadge = 'badge-danger';
            else if (stats.percentage < this.settings.minAttendanceThreshold) statusBadge = 'badge-warning';

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
                console.error('API save error:', e);
            }
        }

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
       Students Directory
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
            let statusLabel = `Eligible (>=${this.settings.minAttendanceThreshold}%)`;

            if (stats.percentage < 65) {
                badgeClass = 'badge-danger';
                statusLabel = 'Debarred (<65%)';
            } else if (stats.percentage < this.settings.minAttendanceThreshold) {
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
        this.renderStudentDirectory(document.getElementById('studentSearchInput').value);
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

        const newStudent = { id: 's_' + Date.now().toString(36), rollNo, name, email, department, semester };

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
            } catch (err) {}
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
        this.records.forEach(r => { if (r.records) delete r.records[studentId]; });
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
                <strong class="${overallStats.percentage >= this.settings.minAttendanceThreshold ? 'text-success' : 'text-danger'}">${overallStats.percentage}%</strong>
            </div>
        `;

        const coursesBody = document.getElementById('modalStudentCoursesBody');
        coursesBody.innerHTML = this.courses.map(c => {
            const cStats = this.getStudentStats(student.id, c.id);
            const isEligible = cStats.percentage >= this.settings.minAttendanceThreshold;
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
       Defaulters Alert
       ------------------------------------------------------------------------ */
    renderDefaultersTable() {
        const tbody = document.getElementById('defaulterTableBody');
        const threshold = this.settings.minAttendanceThreshold || 75.0;
        document.getElementById('defaulterThresholdText').textContent = threshold;
        const defaulters = this.getDefaulters(threshold);

        if (defaulters.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 2.5rem; color: #10b981;">
                <strong>All students meet the ${threshold}% attendance threshold! No defaulters.</strong>
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
                            ${isCritical ? 'CRITICAL (<65%)' : `WARNING (<${threshold}%)`}
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
        const threshold = this.settings.minAttendanceThreshold || 75.0;
        const defaulters = this.getDefaulters(threshold);
        if (defaulters.length === 0) {
            this.showToast('No defaulters to copy.', 'info');
            return;
        }

        let notice = `ATTENDANCE SHORTAGE NOTICE (<${threshold}%)\n`;
        notice += `Institution: ${this.settings.institutionName}\n`;
        notice += `Date: ${new Date().toLocaleDateString()}\n\n`;
        notice += `The following students have fallen below the mandatory ${threshold}% attendance requirement for exam eligibility:\n\n`;

        defaulters.forEach((d, idx) => {
            notice += `${idx + 1}. [${d.student.rollNo}] ${d.student.name} - ${d.stats.percentage}% (${d.stats.present}/${d.stats.total} classes attended)\n`;
            notice += `   Recovery Action: Must attend next ${d.neededClasses} consecutive classes.\n`;
        });

        navigator.clipboard.writeText(notice).then(() => {
            this.showToast('Notice text copied to clipboard!', 'success');
        }).catch(() => {
            this.showToast('Failed to copy.', 'danger');
        });
    }

    exportDefaultersCsv() {
        const defaulters = this.getDefaulters();
        let csv = 'Roll No,Student Name,Email,Attended,Total Classes,Attendance %,Severity,Classes Needed To Recover\n';

        defaulters.forEach(d => {
            csv += `"${d.student.rollNo}","${d.student.name}","${d.student.email}",${d.stats.present},${d.stats.total},${d.stats.percentage}%,${d.severity},${d.neededClasses}\n`;
        });

        this.downloadCsv(csv, `defaulters_list_${new Date().toISOString().split('T')[0]}.csv`);
    }

    /* ------------------------------------------------------------------------
       Reports & Export
       ------------------------------------------------------------------------ */
    renderReportTable() {
        const tbody = document.getElementById('reportTableBody');
        const courseId = document.getElementById('reportCourseSelect').value;

        tbody.innerHTML = this.students.map(student => {
            const stats = this.getStudentStats(student.id, courseId);
            const isEligible = stats.percentage >= this.settings.minAttendanceThreshold;

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

        let csv = `Student Attendance Official Report\n`;
        csv += `Institution:,"${this.settings.institutionName}"\n`;
        csv += `Course:,"${courseTitle}"\n`;
        csv += `Generated On:,"${new Date().toLocaleString()}"\n\n`;
        csv += `Roll No,Student Name,Department,Total Classes,Attended,Absent,Late,Attendance %,Eligibility\n`;

        this.students.forEach(s => {
            const stats = this.getStudentStats(s.id, courseId);
            const status = stats.percentage >= this.settings.minAttendanceThreshold ? 'ELIGIBLE' : (stats.percentage >= 65 ? 'WARNING (<75%)' : 'DEBARRED (<65%)');
            csv += `"${s.rollNo}","${s.name}","${s.department}",${stats.total},${stats.present},${stats.absent},${stats.late},${stats.percentage}%,"${status}"\n`;
        });

        this.downloadCsv(csv, `attendance_report_${course ? course.code : 'all'}_${new Date().toISOString().split('T')[0]}.csv`);
    }

    /* ------------------------------------------------------------------------
       ADMIN: Faculty Roster
       ------------------------------------------------------------------------ */
    renderAdminFacultyTable() {
        const tbody = document.getElementById('adminFacultyTableBody');
        if (!tbody) return;

        if (this.users.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 2rem;">No faculty registered yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = this.users.map(u => {
            const assignedCourses = this.courses.filter(c => c.faculty && c.faculty.toLowerCase().includes(u.name.toLowerCase()));
            const courseBadges = assignedCourses.length > 0 
                ? assignedCourses.map(c => `<span class="roll-badge">${c.code}</span>`).join(' ')
                : `<span class="text-muted">None</span>`;

            const isRootAdmin = u.username.toLowerCase() === 'admin';

            return `
                <tr>
                    <td><span class="roll-badge">${u.username}</span></td>
                    <td><strong>${u.name}</strong></td>
                    <td>${u.email}</td>
                    <td>${u.department}</td>
                    <td>
                        <span class="badge ${u.role === 'ADMIN' ? 'badge-info' : 'badge-success'}">${u.role}</span>
                    </td>
                    <td>${courseBadges}</td>
                    <td>
                        ${isRootAdmin 
                            ? `<span class="badge badge-secondary">Protected</span>` 
                            : `<button class="btn btn-outline-danger btn-sm" onclick="app.deleteFaculty('${u.username}')">Remove</button>`
                        }
                    </td>
                </tr>
            `;
        }).join('');
    }

    openAddFacultyModal() {
        document.getElementById('addFacultyForm').reset();
        document.getElementById('addFacultyModal').classList.add('active');
    }

    async saveNewFaculty(e) {
        e.preventDefault();
        const username = document.getElementById('facultyUsername').value.trim();
        const name = document.getElementById('facultyName').value.trim();
        const email = document.getElementById('facultyEmail').value.trim();
        const department = document.getElementById('facultyDept').value.trim();
        const role = document.getElementById('facultyRole').value;
        const password = document.getElementById('facultyPassword').value.trim() || 'faculty123';

        const newFaculty = {
            id: 'u_' + Date.now().toString(36),
            username,
            name,
            email,
            department,
            role,
            password
        };

        if (this.isBackendConnected) {
            try {
                const res = await fetch(`${this.apiBase}/api/admin/users`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(newFaculty)
                });
                if (res.ok) {
                    const saved = await res.json();
                    this.users.push(saved);
                    this.closeModal('addFacultyModal');
                    this.showToast('Faculty member registered!', 'success');
                    this.renderAdminFacultyTable();
                    return;
                }
            } catch (err) {}
        }

        this.users.push(newFaculty);
        this.saveLocalData();
        this.closeModal('addFacultyModal');
        this.showToast('Faculty member registered!', 'success');
        this.renderAdminFacultyTable();
    }

    async deleteFaculty(username) {
        if (!confirm(`Are you sure you want to remove faculty account: ${username}?`)) return;

        if (this.isBackendConnected) {
            try {
                const res = await fetch(`${this.apiBase}/api/admin/users?username=${username}`, { method: 'DELETE' });
                if (res.ok) {
                    this.users = this.users.filter(u => u.username !== username);
                    this.showToast('Faculty removed.', 'info');
                    this.renderAdminFacultyTable();
                    return;
                }
            } catch (err) {}
        }

        this.users = this.users.filter(u => u.username !== username);
        this.saveLocalData();
        this.showToast('Faculty removed.', 'info');
        this.renderAdminFacultyTable();
    }

    /* ------------------------------------------------------------------------
       ADMIN: System Settings & Database Management
       ------------------------------------------------------------------------ */
    renderAdminSettingsForm() {
        document.getElementById('settingInstitutionName').value = this.settings.institutionName || 'Vishwa Vishwani Institute Of System And Management';
        document.getElementById('settingThreshold').value = this.settings.minAttendanceThreshold || 75;
        document.getElementById('settingAcademicTerm').value = this.settings.academicTerm || 'Fall 2026';
    }

    async saveAdminSettings(e) {
        e.preventDefault();
        const instName = document.getElementById('settingInstitutionName').value.trim();
        const threshold = parseFloat(document.getElementById('settingThreshold').value) || 75.0;
        const term = document.getElementById('settingAcademicTerm').value.trim();

        this.settings.institutionName = instName;
        this.settings.minAttendanceThreshold = threshold;
        this.settings.academicTerm = term;

        if (this.isBackendConnected) {
            try {
                await fetch(`${this.apiBase}/api/admin/settings`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        institutionName: instName,
                        minAttendanceThreshold: threshold.toString(),
                        academicTerm: term
                    })
                });
            } catch (err) {}
        }

        this.saveLocalData();
        this.showToast('Institutional settings updated!', 'success');
        this.renderAll();
    }

    exportFullDatabaseJson() {
        const fullBackup = {
            institution: this.settings,
            users: this.users,
            courses: this.courses,
            students: this.students,
            records: this.records,
            exportedAt: new Date().toISOString()
        };
        const jsonStr = JSON.stringify(fullBackup, null, 2);
        const blob = new Blob([jsonStr], { type: 'application/json' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `attendsmart_database_backup_${new Date().toISOString().split('T')[0]}.json`;
        link.click();
        this.showToast('Database backup downloaded!', 'success');
    }

    async resetSystemData() {
        if (!confirm('Warning: This will clear all courses, students, and attendance records to start completely fresh. Continue?')) return;

        if (this.isBackendConnected) {
            try {
                await fetch(`${this.apiBase}/api/admin/reset`, { method: 'POST' });
                await this.loadAllData();
                this.showToast('Database cleared to clean initial state.', 'info');
                this.renderAll();
                return;
            } catch (err) {}
        }

        this.seedLocalData();
        this.showToast('Database cleared to clean initial state.', 'info');
        this.renderAll();
    }

    /* ------------------------------------------------------------------------
       Courses Tab
       ------------------------------------------------------------------------ */
    renderCoursesTable() {
        const tbody = document.getElementById('coursesTableBody');
        if (this.courses.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 2.5rem; color: var(--text-secondary);">No courses registered yet. Click "Add Course" above to create your first subject.</td></tr>`;
            return;
        }

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

        const newCourse = { id: 'c_' + Date.now().toString(36), code, name, department, faculty };

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
       Utilities
       ------------------------------------------------------------------------ */
    closeModal(modalId) {
        document.getElementById(modalId).classList.remove('active');
    }

    downloadCsv(csvContent, fileName) {
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = fileName;
        link.click();
        this.showToast(`Downloaded: ${fileName}`, 'success');
    }

    showToast(message, type = 'info') {
        const container = document.getElementById('toastContainer');
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `<span>${message}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(12px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    }
}

// Instantiate global app
const app = new AttendanceApp();
window.app = app;
