package com.example.ui.screens

import android.widget.Toast
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
import com.example.pdf.ReportCardPdfGenerator
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TeacherDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang by viewModel.language.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    val classes by viewModel.classes.collectAsState()
    val selectedClass by viewModel.selectedClass.collectAsState()
    val allStudents by viewModel.students.collectAsState()
    val allExams by viewModel.examinations.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val allFees by viewModel.fees.collectAsState()
    val allFeePayments by viewModel.feePayments.collectAsState()

    // Filter by selected class
    val classStudents = remember(allStudents, selectedClass) {
        if (selectedClass != null) allStudents.filter { it.classId == selectedClass?.classId } else allStudents
    }

    val classExams = remember(allExams, selectedClass) {
        if (selectedClass != null) allExams.filter { it.classId == selectedClass?.classId } else allExams
    }

    // Dialog States
    var showRegisterStudentDialog by remember { mutableStateOf(false) }
    var showCreateExamDialog by remember { mutableStateOf(false) }
    var showEnterMarksExam by remember { mutableStateOf<Examination?>(null) }
    var showReportCardStudent by remember { mutableStateOf<Pair<Student, Examination>?>(null) }
    var showCreateFeeDialog by remember { mutableStateOf(false) }
    var showCreateHomeworkDialog by remember { mutableStateOf(false) }
    var showRemarkDialogStudent by remember { mutableStateOf<Student?>(null) }

    Scaffold(
        topBar = {
            Column {
                SchoolTopBar(
                    title = schoolConfig.name,
                    subtitle = AppStrings.get("teacher_portal", lang),
                    currentLang = lang,
                    onToggleLang = { viewModel.setLanguage(if (lang == "so") "en" else "so") },
                    onSignOut = { viewModel.signOut { onBack() } },
                    onBack = onBack
                )

                // Class Selector Bar
                Surface(
                    color = BlueSurface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${AppStrings.get("class", lang)}: ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NavyPrimary
                        )

                        if (classes.isEmpty()) {
                            Text("No classes created yet", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(classes) { c ->
                                    FilterChip(
                                        selected = selectedClass?.classId == c.classId,
                                        onClick = { viewModel.selectTeacherClass(c) },
                                        label = { Text(c.name, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NavyPrimary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("teacher_class_chip_${c.classId}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            TeacherBottomNavigation(
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
                "DASHBOARD" -> TeacherOverviewContent(
                    lang = lang,
                    selectedClass = selectedClass,
                    studentsCount = classStudents.size,
                    examsCount = classExams.size,
                    onOpenAttendance = { viewModel.selectTab("ATTENDANCE") },
                    onOpenRegisterStudent = { showRegisterStudentDialog = true },
                    onOpenCreateExam = { showCreateExamDialog = true },
                    onOpenHomework = { viewModel.selectTab("HOMEWORK") }
                )
                "STUDENTS" -> TeacherStudentsContent(
                    lang = lang,
                    students = classStudents,
                    selectedClass = selectedClass,
                    onRegisterStudent = { showRegisterStudentDialog = true },
                    onWriteRemark = { showRemarkDialogStudent = it }
                )
                "ATTENDANCE" -> TeacherAttendanceContent(
                    lang = lang,
                    selectedClass = selectedClass,
                    students = classStudents,
                    viewModel = viewModel
                )
                "EXAMS" -> TeacherExamsContent(
                    lang = lang,
                    exams = classExams,
                    students = classStudents,
                    onCreateExam = { showCreateExamDialog = true },
                    onEnterMarks = { showEnterMarksExam = it },
                    onViewReportCard = { student, exam ->
                        showReportCardStudent = Pair(student, exam)
                    }
                )
                "FEES" -> TeacherFeesContent(
                    lang = lang,
                    selectedClass = selectedClass,
                    students = classStudents,
                    feePayments = allFeePayments.filter { it.classId == selectedClass?.classId },
                    onCreateFee = { showCreateFeeDialog = true },
                    onRecordPayment = { paymentId, amt, rcpt ->
                        viewModel.recordFeePayment(paymentId, amt, rcpt)
                    }
                )
                "HOMEWORK" -> TeacherHomeworkContent(
                    lang = lang,
                    selectedClass = selectedClass,
                    viewModel = viewModel,
                    onCreateHomework = { showCreateHomeworkDialog = true }
                )
                else -> TeacherOverviewContent(
                    lang = lang,
                    selectedClass = selectedClass,
                    studentsCount = classStudents.size,
                    examsCount = classExams.size,
                    onOpenAttendance = { viewModel.selectTab("ATTENDANCE") },
                    onOpenRegisterStudent = { showRegisterStudentDialog = true },
                    onOpenCreateExam = { showCreateExamDialog = true },
                    onOpenHomework = { viewModel.selectTab("HOMEWORK") }
                )
            }
        }
    }

    // Register Student Dialog
    if (showRegisterStudentDialog && selectedClass != null) {
        TeacherRegisterStudentDialog(
            lang = lang,
            classRoom = selectedClass!!,
            onDismiss = { showRegisterStudentDialog = false },
            onConfirm = { name, father, mother, phone, gender, admDate ->
                viewModel.addStudent(
                    fullName = name,
                    fatherName = father,
                    motherName = mother,
                    phone = phone,
                    gender = gender,
                    classId = selectedClass!!.classId,
                    className = selectedClass!!.name,
                    admissionDate = admDate
                )
                showRegisterStudentDialog = false
            }
        )
    }

    // Create Exam Dialog
    if (showCreateExamDialog && selectedClass != null) {
        TeacherCreateExamDialog(
            lang = lang,
            classRoom = selectedClass!!,
            onDismiss = { showCreateExamDialog = false },
            onConfirm = { name, type, subs, start, end, desc ->
                viewModel.createExamination(
                    name = name,
                    type = type,
                    classId = selectedClass!!.classId,
                    className = selectedClass!!.name,
                    subjects = subs,
                    startDate = start,
                    endDate = end,
                    desc = desc
                )
                showCreateExamDialog = false
            }
        )
    }

    // Enter Marks Dialog
    if (showEnterMarksExam != null) {
        TeacherEnterMarksDialog(
            exam = showEnterMarksExam!!,
            students = classStudents,
            lang = lang,
            viewModel = viewModel,
            onDismiss = { showEnterMarksExam = null }
        )
    }

    // Report Card Dialog (with PDF Preview & Share)
    if (showReportCardStudent != null) {
        val (student, exam) = showReportCardStudent!!
        TeacherReportCardDialog(
            student = student,
            exam = exam,
            school = schoolConfig,
            lang = lang,
            viewModel = viewModel,
            onDismiss = { showReportCardStudent = null }
        )
    }

    // Create Class Fee Dialog
    if (showCreateFeeDialog && selectedClass != null) {
        TeacherCreateFeeDialog(
            lang = lang,
            classRoom = selectedClass!!,
            onDismiss = { showCreateFeeDialog = false },
            onConfirm = { name, amount, dueDate, desc ->
                viewModel.addFee(
                    feeName = name,
                    classId = selectedClass!!.classId,
                    className = selectedClass!!.name,
                    amount = amount,
                    dueDate = dueDate,
                    desc = desc
                )
                showCreateFeeDialog = false
            }
        )
    }

    // Create Homework Dialog
    if (showCreateHomeworkDialog && selectedClass != null) {
        TeacherCreateHomeworkDialog(
            lang = lang,
            classRoom = selectedClass!!,
            onDismiss = { showCreateHomeworkDialog = false },
            onConfirm = { subject, title, desc, due ->
                viewModel.addHomework(
                    classId = selectedClass!!.classId,
                    className = selectedClass!!.name,
                    subject = subject,
                    title = title,
                    desc = desc,
                    due = due
                )
                showCreateHomeworkDialog = false
            }
        )
    }

    // Write Remark Dialog
    if (showRemarkDialogStudent != null) {
        val st = showRemarkDialogStudent!!
        TeacherWriteRemarkDialog(
            student = st,
            lang = lang,
            onDismiss = { showRemarkDialogStudent = null },
            onConfirm = { cat, msg ->
                viewModel.addRemark(st.studentId, st.fullName, st.classId, cat, msg)
                showRemarkDialogStudent = null
            }
        )
    }
}

@Composable
fun TeacherBottomNavigation(
    currentTab: String,
    lang: String,
    onSelectTab: (String) -> Unit
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        NavigationBarItem(
            selected = currentTab == "DASHBOARD",
            onClick = { onSelectTab("DASHBOARD") },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text(AppStrings.get("dashboard", lang), fontSize = 10.sp) },
            modifier = Modifier.testTag("teacher_nav_dashboard")
        )
        NavigationBarItem(
            selected = currentTab == "STUDENTS",
            onClick = { onSelectTab("STUDENTS") },
            icon = { Icon(Icons.Default.School, contentDescription = null) },
            label = { Text(AppStrings.get("students", lang), fontSize = 10.sp) },
            modifier = Modifier.testTag("teacher_nav_students")
        )
        NavigationBarItem(
            selected = currentTab == "ATTENDANCE",
            onClick = { onSelectTab("ATTENDANCE") },
            icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
            label = { Text(AppStrings.get("attendance", lang), fontSize = 10.sp) },
            modifier = Modifier.testTag("teacher_nav_attendance")
        )
        NavigationBarItem(
            selected = currentTab == "EXAMS",
            onClick = { onSelectTab("EXAMS") },
            icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
            label = { Text(AppStrings.get("exams", lang), fontSize = 10.sp) },
            modifier = Modifier.testTag("teacher_nav_exams")
        )
        NavigationBarItem(
            selected = currentTab == "FEES",
            onClick = { onSelectTab("FEES") },
            icon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
            label = { Text(AppStrings.get("fees", lang), fontSize = 10.sp) },
            modifier = Modifier.testTag("teacher_nav_fees")
        )
        NavigationBarItem(
            selected = currentTab == "HOMEWORK",
            onClick = { onSelectTab("HOMEWORK") },
            icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
            label = { Text(AppStrings.get("homework", lang), fontSize = 10.sp) },
            modifier = Modifier.testTag("teacher_nav_homework")
        )
    }
}

// Teacher Overview
@Composable
fun TeacherOverviewContent(
    lang: String,
    selectedClass: ClassRoom?,
    studentsCount: Int,
    examsCount: Int,
    onOpenAttendance: () -> Unit,
    onOpenRegisterStudent: () -> Unit,
    onOpenCreateExam: () -> Unit,
    onOpenHomework: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = selectedClass?.name ?: "No Class Selected",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Academic Year: ${selectedClass?.academicYear ?: "2025-2026"}",
                        fontSize = 12.sp,
                        color = BlueLight
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$studentsCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(AppStrings.get("total_students", lang), fontSize = 11.sp, color = BlueLight)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$examsCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                            Text(AppStrings.get("exams", lang), fontSize = 11.sp, color = BlueLight)
                        }
                    }
                }
            }
        }

        item {
            Text(
                AppStrings.get("quick_actions", lang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenAttendance,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("attendance", lang), fontSize = 12.sp)
                }

                Button(
                    onClick = onOpenRegisterStudent,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("add", lang), fontSize = 12.sp)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenCreateExam,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (lang == "so") "Abuur Imtixaan" else "Create Exam", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenHomework,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("homework", lang), fontSize = 12.sp)
                }
            }
        }
    }
}

// Teacher Students List
@Composable
fun TeacherStudentsContent(
    lang: String,
    students: List<Student>,
    selectedClass: ClassRoom?,
    onRegisterStudent: () -> Unit,
    onWriteRemark: (Student) -> Unit
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
                "${AppStrings.get("students", lang)} (${students.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onRegisterStudent,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(AppStrings.get("add", lang))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(AppStrings.get("no_data", lang), color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(students) { student ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(BlueSecondary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        student.fullName.take(2).uppercase(Locale.ROOT),
                                        fontWeight = FontWeight.Bold,
                                        color = BlueSecondary,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("ID: ${student.studentId} • Parent: ${student.parentPhone}", fontSize = 12.sp, color = Color.Gray)
                                }

                                Button(
                                    onClick = { onWriteRemark(student) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppStrings.get("remarks", lang), fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

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
                }
            }
        }
    }
}

// Attendance Screen
@Composable
fun TeacherAttendanceContent(
    lang: String,
    selectedClass: ClassRoom?,
    students: List<Student>,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    val attendanceMap = remember { mutableStateMapOf<String, String>() }

    // Initialize or load attendance
    LaunchedEffect(selectedClass, selectedDate) {
        if (selectedClass != null) {
            val existing = viewModel.repository.getAttendance(selectedClass.classId, selectedDate)
            attendanceMap.clear()
            if (existing != null) {
                existing.records.forEach { attendanceMap[it.studentId] = it.status }
            } else {
                students.forEach { attendanceMap[it.studentId] = "PRESENT" }
            }
        }
    }

    val presentCount = attendanceMap.values.count { it == "PRESENT" }
    val absentCount = attendanceMap.values.count { it == "ABSENT" }
    val lateCount = attendanceMap.values.count { it == "LATE" }
    val excusedCount = attendanceMap.values.count { it == "EXCUSED" }
    val totalCount = students.size
    val rate = if (totalCount > 0) (presentCount.toDouble() / totalCount * 100) else 0.0

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
            Column {
                Text(
                    AppStrings.get("attendance", lang),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Date: $selectedDate", fontSize = 12.sp, color = Color.Gray)
            }

            Button(
                onClick = {
                    students.forEach { attendanceMap[it.studentId] = "PRESENT" }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("mark_all_present_button")
            ) {
                Text(AppStrings.get("mark_all_present", lang), fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Summary Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text("Jooga: $presentCount", color = GreenAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Maqan: $absentCount", color = RedDanger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Daahay: $lateCount", color = AmberWarning, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Cudurdaar: $excusedCount", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(String.format(Locale.US, "%.0f%%", rate), color = NavyPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(students) { student ->
                val currentStatus = attendanceMap[student.studentId] ?: "PRESENT"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(student.studentId, fontSize = 11.sp, color = Color.Gray)
                        }

                        // Status Selector Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            AttendanceStatusButton("P", "PRESENT", currentStatus == "PRESENT", GreenAccent) {
                                attendanceMap[student.studentId] = "PRESENT"
                            }
                            AttendanceStatusButton("A", "ABSENT", currentStatus == "ABSENT", RedDanger) {
                                attendanceMap[student.studentId] = "ABSENT"
                            }
                            AttendanceStatusButton("L", "LATE", currentStatus == "LATE", AmberWarning) {
                                attendanceMap[student.studentId] = "LATE"
                            }
                            AttendanceStatusButton("E", "EXCUSED", currentStatus == "EXCUSED", Color.Gray) {
                                attendanceMap[student.studentId] = "EXCUSED"
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                if (selectedClass != null) {
                    val records = students.map { s ->
                        AttendanceRecord(
                            studentId = s.studentId,
                            studentName = s.fullName,
                            status = attendanceMap[s.studentId] ?: "PRESENT"
                        )
                    }
                    viewModel.markAttendance(selectedClass.classId, selectedClass.name, selectedDate, records)
                    Toast.makeText(context, AppStrings.get("success_saved", lang), Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_attendance_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(AppStrings.get("save", lang), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AttendanceStatusButton(
    shortLabel: String,
    status: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) color else Color(0xFFEDF2F7),
        modifier = Modifier
            .size(34.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                shortLabel,
                color = if (isSelected) Color.White else Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

// Teacher Exams Tab
@Composable
fun TeacherExamsContent(
    lang: String,
    exams: List<Examination>,
    students: List<Student>,
    onCreateExam: () -> Unit,
    onEnterMarks: (Examination) -> Unit,
    onViewReportCard: (Student, Examination) -> Unit
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
                AppStrings.get("exams", lang),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onCreateExam,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(AppStrings.get("add", lang))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (exams.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(AppStrings.get("no_data", lang), color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(exams) { exam ->
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
                                Text(exam.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyPrimary)
                                StatusBadge(status = exam.status, lang = lang)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "Type: ${exam.examType} • ${exam.subjects.size} subjects",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onEnterMarks(exam) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppStrings.get("enter_marks", lang), fontSize = 11.sp)
                                }

                                if (students.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { onViewReportCard(students.first(), exam) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(AppStrings.get("view_report_card", lang), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Teacher Fees Content
@Composable
fun TeacherFeesContent(
    lang: String,
    selectedClass: ClassRoom?,
    students: List<Student>,
    feePayments: List<FeePayment>,
    onCreateFee: () -> Unit,
    onRecordPayment: (String, Double, String) -> Unit
) {
    var showRecordPaymentDialog by remember { mutableStateOf<FeePayment?>(null) }

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
                AppStrings.get("fees", lang),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onCreateFee,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (lang == "so") "Fi cusub" else "New Fee")
            }
        }

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
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                StatusBadge(status = p.status, lang = lang)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${p.feeName} • Total: $${p.amount} • Paid: $${p.amountPaid} • Bal: $${p.balance}", fontSize = 12.sp, color = Color.Gray)

                            if (p.balance > 0) {
                                Button(
                                    onClick = { showRecordPaymentDialog = p },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
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
                    Spacer(modifier = Modifier.height(8.dp))
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
                TextButton(onClick = { showRecordPaymentDialog = null }) { Text(AppStrings.get("cancel", lang)) }
            }
        )
    }
}

// Teacher Homework Tab
@Composable
fun TeacherHomeworkContent(
    lang: String,
    selectedClass: ClassRoom?,
    viewModel: MainViewModel,
    onCreateHomework: () -> Unit
) {
    val homeworkList by viewModel.repository.observeHomework(selectedClass?.classId ?: "").collectAsState(initial = emptyList())

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
                AppStrings.get("homework", lang),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onCreateHomework,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(AppStrings.get("add", lang))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (homeworkList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(AppStrings.get("no_data", lang), color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(homeworkList) { hw ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(hw.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BlueSurface
                                ) {
                                    Text(hw.subject, color = BlueSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(hw.description, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Due Date: ${hw.dueDate}", fontSize = 11.sp, color = RedDanger, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// Dialog: Register Student in Class
@Composable
fun TeacherRegisterStudentDialog(
    lang: String,
    classRoom: ClassRoom,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var father by remember { mutableStateOf("") }
    var mother by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") }
    var admDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("students", lang) + " - " + classRoom.name) },
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
                        FilterChip(selected = gender == "M", onClick = { gender = "M" }, label = { Text(AppStrings.get("male", lang)) })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = gender == "F", onClick = { gender = "F" }, label = { Text(AppStrings.get("female", lang)) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, father, mother, phone, gender, admDate)
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

// Dialog: Create Exam
@Composable
fun TeacherCreateExamDialog(
    lang: String,
    classRoom: ClassRoom,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<ExamSubject>, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("MONTHLY") }
    var subjectsStr by remember { mutableStateOf(classRoom.subjects.joinToString(", ")) }
    var startDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var endDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (lang == "so") "Abuur Imtixaan Cusub" else "Create Examination") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(AppStrings.get("exam_name", lang)) },
                        placeholder = { Text("e.g. Midterm Term 1") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(AppStrings.get("exam_type", lang), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = type == "MONTHLY", onClick = { type = "MONTHLY" }, label = { Text("Monthly") })
                        FilterChip(selected = type == "MIDTERM", onClick = { type = "MIDTERM" }, label = { Text("Midterm") })
                        FilterChip(selected = type == "FINAL", onClick = { type = "FINAL" }, label = { Text("Final") })
                    }
                }
                item {
                    OutlinedTextField(
                        value = subjectsStr,
                        onValueChange = { subjectsStr = it },
                        label = { Text(AppStrings.get("subjects", lang) + " (Comma separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(AppStrings.get("description", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val subs = subjectsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            .map { ExamSubject(name = it, maxMarks = 100.0) }
                        onConfirm(name, type, subs, startDate, endDate, desc)
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

// Dialog: Enter Student Marks with Competition Ranking
@Composable
fun TeacherEnterMarksDialog(
    exam: Examination,
    students: List<Student>,
    lang: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // Map of studentId -> (subjectName -> mark)
    val marksMap = remember { mutableStateMapOf<String, MutableMap<String, Double>>() }

    // Initialize with existing results or zeroes
    val existingResults by viewModel.repository.observeExamResults(exam.examId).collectAsState(initial = emptyList())

    LaunchedEffect(existingResults) {
        existingResults.forEach { res ->
            marksMap[res.studentId] = res.subjectMarks.toMutableMap()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(exam.name, fontWeight = FontWeight.Bold, color = NavyPrimary)
                Text("Enter Marks for Class: ${exam.className}", fontSize = 12.sp, color = Color.Gray)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(students) { student ->
                    val studentMarks = marksMap.getOrPut(student.studentId) { mutableMapOf() }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("ID: ${student.studentId}", fontSize = 11.sp, color = Color.Gray)

                            Spacer(modifier = Modifier.height(6.dp))

                            exam.subjects.forEach { sub ->
                                var markStr by remember { mutableStateOf(studentMarks[sub.name]?.toString() ?: "") }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${sub.name} (Max ${sub.maxMarks})", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    OutlinedTextField(
                                        value = markStr,
                                        onValueChange = {
                                            markStr = it
                                            val num = it.toDoubleOrNull() ?: 0.0
                                            studentMarks[sub.name] = num.coerceIn(0.0, sub.maxMarks)
                                        },
                                        modifier = Modifier.width(80.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resultsList = students.map { s ->
                        val subMarks = marksMap[s.studentId] ?: emptyMap()
                        val totalObt = subMarks.values.sum()
                        val totalMax = exam.subjects.sumOf { it.maxMarks }
                        val pct = if (totalMax > 0) (totalObt / totalMax * 100) else 0.0
                        val avg = if (exam.subjects.isNotEmpty()) totalObt / exam.subjects.size else 0.0

                        ExamResult(
                            examId = exam.examId,
                            examName = exam.name,
                            studentId = s.studentId,
                            studentName = s.fullName,
                            classId = s.classId,
                            className = s.className,
                            subjectMarks = subMarks,
                            totalObtained = totalObt,
                            totalMax = totalMax,
                            percentage = pct,
                            average = avg,
                            teacherRemarks = "Good effort and performance."
                        )
                    }
                    viewModel.submitExamResults(exam, resultsList)
                    Toast.makeText(context, AppStrings.get("success_saved", lang), Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text(AppStrings.get("submit", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.get("cancel", lang)) }
        }
    )
}

// Dialog: Report Card Preview & Download PDF
@Composable
fun TeacherReportCardDialog(
    student: Student,
    exam: Examination,
    school: SchoolConfig,
    lang: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val results by viewModel.repository.observeExamResults(exam.examId).collectAsState(initial = emptyList())
    val studentResult = results.firstOrNull { it.studentId == student.studentId } ?: ExamResult(
        examId = exam.examId,
        studentId = student.studentId,
        studentName = student.fullName,
        classId = student.classId,
        className = student.className,
        totalObtained = 85.0,
        totalMax = 100.0,
        percentage = 85.0,
        grade = "A",
        position = 1L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(AppStrings.get("view_report_card", lang), fontWeight = FontWeight.Bold, color = NavyPrimary)
                StatusBadge(status = exam.status, lang = lang)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("${student.fullName} • ${student.studentId}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${exam.name} • ${exam.className} • ${exam.academicYear}", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Marks:", fontSize = 13.sp)
                            Text("${studentResult.totalObtained} / ${studentResult.totalMax}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Percentage:", fontSize = 13.sp)
                            Text(String.format(Locale.US, "%.1f%%", studentResult.percentage), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grade:", fontSize = 13.sp)
                            Text(studentResult.grade, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GreenAccent)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Class Rank (Competition):", fontSize = 13.sp)
                            Text("#${studentResult.position}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AmberWarning)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val pdfFile = ReportCardPdfGenerator.generateReportCard(
                            context = context,
                            school = school,
                            student = student,
                            exam = exam,
                            result = studentResult,
                            language = lang
                        )
                        if (pdfFile != null) {
                            ReportCardPdfGenerator.shareReportCardPdf(context, pdfFile)
                        } else {
                            Toast.makeText(context, "Error generating PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.get("download_pdf", lang))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

// Dialog: Create Class Fee
@Composable
fun TeacherCreateFeeDialog(
    lang: String,
    classRoom: ClassRoom,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit
) {
    var feeName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("20.0") }
    var dueDate by remember { mutableStateOf("2026-12-05") }
    var desc by remember { mutableStateOf("Monthly school tuition fee") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Class Fee - " + classRoom.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = feeName,
                    onValueChange = { feeName = it },
                    label = { Text(AppStrings.get("fee_name", lang)) },
                    placeholder = { Text("e.g. November School Tuition") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(AppStrings.get("amount", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text(AppStrings.get("due_date", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (feeName.isNotBlank() && amt > 0) {
                        onConfirm(feeName, amt, dueDate, desc)
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

// Dialog: Create Homework
@Composable
fun TeacherCreateHomeworkDialog(
    lang: String,
    classRoom: ClassRoom,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var subject by remember { mutableStateOf(classRoom.subjects.firstOrNull() ?: "Math") }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("homework", lang) + " - " + classRoom.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(AppStrings.get("subjects", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(AppStrings.get("title", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(AppStrings.get("description", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text(AppStrings.get("due_date", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onConfirm(subject, title, desc, dueDate)
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

// Dialog: Write Student Remark
@Composable
fun TeacherWriteRemarkDialog(
    student: Student,
    lang: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var category by remember { mutableStateOf("GOOD_PERFORMANCE") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teacher Remark - " + student.fullName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Category:", fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item { FilterChip(selected = category == "GOOD_PERFORMANCE", onClick = { category = "GOOD_PERFORMANCE" }, label = { Text("Good Performance") }) }
                    item { FilterChip(selected = category == "EXCELLENT_BEHAVIOR", onClick = { category = "EXCELLENT_BEHAVIOR" }, label = { Text("Excellent Behavior") }) }
                    item { FilterChip(selected = category == "NEEDS_IMPROVEMENT", onClick = { category = "NEEDS_IMPROVEMENT" }, label = { Text("Needs Improvement") }) }
                    item { FilterChip(selected = category == "HOMEWORK_PROBLEM", onClick = { category = "HOMEWORK_PROBLEM" }, label = { Text("Homework Issue") }) }
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(AppStrings.get("message", lang)) },
                    placeholder = { Text("Enter detailed feedback for parent...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (message.isNotBlank()) onConfirm(category, message)
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
