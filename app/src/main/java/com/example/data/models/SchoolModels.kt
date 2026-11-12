package com.example.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val FIXED_SCHOOL_ID = "iftin_school_2026"

@IgnoreExtraProperties
data class SchoolConfig(
    @DocumentId val id: String = "main",
    val schoolId: String = FIXED_SCHOOL_ID,
    val name: String = "Iftin Academy",
    val motto: String = "Education for a Brighter Future",
    val address: String = "KM4, Wadajir, Mogadishu, Somalia",
    val phone: String = "+252 61 555 1234",
    val email: String = "admin@iftinschool.so",
    val academicYear: String = "2025-2026",
    val adminUid: String = "",
    val adminPasscode: String = "IFTIN#2026",
    val gradingScale: Map<String, Long> = mapOf(
        "A_PLUS" to 90L,
        "A" to 80L,
        "B" to 70L,
        "C" to 60L,
        "D" to 50L
    ),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val userId: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "PARENT", // "ADMIN", "TEACHER", "PARENT"
    val schoolId: String = FIXED_SCHOOL_ID,
    val teacherId: String = "",
    val parentId: String = "",
    val language: String = "so", // "so" or "en"
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Teacher(
    @DocumentId val teacherId: String = "",
    val schoolId: String = FIXED_SCHOOL_ID,
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val assignedClasses: List<String> = emptyList(), // class IDs
    val subjects: List<String> = emptyList(),
    val status: String = "ACTIVE",
    val photoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ClassRoom(
    @DocumentId val classId: String = "",
    val schoolId: String = FIXED_SCHOOL_ID,
    val name: String = "", // e.g. Form 1A, Grade 8, Class 5
    val classTeacherId: String = "",
    val classTeacherName: String = "",
    val subjects: List<String> = emptyList(),
    val academicYear: String = "2025-2026",
    val accessCode: String = "",
    val studentCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Student(
    @DocumentId val studentId: String = "", // Unique ID like "IFT-001"
    val schoolId: String = FIXED_SCHOOL_ID,
    val fullName: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val parentPhone: String = "",
    val gender: String = "M", // "M" or "F"
    val classId: String = "",
    val className: String = "",
    val admissionDate: String = "",
    val linkingCode: String = "", // Secure code for parent linking, e.g. "749201"
    val status: String = "ACTIVE",
    val photoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ParentProfile(
    @DocumentId val parentId: String = "", // userId
    val schoolId: String = FIXED_SCHOOL_ID,
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val linkedStudentIds: List<String> = emptyList()
)

@IgnoreExtraProperties
data class AttendanceRecord(
    val studentId: String = "",
    val studentName: String = "",
    val status: String = "PRESENT" // "PRESENT", "ABSENT", "LATE", "EXCUSED"
)

@IgnoreExtraProperties
data class DailyAttendance(
    @DocumentId val attendanceId: String = "", // "${classId}_${date}"
    val schoolId: String = FIXED_SCHOOL_ID,
    val classId: String = "",
    val className: String = "",
    val date: String = "", // YYYY-MM-DD
    val teacherId: String = "",
    val teacherName: String = "",
    val records: List<AttendanceRecord> = emptyList(),
    val presentCount: Long = 0L,
    val absentCount: Long = 0L,
    val lateCount: Long = 0L,
    val excusedCount: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExamSubject(
    val name: String = "",
    val maxMarks: Double = 100.0
)

@IgnoreExtraProperties
data class Examination(
    @DocumentId val examId: String = "",
    val schoolId: String = FIXED_SCHOOL_ID,
    val name: String = "",
    val examType: String = "MONTHLY", // "MONTHLY", "MIDTERM", "FINAL", "WEEKLY", "CUSTOM"
    val classId: String = "",
    val className: String = "",
    val academicYear: String = "2025-2026",
    val startDate: String = "",
    val endDate: String = "",
    val subjects: List<ExamSubject> = emptyList(),
    val description: String = "",
    val status: String = "DRAFT", // "DRAFT", "OPEN", "SUBMITTED", "UNDER_REVIEW", "APPROVED", "PUBLISHED", "LOCKED"
    val createdByTeacherId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExamResult(
    @DocumentId val resultId: String = "", // "${examId}_${studentId}"
    val schoolId: String = FIXED_SCHOOL_ID,
    val examId: String = "",
    val examName: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val classId: String = "",
    val className: String = "",
    val subjectMarks: Map<String, Double> = emptyMap(),
    val totalObtained: Double = 0.0,
    val totalMax: Double = 0.0,
    val percentage: Double = 0.0,
    val average: Double = 0.0,
    val grade: String = "F",
    val position: Long = 0L, // Competition rank
    val teacherRemarks: String = "",
    val isPublished: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Fee(
    @DocumentId val feeId: String = "",
    val schoolId: String = FIXED_SCHOOL_ID,
    val feeName: String = "",
    val classId: String = "",
    val className: String = "",
    val amount: Double = 0.0,
    val dueDate: String = "",
    val description: String = "",
    val academicYear: String = "2025-2026",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FeePayment(
    @DocumentId val paymentId: String = "",
    val schoolId: String = FIXED_SCHOOL_ID,
    val feeId: String = "",
    val feeName: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val classId: String = "",
    val className: String = "",
    val amount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balance: Double = 0.0,
    val status: String = "UNPAID", // "UNPAID", "PARTIALLY_PAID", "PAID", "OVERDUE"
    val paymentDate: String = "",
    val receiptNo: String = "",
    val notes: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TimetableSlot(
    @DocumentId val timetableId: String = "",
    val schoolId: String = FIXED_SCHOOL_ID,
    val classId: String = "",
    val className: String = "",
    val day: String = "Monday", // Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
    val subject: String = "",
    val teacherName: String = "",
    val startTime: String = "", // e.g. "08:00 AM"
    val endTime: String = "",   // e.g. "08:45 AM"
    val roomNo: String = ""
)

@IgnoreExtraProperties
data class Homework(
    @DocumentId val homeworkId: String = "",
    val schoolId: String = FIXED_SCHOOL_ID,
    val classId: String = "",
    val className: String = "",
    val subject: String = "",
    val title: String = "",
    val description: String = "",
    val assignedDate: String = "",
    val dueDate: String = "",
    val teacherName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudentRemark(
    @DocumentId val remarkId: String = "",
    val schoolId: String = FIXED_SCHOOL_ID,
    val studentId: String = "",
    val studentName: String = "",
    val classId: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val date: String = "",
    val category: String = "GOOD_PERFORMANCE", // GOOD_PERFORMANCE, EXCELLENT_BEHAVIOR, NEEDS_IMPROVEMENT, HOMEWORK_PROBLEM, ATTENDANCE_PROBLEM, GENERAL_COMPLAINT, POSITIVE_FEEDBACK
    val message: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SchoolNotification(
    @DocumentId val notificationId: String = "",
    val schoolId: String = FIXED_SCHOOL_ID,
    val targetRole: String = "ALL", // "ALL", "ADMIN", "TEACHER", "PARENT"
    val targetUserId: String = "",
    val targetStudentId: String = "",
    val title: String = "",
    val message: String = "",
    val date: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
