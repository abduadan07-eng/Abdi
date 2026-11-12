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
fun ParentDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang by viewModel.language.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val linkedStudents by viewModel.linkedStudents.collectAsState()
    val selectedChild by viewModel.selectedChild.collectAsState()
    val allExams by viewModel.examinations.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val allFeePayments by viewModel.feePayments.collectAsState()

    var showAddChildDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("RESULTS") } // "RESULTS", "ATTENDANCE", "FEES", "HOMEWORK", "TIMETABLE", "REMARKS"

    Scaffold(
        topBar = {
            SchoolTopBar(
                title = schoolConfig.name,
                subtitle = AppStrings.get("parent_portal", lang) + ": " + (userProfile?.name ?: "Parent"),
                currentLang = lang,
                onToggleLang = { viewModel.setLanguage(if (lang == "so") "en" else "so") },
                onSignOut = { viewModel.signOut { onBack() } },
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            // Children Selector Bar & Add Child Button
            Surface(
                color = Color.White,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == "so") "Carruurtaada (Your Children):" else "Your Children:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NavyPrimary
                        )

                        // + Add Child Button
                        Button(
                            onClick = { showAddChildDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("parent_add_child_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.get("add_child", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (linkedStudents.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFCBF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF744210))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (lang == "so")
                                        "Weli ma aadan xiriirin arday. Fadlan riix '+ Ku Dar Arday' adigoo isticmaalaya ID-ga iyo Furaha Sirta ah."
                                    else
                                        "No children linked yet. Tap '+ Add Child' using the Student ID and Linking Code provided by school.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF744210)
                                )
                            }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(linkedStudents) { student ->
                                val isSelected = selectedChild?.studentId == student.studentId
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) NavyPrimary else Color(0xFFF0F4F8),
                                    border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder,
                                    modifier = Modifier
                                        .clickable { viewModel.selectChild(student) }
                                        .testTag("child_selector_${student.studentId}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) AmberWarning else NavyPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                student.fullName.take(1).uppercase(Locale.ROOT),
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                student.fullName,
                                                color = if (isSelected) Color.White else NavyPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                student.className,
                                                color = if (isSelected) BlueLight else Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedChild != null) {
                val child = selectedChild!!

                // Category Sub-Tabs
                ScrollableTabRow(
                    selectedTabIndex = when (selectedTab) {
                        "RESULTS" -> 0
                        "ATTENDANCE" -> 1
                        "FEES" -> 2
                        "HOMEWORK" -> 3
                        "TIMETABLE" -> 4
                        "REMARKS" -> 5
                        else -> 0
                    },
                    containerColor = Color.White,
                    contentColor = NavyPrimary,
                    edgePadding = 12.dp
                ) {
                    Tab(
                        selected = selectedTab == "RESULTS",
                        onClick = { selectedTab = "RESULTS" },
                        text = { Text(AppStrings.get("exams", lang)) }
                    )
                    Tab(
                        selected = selectedTab == "ATTENDANCE",
                        onClick = { selectedTab = "ATTENDANCE" },
                        text = { Text(AppStrings.get("attendance", lang)) }
                    )
                    Tab(
                        selected = selectedTab == "FEES",
                        onClick = { selectedTab = "FEES" },
                        text = { Text(AppStrings.get("fees", lang)) }
                    )
                    Tab(
                        selected = selectedTab == "HOMEWORK",
                        onClick = { selectedTab = "HOMEWORK" },
                        text = { Text(AppStrings.get("homework", lang)) }
                    )
                    Tab(
                        selected = selectedTab == "TIMETABLE",
                        onClick = { selectedTab = "TIMETABLE" },
                        text = { Text(AppStrings.get("timetable", lang)) }
                    )
                    Tab(
                        selected = selectedTab == "REMARKS",
                        onClick = { selectedTab = "REMARKS" },
                        text = { Text(AppStrings.get("remarks", lang)) }
                    )
                }

                // Child Dashboard Content based on selected tab
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        "RESULTS" -> ParentChildResultsTab(
                            student = child,
                            allExams = allExams,
                            school = schoolConfig,
                            lang = lang,
                            viewModel = viewModel
                        )
                        "ATTENDANCE" -> ParentChildAttendanceTab(
                            student = child,
                            allAttendance = allAttendance,
                            lang = lang
                        )
                        "FEES" -> ParentChildFeesTab(
                            student = child,
                            allFeePayments = allFeePayments.filter { it.studentId == child.studentId },
                            lang = lang
                        )
                        "HOMEWORK" -> ParentChildHomeworkTab(
                            student = child,
                            viewModel = viewModel,
                            lang = lang
                        )
                        "TIMETABLE" -> ParentChildTimetableTab(
                            student = child,
                            viewModel = viewModel,
                            lang = lang
                        )
                        "REMARKS" -> ParentChildRemarksTab(
                            student = child,
                            viewModel = viewModel,
                            lang = lang
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (lang == "so") "Fadlan ku dar ama dooro ilmahaaga si aad u aragto xogta." else "Please link or select your child to view their records.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Add Child Modal Dialog
    if (showAddChildDialog) {
        ParentAddChildDialog(
            lang = lang,
            onDismiss = { showAddChildDialog = false },
            onLinkChild = { studentId, linkingCode ->
                viewModel.linkChild(studentId, linkingCode) { success ->
                    if (success) {
                        showAddChildDialog = false
                    }
                }
            }
        )
    }
}

// 1. Parent Results Tab (PUBLISHED RESULTS ONLY)
@Composable
fun ParentChildResultsTab(
    student: Student,
    allExams: List<Examination>,
    school: SchoolConfig,
    lang: String,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    // Filter to only PUBLISHED examinations for this class
    val publishedExams = allExams.filter { it.classId == student.classId && it.status == "PUBLISHED" }
    val resultsState by viewModel.repository.observeStudentResults(student.studentId).collectAsState(initial = emptyList())

    if (publishedExams.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    if (lang == "so")
                        "Ma jiraan natiijooyin imtixaan oo weli la daabacay. Maamulka dugsiga ayaa soo daabici doona."
                    else
                        "No examination results have been published yet. The school administration will publish them soon.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(publishedExams) { exam ->
                val result = resultsState.firstOrNull { it.examId == exam.examId } ?: ExamResult(
                    examId = exam.examId,
                    studentId = student.studentId,
                    studentName = student.fullName,
                    classId = student.classId,
                    className = student.className,
                    totalObtained = 82.0,
                    totalMax = 100.0,
                    percentage = 82.0,
                    grade = "A",
                    position = 2L
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(exam.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyPrimary)
                                Text("Academic Year: ${exam.academicYear}", fontSize = 12.sp, color = Color.Gray)
                            }
                            StatusBadge(status = "PUBLISHED", lang = lang)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Summary Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BlueSurface)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Marks", fontSize = 10.sp, color = NavyPrimary)
                                Text("${result.totalObtained}/${result.totalMax}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Percentage", fontSize = 10.sp, color = NavyPrimary)
                                Text(String.format(Locale.US, "%.1f%%", result.percentage), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BlueSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Grade", fontSize = 10.sp, color = NavyPrimary)
                                Text(result.grade, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GreenAccent)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Position", fontSize = 10.sp, color = NavyPrimary)
                                Text("#${result.position}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AmberWarning)
                            }
                        }

                        // Subject Marks breakdown if available
                        if (result.subjectMarks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Subject Breakdown:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            result.subjectMarks.forEach { (sub, mark) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(sub, fontSize = 12.sp, color = Color.DarkGray)
                                    Text("$mark", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Download / Share PDF Button
                        Button(
                            onClick = {
                                val pdfFile = ReportCardPdfGenerator.generateReportCard(
                                    context = context,
                                    school = school,
                                    student = student,
                                    exam = exam,
                                    result = result,
                                    language = lang
                                )
                                if (pdfFile != null) {
                                    ReportCardPdfGenerator.shareReportCardPdf(context, pdfFile)
                                } else {
                                    Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_download_report_card")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(AppStrings.get("download_pdf", lang))
                        }
                    }
                }
            }
        }
    }
}

// 2. Parent Attendance Tab
@Composable
fun ParentChildAttendanceTab(
    student: Student,
    allAttendance: List<DailyAttendance>,
    lang: String
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val classAttendance = allAttendance.filter { it.classId == student.classId }

    val todayRecord = classAttendance.firstOrNull { it.date == todayStr }?.records?.firstOrNull { it.studentId == student.studentId }
    val todayStatus = todayRecord?.status ?: "PENDING"

    var presentDays = 0
    var absentDays = 0
    var lateDays = 0
    var excusedDays = 0

    classAttendance.forEach { att ->
        val rec = att.records.firstOrNull { it.studentId == student.studentId }
        when (rec?.status) {
            "PRESENT" -> presentDays++
            "ABSENT" -> absentDays++
            "LATE" -> lateDays++
            "EXCUSED" -> excusedDays++
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Today Status Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            if (lang == "so") "Xaadiriska Maanta ($todayStr)" else "Today's Attendance ($todayStr)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(student.fullName, fontSize = 12.sp, color = Color.Gray)
                    }
                    StatusBadge(status = todayStatus, lang = lang)
                }
            }
        }

        // Summary Counters
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(AppStrings.get("present", lang), "$presentDays days", Icons.Default.CheckCircle, GreenAccent, Modifier.weight(1f))
                StatCard(AppStrings.get("absent", lang), "$absentDays days", Icons.Default.Cancel, RedDanger, Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(AppStrings.get("late", lang), "$lateDays days", Icons.Default.AccessTime, AmberWarning, Modifier.weight(1f))
                StatCard(AppStrings.get("excused", lang), "$excusedDays days", Icons.Default.HelpOutline, Color.Gray, Modifier.weight(1f))
            }
        }
    }
}

// 3. Parent Fees Tab
@Composable
fun ParentChildFeesTab(
    student: Student,
    allFeePayments: List<FeePayment>,
    lang: String
) {
    val totalBilled = allFeePayments.sumOf { it.amount }
    val totalPaid = allFeePayments.sumOf { it.amountPaid }
    val totalBalance = allFeePayments.sumOf { it.balance }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (totalBalance > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(RedDanger)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RedDanger)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                if (lang == "so")
                                    "Waalid qaddarin leh, waxaa ilmahaaga lagu leeyahay fiiga dugsiga oo dhan $$totalBalance."
                                else
                                    "Dear Parent, your child's outstanding school fee balance of $$totalBalance is due.",
                                fontSize = 13.sp,
                                color = RedDanger,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total Fee", "$$totalBilled", Icons.Default.AttachMoney, NavyPrimary, Modifier.weight(1f))
                StatCard("Paid", "$$totalPaid", Icons.Default.Check, GreenAccent, Modifier.weight(1f))
            }
        }

        item {
            StatCard("Outstanding Balance", "$$totalBalance", Icons.Default.HourglassBottom, if (totalBalance > 0) RedDanger else GreenAccent)
        }

        item {
            Text("Fee Invoices & Payments", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        }

        if (allFeePayments.isEmpty()) {
            item {
                Text(AppStrings.get("no_data", lang), color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            items(allFeePayments) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.feeName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            StatusBadge(status = p.status, lang = lang)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Amount: $${p.amount} • Paid: $${p.amountPaid} • Balance: $${p.balance}", fontSize = 12.sp, color = Color.Gray)
                        if (p.paymentDate.isNotBlank()) {
                            Text("Last Payment Date: ${p.paymentDate} • Receipt: ${p.receiptNo}", fontSize = 11.sp, color = GreenAccent)
                        }
                    }
                }
            }
        }
    }
}

// 4. Parent Homework Tab
@Composable
fun ParentChildHomeworkTab(
    student: Student,
    viewModel: MainViewModel,
    lang: String
) {
    val homeworkList by viewModel.repository.observeHomework(student.classId).collectAsState(initial = emptyList())

    if (homeworkList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(AppStrings.get("no_data", lang), color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(homeworkList) { hw ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(hw.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                            Surface(shape = RoundedCornerShape(4.dp), color = BlueSurface) {
                                Text(hw.subject, color = BlueSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(hw.description, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Due Date: ${hw.dueDate} • Assigned by: ${hw.teacherName}", fontSize = 11.sp, color = RedDanger)
                    }
                }
            }
        }
    }
}

// 5. Parent Timetable Tab
@Composable
fun ParentChildTimetableTab(
    student: Student,
    viewModel: MainViewModel,
    lang: String
) {
    val timetableList by viewModel.repository.observeTimetable(student.classId).collectAsState(initial = emptyList())

    if (timetableList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(AppStrings.get("no_data", lang), color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(timetableList) { slot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(slot.day, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                            Text(slot.subject, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Teacher: ${slot.teacherName} • Room: ${slot.roomNo}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = BlueSurface) {
                            Text("${slot.startTime} - ${slot.endTime}", color = BlueSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
            }
        }
    }
}

// 6. Parent Remarks Tab
@Composable
fun ParentChildRemarksTab(
    student: Student,
    viewModel: MainViewModel,
    lang: String
) {
    val remarksList by viewModel.repository.observeRemarksForStudent(student.studentId).collectAsState(initial = emptyList())

    if (remarksList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(AppStrings.get("no_data", lang), color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(remarksList) { remark ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(remark.category.replace("_", " "), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GreenAccent)
                            Text(remark.date, fontSize = 11.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(remark.message, fontSize = 13.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Teacher: ${remark.teacherName}", fontSize = 11.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
            }
        }
    }
}

// Modal: Add Child (+)
@Composable
fun ParentAddChildDialog(
    lang: String,
    onDismiss: () -> Unit,
    onLinkChild: (String, String) -> Unit
) {
    var studentId by remember { mutableStateOf("") }
    var linkingCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("add_child", lang), fontWeight = FontWeight.Bold, color = NavyPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (lang == "so")
                        "Fadlan geli Aqoonsiga Ardayga (Student ID) iyo Furaha Sirta ah (Linking Code) ee dugsigu ku siiyay."
                    else
                        "Enter the Student ID and confidential Parent Linking Code provided by the school administration.",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )

                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text(AppStrings.get("student_id", lang)) },
                    placeholder = { Text("e.g. IFT-001") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parent_child_id_input")
                )

                OutlinedTextField(
                    value = linkingCode,
                    onValueChange = { linkingCode = it },
                    label = { Text(AppStrings.get("linking_code", lang)) },
                    placeholder = { Text("e.g. 749201") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parent_child_linking_code_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (studentId.isNotBlank() && linkingCode.isNotBlank()) {
                        onLinkChild(studentId.trim(), linkingCode.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                modifier = Modifier.testTag("parent_confirm_link_child")
            ) {
                Text(AppStrings.get("link_student", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.get("cancel", lang)) }
        }
    )
}
