package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.localization.AppStrings
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang by viewModel.language.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    val teachers by viewModel.teachers.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val students by viewModel.students.collectAsState()
    val exams by viewModel.examinations.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val feePayments by viewModel.feePayments.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    // Dialog States
    var showAddTeacherDialog by remember { mutableStateOf(false) }
    var showAddClassDialog by remember { mutableStateOf(false) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showExamDetailsDialog by remember { mutableStateOf<Examination?>(null) }
    var showSendNotificationDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Today's attendance calculation
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayAttendance = allAttendance.filter { it.date == todayStr }
    val presentToday = todayAttendance.sumOf { it.presentCount }
    val absentToday = todayAttendance.sumOf { it.absentCount }

    // Published exams & unpaid fees
    val publishedExamsCount = exams.count { it.status == "PUBLISHED" }
    val unpaidFeesTotal = feePayments.filter { it.status != "PAID" }.sumOf { it.balance }

    Scaffold(
        topBar = {
            SchoolTopBar(
                title = schoolConfig.name,
                subtitle = AppStrings.get("admin_portal", lang),
                currentLang = lang,
                onToggleLang = { viewModel.setLanguage(if (lang == "so") "en" else "so") },
                onSignOut = {
                    viewModel.signOut { onBack() }
                },
                onBack = onBack
            )
        },
        bottomBar = {
            AdminBottomNavigation(
                currentTab = currentTab,
                lang = lang,
                onSelectTab = { viewModel.selectTab(it) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            when (currentTab) {
                "DASHBOARD" -> AdminOverviewContent(
                    lang = lang,
                    studentsCount = students.size,
                    teachersCount = teachers.size,
                    classesCount = classes.size,
                    presentToday = presentToday,
                    absentToday = absentToday,
                    publishedExams = publishedExamsCount,
                    unpaidFees = unpaidFeesTotal,
                    notifications = notifications,
                    onOpenTeachers = { viewModel.selectTab("TEACHERS") },
                    onOpenClasses = { viewModel.selectTab("CLASSES") },
                    onOpenStudents = { viewModel.selectTab("STUDENTS") },
                    onOpenExams = { viewModel.selectTab("EXAMS") },
                    onOpenFees = { viewModel.selectTab("FEES") },
                    onOpenReports = { viewModel.selectTab("REPORTS") },
                    onOpenSettings = { showSettingsDialog = true }
                )
                "TEACHERS" -> AdminTeachersContent(
                    lang = lang,
                    teachers = teachers,
                    classes = classes,
                    onAddTeacher = { showAddTeacherDialog = true },
                    onDeleteTeacher = { viewModel.deleteTeacher(it) }
                )
                "CLASSES" -> AdminClassesContent(
                    lang = lang,
                    classes = classes,
                    teachers = teachers,
                    students = students,
                    onAddClass = { showAddClassDialog = true },
                    onDeleteClass = { viewModel.deleteClass(it) }
                )
                "STUDENTS" -> AdminStudentsContent(
                    lang = lang,
                    students = students,
                    classes = classes,
                    onAddStudent = { showAddStudentDialog = true },
                    onDeleteStudent = { viewModel.deleteStudent(it) }
                )
                "EXAMS" -> AdminExamsContent(
                    lang = lang,
                    exams = exams,
                    viewModel = viewModel,
                    onViewExam = { showExamDetailsDialog = it }
                )
                "FEES" -> AdminFeesContent(
                    lang = lang,
                    feePayments = feePayments,
                    onRecordPayment = { paymentId, amt, rcpt ->
                        viewModel.recordFeePayment(paymentId, amt, rcpt)
                    }
                )
                "REPORTS" -> AdminReportsContent(
                    lang = lang,
                    school = schoolConfig,
                    students = students,
                    teachers = teachers,
                    classes = classes,
                    exams = exams,
                    feePayments = feePayments
                )
                else -> AdminOverviewContent(
                    lang = lang,
                    studentsCount = students.size,
                    teachersCount = teachers.size,
                    classesCount = classes.size,
                    presentToday = presentToday,
                    absentToday = absentToday,
                    publishedExams = publishedExamsCount,
                    unpaidFees = unpaidFeesTotal,
                    notifications = notifications,
                    onOpenTeachers = { viewModel.selectTab("TEACHERS") },
                    onOpenClasses = { viewModel.selectTab("CLASSES") },
                    onOpenStudents = { viewModel.selectTab("STUDENTS") },
                    onOpenExams = { viewModel.selectTab("EXAMS") },
                    onOpenFees = { viewModel.selectTab("FEES") },
                    onOpenReports = { viewModel.selectTab("REPORTS") },
                    onOpenSettings = { showSettingsDialog = true }
                )
            }
        }
    }

    // Add Teacher Dialog
    if (showAddTeacherDialog) {
        AddTeacherDialog(
            lang = lang,
            classes = classes,
            onDismiss = { showAddTeacherDialog = false },
            onConfirm = { name, phone, email, assigned, subjects ->
                viewModel.addTeacher(name, phone, email, assigned, subjects)
                showAddTeacherDialog = false
            }
        )
    }

    // Add Class Dialog
    if (showAddClassDialog) {
        AddClassDialog(
            lang = lang,
            teachers = teachers,
            onDismiss = { showAddClassDialog = false },
            onConfirm = { name, teacherName, subjects, year ->
                viewModel.addClass(name, teacherName, subjects, year)
                showAddClassDialog = false
            }
        )
    }

    // Add Student Dialog
    if (showAddStudentDialog) {
        AddStudentDialog(
            lang = lang,
            classes = classes,
            onDismiss = { showAddStudentDialog = false },
            onConfirm = { fullName, father, mother, phone, gender, classId, className, admDate ->
                viewModel.addStudent(fullName, father, mother, phone, gender, classId, className, admDate)
                showAddStudentDialog = false
            }
        )
    }

    // Exam Details / Publish Control Dialog
    if (showExamDetailsDialog != null) {
        AdminExamDetailsDialog(
            exam = showExamDetailsDialog!!,
            lang = lang,
            viewModel = viewModel,
            onDismiss = { showExamDetailsDialog = null }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        AdminSettingsDialog(
            school = schoolConfig,
            lang = lang,
            onDismiss = { showSettingsDialog = false },
            onSave = { name, address, phone, email, year, passcode ->
                viewModel.updateSchoolSettings(name, address, phone, email, year, passcode)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun AdminBottomNavigation(
    currentTab: String,
    lang: String,
    onSelectTab: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == "DASHBOARD",
            onClick = { onSelectTab("DASHBOARD") },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text(AppStrings.get("dashboard", lang), fontSize = 10.sp) },
            modifier = Modifier.testTag("admin_nav_dashboard")
        )
        NavigationBarItem(
            selected = currentTab == "TEACHERS",
            onClick = { onSelectTab("TEACHERS") },
            icon = { Icon(Icons.Default.People, contentDescription = null) },
            label = { Text(AppStrings.get("teachers", lang), fontSize = 10.sp) },
            modifier = Modifier.testTag("admin_nav_teachers")
        )
        NavigationBarItem(
            selected = currentTab == "CLASSES",
            onClick = { onSelectTab("CLASSES") },
            icon = { Icon(Icons.Default.Class, contentDescription = null) },
            label = { Text(AppStrings.get("classes", lang), fontSize = 10.sp) },
            modifier = Modifier.testTag("admin_nav_classes")
        )
        NavigationBarItem(
            selected = currentTab == "STUDENTS",
            onClick = { onSelectTab("STUDENTS") },
            icon = { Icon(Icons.Default.School, contentDescription = null) },
            label = { Text(AppStrings.get("students", lang), fontSize = 10.sp) },
            modifier = Modifier.testTag("admin_nav_students")
        )
        NavigationBarItem(
            selected = currentTab == "EXAMS",
            onClick = { onSelectTab("EXAMS") },
            icon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null) },
            label = { Text(AppStrings.get("exams", lang), fontSize = 10.sp) },
            modifier = Modifier.testTag("admin_nav_exams")
        )
        NavigationBarItem(
            selected = currentTab == "REPORTS",
            onClick = { onSelectTab("REPORTS") },
            icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
            label = { Text(AppStrings.get("reports", lang), fontSize = 10.sp) },
            modifier = Modifier.testTag("admin_nav_reports")
        )
    }
}

@Composable
fun AdminOverviewContent(
    lang: String,
    studentsCount: Int,
    teachersCount: Int,
    classesCount: Int,
    presentToday: Long,
    absentToday: Long,
    publishedExams: Int,
    unpaidFees: Double,
    notifications: List<SchoolNotification>,
    onOpenTeachers: () -> Unit,
    onOpenClasses: () -> Unit,
    onOpenStudents: () -> Unit,
    onOpenExams: () -> Unit,
    onOpenFees: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Notice banner: Single School System
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyPrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        AppStrings.get("single_school_notice", lang),
                        fontSize = 12.sp,
                        color = NavyPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            Text(
                AppStrings.get("dashboard", lang),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Stats Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = AppStrings.get("total_students", lang),
                        value = "$studentsCount",
                        icon = Icons.Default.School,
                        color = NavyPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = AppStrings.get("total_teachers", lang),
                        value = "$teachersCount",
                        icon = Icons.Default.People,
                        color = BlueSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = AppStrings.get("total_classes", lang),
                        value = "$classesCount",
                        icon = Icons.Default.Class,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = AppStrings.get("published_exams", lang),
                        value = "$publishedExams",
                        icon = Icons.Default.AssignmentTurnedIn,
                        color = GreenAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = AppStrings.get("present_today", lang),
                        value = "$presentToday",
                        icon = Icons.Default.CheckCircle,
                        color = GreenAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = AppStrings.get("absent_today", lang),
                        value = "$absentToday",
                        icon = Icons.Default.Cancel,
                        color = RedDanger,
                        modifier = Modifier.weight(1f)
                    )
                }

                StatCard(
                    title = AppStrings.get("unpaid_fees", lang),
                    value = String.format(Locale.US, "$%.2f", unpaidFees),
                    icon = Icons.Default.AttachMoney,
                    color = AmberWarning
                )
            }
        }

        // Quick Actions
        item {
            Text(
                AppStrings.get("quick_actions", lang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenTeachers,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_action_teachers")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("teachers", lang), fontSize = 12.sp)
                }

                Button(
                    onClick = onOpenClasses,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_action_classes")
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("classes", lang), fontSize = 12.sp)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenExams,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_action_publish_exams")
                ) {
                    Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("publish", lang), fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_action_settings")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("settings", lang), fontSize = 12.sp)
                }
            }
        }

        // Recent Activity Feed
        item {
            Text(
                AppStrings.get("recent_activities", lang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (notifications.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        AppStrings.get("no_data", lang),
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(notifications.take(5)) { notif ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NavyPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(notif.message, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// Teachers Tab
@Composable
fun AdminTeachersContent(
    lang: String,
    teachers: List<Teacher>,
    classes: List<ClassRoom>,
    onAddTeacher: () -> Unit,
    onDeleteTeacher: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                AppStrings.get("teachers", lang),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddTeacher,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("admin_add_teacher_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(AppStrings.get("add", lang))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (teachers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(AppStrings.get("no_data", lang), color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(teachers) { teacher ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(NavyPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    teacher.fullName.take(2).uppercase(Locale.ROOT),
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    teacher.fullName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${teacher.phone} • ${teacher.email.ifBlank { "No email" }}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                if (teacher.subjects.isNotEmpty()) {
                                    Text(
                                        "${AppStrings.get("subjects", lang)}: ${teacher.subjects.joinToString(", ")}",
                                        fontSize = 11.sp,
                                        color = BlueSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteTeacher(teacher.teacherId) },
                                modifier = Modifier.testTag("delete_teacher_${teacher.teacherId}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedDanger)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Classes Tab
@Composable
fun AdminClassesContent(
    lang: String,
    classes: List<ClassRoom>,
    teachers: List<Teacher>,
    students: List<Student>,
    onAddClass: () -> Unit,
    onDeleteClass: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                AppStrings.get("classes", lang),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddClass,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("admin_add_class_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(AppStrings.get("add", lang))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (classes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(AppStrings.get("no_data", lang), color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(classes) { classRoom ->
                    val studentCount = students.count { it.classId == classRoom.classId }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    classRoom.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BlueSurface
                                ) {
                                    Text(
                                        "Code: ${classRoom.accessCode}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BlueSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                "${AppStrings.get("class_teacher", lang)}: ${classRoom.classTeacherName.ifBlank { "Not Assigned" }}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                "${AppStrings.get("total_students", lang)}: $studentCount • ${classRoom.academicYear}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            if (classRoom.subjects.isNotEmpty()) {
                                Text(
                                    "${AppStrings.get("subjects", lang)}: ${classRoom.subjects.joinToString(", ")}",
                                    fontSize = 11.sp,
                                    color = GreenAccent,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { onDeleteClass(classRoom.classId) },
                                    modifier = Modifier.testTag("delete_class_${classRoom.classId}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedDanger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Students Tab
@Composable
fun AdminStudentsContent(
    lang: String,
    students: List<Student>,
    classes: List<ClassRoom>,
    onAddStudent: () -> Unit,
    onDeleteStudent: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf<String?>(null) }

    val filtered = students.filter { s ->
        (selectedClassFilter == null || s.classId == selectedClassFilter) &&
                (searchQuery.isBlank() || s.fullName.contains(searchQuery, ignoreCase = true) || s.studentId.contains(searchQuery, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                AppStrings.get("students", lang),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddStudent,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("admin_add_student_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(AppStrings.get("add", lang))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(AppStrings.get("search", lang)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_student_search"),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Class Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedClassFilter == null,
                    onClick = { selectedClassFilter = null },
                    label = { Text("Dhammaan (All)") }
                )
            }
            items(classes) { c ->
                FilterChip(
                    selected = selectedClassFilter == c.classId,
                    onClick = { selectedClassFilter = c.classId },
                    label = { Text(c.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(AppStrings.get("no_data", lang), color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered) { student ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GreenAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    student.fullName.take(2).uppercase(Locale.ROOT),
                                    fontWeight = FontWeight.Bold,
                                    color = GreenAccent,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    student.fullName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "ID: ${student.studentId} • ${student.className}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFEFCBF)
                                    ) {
                                        Text(
                                            "Linking Code: ${student.linkingCode}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF744210),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onDeleteStudent(student.studentId) },
                                modifier = Modifier.testTag("delete_student_${student.studentId}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedDanger)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Examinations & Publishing Control Tab
@Composable
fun AdminExamsContent(
    lang: String,
    exams: List<Examination>,
    viewModel: MainViewModel,
    onViewExam: (Examination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            AppStrings.get("exams", lang),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (lang == "so") "Guddiye Maamulka: Daabacaadda, Dib-u-eegista & Xiritaanka Imtixaannada" else "Admin Examination Release & Publishing Control Center",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (exams.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(AppStrings.get("no_data", lang), color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(exams) { exam ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewExam(exam) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    exam.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                                StatusBadge(status = exam.status, lang = lang)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                "${AppStrings.get("class", lang)}: ${exam.className} • ${exam.academicYear}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                "${AppStrings.get("subjects", lang)}: ${exam.subjects.size} subjects",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Release Control Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (exam.status != "PUBLISHED") {
                                    Button(
                                        onClick = { viewModel.updateExamStatus(exam.examId, "PUBLISHED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(AppStrings.get("publish", lang), fontSize = 11.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.updateExamStatus(exam.examId, "LOCKED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(AppStrings.get("lock", lang), fontSize = 11.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onViewExam(exam) },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (lang == "so") "Fiiri Natiijada" else "View Marks", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Fees Content
@Composable
fun AdminFeesContent(
    lang: String,
    feePayments: List<FeePayment>,
    onRecordPayment: (String, Double, String) -> Unit
) {
    var showRecordPaymentDialog by remember { mutableStateOf<FeePayment?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            AppStrings.get("fees", lang),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (feePayments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(AppStrings.get("no_data", lang), color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(feePayments) { p ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    p.studentName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusBadge(status = p.status, lang = lang)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "${p.feeName} • ${p.className}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${AppStrings.get("amount", lang)}: $${p.amount}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    "${AppStrings.get("amount_paid", lang)}: $${p.amountPaid}",
                                    fontSize = 12.sp,
                                    color = GreenAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${AppStrings.get("balance", lang)}: $${p.balance}",
                                    fontSize = 12.sp,
                                    color = if (p.balance > 0) RedDanger else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (p.balance > 0) {
                                Button(
                                    onClick = { showRecordPaymentDialog = p },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .padding(top = 6.dp)
                                ) {
                                    Text(AppStrings.get("record_payment", lang), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecordPaymentDialog != null) {
        val p = showRecordPaymentDialog!!
        var amtStr by remember { mutableStateOf(p.balance.toString()) }
        var receiptStr by remember { mutableStateOf("RCP-${System.currentTimeMillis() % 100000}") }

        AlertDialog(
            onDismissRequest = { showRecordPaymentDialog = null },
            title = { Text(AppStrings.get("record_payment", lang)) },
            text = {
                Column {
                    Text("${p.studentName} • ${p.feeName}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amtStr,
                        onValueChange = { amtStr = it },
                        label = { Text(AppStrings.get("amount", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = receiptStr,
                        onValueChange = { receiptStr = it },
                        label = { Text(AppStrings.get("receipt_no", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amtStr.toDoubleOrNull() ?: p.balance
                        onRecordPayment(p.paymentId, amt, receiptStr)
                        showRecordPaymentDialog = null
                    }
                ) {
                    Text(AppStrings.get("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordPaymentDialog = null }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}

// Reports Tab
@Composable
fun AdminReportsContent(
    lang: String,
    school: SchoolConfig,
    students: List<Student>,
    teachers: List<Teacher>,
    classes: List<ClassRoom>,
    exams: List<Examination>,
    feePayments: List<FeePayment>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                AppStrings.get("reports", lang),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${school.name} • ${school.academicYear}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (lang == "so") "Warbixinta Guud ee Dugsiga" else "Executive Summary Report",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ReportMetricRow("Total Enrolled Students", "${students.size}")
                    ReportMetricRow("Total Employed Teachers", "${teachers.size}")
                    ReportMetricRow("Total Active Classes", "${classes.size}")
                    ReportMetricRow("Total Examinations Created", "${exams.size}")
                    val totalFeeBilled = feePayments.sumOf { it.amount }
                    val totalFeeCollected = feePayments.sumOf { it.amountPaid }
                    val totalFeeOutstanding = feePayments.sumOf { it.balance }
                    ReportMetricRow("Total Fees Billed", "$$totalFeeBilled")
                    ReportMetricRow("Total Fees Collected", "$$totalFeeCollected")
                    ReportMetricRow("Total Outstanding Balance", "$$totalFeeOutstanding")
                }
            }
        }
    }
}

@Composable
fun ReportMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// Dialog: Add Teacher
@Composable
fun AddTeacherDialog(
    lang: String,
    classes: List<ClassRoom>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, List<String>, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var subjectsStr by remember { mutableStateOf("") }
    val selectedClasses = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("teachers", lang) + " - " + AppStrings.get("add", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStrings.get("full_name", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(AppStrings.get("phone", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(AppStrings.get("email", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subjectsStr,
                    onValueChange = { subjectsStr = it },
                    label = { Text(AppStrings.get("subjects", lang) + " (Comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val subList = subjectsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onConfirm(name, phone, email, selectedClasses.toList(), subList)
                    }
                }
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.get("cancel", lang)) }
        }
    )
}

// Dialog: Add Class
@Composable
fun AddClassDialog(
    lang: String,
    teachers: List<Teacher>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>, String) -> Unit
) {
    var className by remember { mutableStateOf("") }
    var selectedTeacherName by remember { mutableStateOf("") }
    var subjectsStr by remember { mutableStateOf("Somali, English, Math, Science, Islamic Studies, Social Studies") }
    var academicYear by remember { mutableStateOf("2025-2026") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("classes", lang) + " - " + AppStrings.get("add", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text(AppStrings.get("class_name", lang)) },
                    placeholder = { Text("e.g. Form 1A, Grade 8") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = selectedTeacherName,
                    onValueChange = { selectedTeacherName = it },
                    label = { Text(AppStrings.get("class_teacher", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subjectsStr,
                    onValueChange = { subjectsStr = it },
                    label = { Text(AppStrings.get("subjects", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = academicYear,
                    onValueChange = { academicYear = it },
                    label = { Text(AppStrings.get("academic_year", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (className.isNotBlank()) {
                        val subs = subjectsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onConfirm(className, selectedTeacherName, subs, academicYear)
                    }
                }
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.get("cancel", lang)) }
        }
    )
}

// Dialog: Add Student
@Composable
fun AddStudentDialog(
    lang: String,
    classes: List<ClassRoom>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var father by remember { mutableStateOf("") }
    var mother by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") }
    var selectedClassId by remember { mutableStateOf(classes.firstOrNull()?.classId ?: "") }
    var selectedClassName by remember { mutableStateOf(classes.firstOrNull()?.name ?: "") }
    var admDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("students", lang) + " - " + AppStrings.get("add", lang)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(AppStrings.get("full_name", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = father,
                        onValueChange = { father = it },
                        label = { Text(AppStrings.get("father_name", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = mother,
                        onValueChange = { mother = it },
                        label = { Text(AppStrings.get("mother_name", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(AppStrings.get("parent_phone", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(AppStrings.get("gender", lang), fontSize = 12.sp)
                    Row {
                        FilterChip(
                            selected = gender == "M",
                            onClick = { gender = "M" },
                            label = { Text(AppStrings.get("male", lang)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = gender == "F",
                            onClick = { gender = "F" },
                            label = { Text(AppStrings.get("female", lang)) }
                        )
                    }
                }
                item {
                    Text(AppStrings.get("class", lang), fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(classes) { c ->
                            FilterChip(
                                selected = selectedClassId == c.classId,
                                onClick = {
                                    selectedClassId = c.classId
                                    selectedClassName = c.name
                                },
                                label = { Text(c.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, father, mother, phone, gender, selectedClassId, selectedClassName, admDate)
                    }
                }
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.get("cancel", lang)) }
        }
    )
}

// Dialog: Exam Details & Rankings
@Composable
fun AdminExamDetailsDialog(
    exam: Examination,
    lang: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val resultsState by viewModel.repository.observeExamResults(exam.examId).collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(exam.name, fontWeight = FontWeight.Bold, color = NavyPrimary)
                StatusBadge(status = exam.status, lang = lang)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("${AppStrings.get("class", lang)}: ${exam.className} • ${exam.academicYear}", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    AppStrings.get("competition_rank", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                if (resultsState.isEmpty()) {
                    Text("No results submitted yet for this exam.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(resultsState.sortedBy { it.position }) { res ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = AmberWarning.copy(alpha = 0.2f),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("#${res.position}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AmberWarning)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(res.studentName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Total: ${res.totalObtained}/${res.totalMax}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(String.format(Locale.US, "%.1f%%", res.percentage), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Grade: ${res.grade}", fontSize = 11.sp, color = GreenAccent)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Release Control
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.updateExamStatus(exam.examId, "PUBLISHED")
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(AppStrings.get("publish", lang), fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.updateExamStatus(exam.examId, "LOCKED")
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(AppStrings.get("lock", lang), fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

// Dialog: Settings
@Composable
fun AdminSettingsDialog(
    school: SchoolConfig,
    lang: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(school.name) }
    var address by remember { mutableStateOf(school.address) }
    var phone by remember { mutableStateOf(school.phone) }
    var email by remember { mutableStateOf(school.email) }
    var year by remember { mutableStateOf(school.academicYear) }
    var passcode by remember { mutableStateOf(school.adminPasscode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("settings", lang)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("School Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Academic Year") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { passcode = it },
                        label = { Text("Master Admin Passcode") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, address, phone, email, year, passcode) }
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.get("cancel", lang)) }
        }
    )
}
