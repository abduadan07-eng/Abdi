package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.models.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SchoolRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before accessing school data.")
    }

    // --- School Config ---
    suspend fun getSchoolConfig(): SchoolConfig {
        val doc = db.collection("school_config").document("main").get().await()
        return if (doc.exists()) {
            doc.toObject(SchoolConfig::class.java) ?: SchoolConfig()
        } else {
            val defaultConfig = SchoolConfig()
            db.collection("school_config").document("main").set(defaultConfig).await()
            defaultConfig
        }
    }

    suspend fun updateSchoolConfig(config: SchoolConfig) {
        db.collection("school_config").document("main").set(config).await()
    }

    // --- User Profile ---
    suspend fun getUserProfile(uid: String): UserProfile? {
        val doc = db.collection("users").document(uid).get().await()
        return doc.toObject(UserProfile::class.java)
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        db.collection("users").document(profile.userId).set(profile).await()
    }

    // --- Teachers ---
    fun observeTeachers(): Flow<List<Teacher>> {
        return db.collection("teachers")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .snapshots()
            .map { it.toObjects(Teacher::class.java) }
    }

    suspend fun addTeacher(teacher: Teacher) {
        val docRef = if (teacher.teacherId.isNotBlank()) {
            db.collection("teachers").document(teacher.teacherId)
        } else {
            db.collection("teachers").document()
        }
        val data = teacher.copy(teacherId = docRef.id, schoolId = FIXED_SCHOOL_ID)
        docRef.set(data).await()
    }

    suspend fun updateTeacher(teacher: Teacher) {
        db.collection("teachers").document(teacher.teacherId).set(teacher).await()
    }

    suspend fun deleteTeacher(teacherId: String) {
        db.collection("teachers").document(teacherId).delete().await()
    }

    // --- Classes ---
    fun observeClasses(): Flow<List<ClassRoom>> {
        return db.collection("classes")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .snapshots()
            .map { it.toObjects(ClassRoom::class.java) }
    }

    suspend fun addClass(classRoom: ClassRoom) {
        val docRef = if (classRoom.classId.isNotBlank()) {
            db.collection("classes").document(classRoom.classId)
        } else {
            db.collection("classes").document()
        }
        val data = classRoom.copy(classId = docRef.id, schoolId = FIXED_SCHOOL_ID)
        docRef.set(data).await()
    }

    suspend fun updateClass(classRoom: ClassRoom) {
        db.collection("classes").document(classRoom.classId).set(classRoom).await()
    }

    suspend fun deleteClass(classId: String) {
        db.collection("classes").document(classId).delete().await()
    }

    // --- Students ---
    fun observeStudents(): Flow<List<Student>> {
        return db.collection("students")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .snapshots()
            .map { it.toObjects(Student::class.java) }
    }

    fun observeStudentsByClass(classId: String): Flow<List<Student>> {
        return db.collection("students")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .whereEqualTo("classId", classId)
            .snapshots()
            .map { it.toObjects(Student::class.java) }
    }

    suspend fun addStudent(student: Student) {
        val studentId = if (student.studentId.isNotBlank()) student.studentId else "IFT-${System.currentTimeMillis() % 10000}"
        val linkingCode = if (student.linkingCode.isNotBlank()) student.linkingCode else (100000 + (Math.random() * 900000).toInt()).toString()
        val data = student.copy(
            studentId = studentId,
            linkingCode = linkingCode,
            schoolId = FIXED_SCHOOL_ID
        )
        db.collection("students").document(studentId).set(data).await()

        // Update class student count
        if (student.classId.isNotBlank()) {
            try {
                db.collection("classes").document(student.classId)
                    .update("studentCount", FieldValue.increment(1))
                    .await()
            } catch (e: Exception) {
                Log.w("SchoolRepo", "Failed to increment class studentCount", e)
            }
        }
    }

    suspend fun updateStudent(student: Student) {
        db.collection("students").document(student.studentId).set(student).await()
    }

    suspend fun deleteStudent(studentId: String) {
        db.collection("students").document(studentId).delete().await()
    }

    suspend fun findStudentByLinkingCode(studentId: String, code: String): Student? {
        val doc = db.collection("students").document(studentId).get().await()
        if (doc.exists()) {
            val s = doc.toObject(Student::class.java)
            if (s != null && s.linkingCode.trim() == code.trim() && s.schoolId == FIXED_SCHOOL_ID) {
                return s
            }
        }
        return null
    }

    // --- Parents ---
    suspend fun linkChildToParent(parentId: String, studentId: String) {
        val parentRef = db.collection("parents").document(parentId)
        val doc = parentRef.get().await()
        if (doc.exists()) {
            parentRef.update("linkedStudentIds", FieldValue.arrayUnion(studentId)).await()
        } else {
            val profile = ParentProfile(
                parentId = parentId,
                schoolId = FIXED_SCHOOL_ID,
                linkedStudentIds = listOf(studentId)
            )
            parentRef.set(profile).await()
        }
    }

    fun observeParentProfile(parentId: String): Flow<ParentProfile?> {
        return db.collection("parents").document(parentId)
            .snapshots()
            .map { it.toObject(ParentProfile::class.java) }
    }

    suspend fun getStudentsByIds(ids: List<String>): List<Student> {
        if (ids.isEmpty()) return emptyList()
        val result = mutableListOf<Student>()
        for (id in ids) {
            val doc = db.collection("students").document(id).get().await()
            doc.toObject(Student::class.java)?.let { result.add(it) }
        }
        return result
    }

    // --- Attendance ---
    suspend fun saveAttendance(attendance: DailyAttendance) {
        val docId = "${attendance.classId}_${attendance.date}"
        val data = attendance.copy(attendanceId = docId, schoolId = FIXED_SCHOOL_ID)
        db.collection("attendance").document(docId).set(data).await()
    }

    suspend fun getAttendance(classId: String, date: String): DailyAttendance? {
        val docId = "${classId}_${date}"
        val doc = db.collection("attendance").document(docId).get().await()
        return doc.toObject(DailyAttendance::class.java)
    }

    fun observeAttendanceByClass(classId: String): Flow<List<DailyAttendance>> {
        return db.collection("attendance")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .whereEqualTo("classId", classId)
            .snapshots()
            .map { it.toObjects(DailyAttendance::class.java) }
    }

    fun observeAllAttendance(): Flow<List<DailyAttendance>> {
        return db.collection("attendance")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .snapshots()
            .map { it.toObjects(DailyAttendance::class.java) }
    }

    // --- Examinations ---
    fun observeExaminations(): Flow<List<Examination>> {
        return db.collection("examinations")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .snapshots()
            .map { it.toObjects(Examination::class.java) }
    }

    fun observeExaminationsByClass(classId: String): Flow<List<Examination>> {
        return db.collection("examinations")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .whereEqualTo("classId", classId)
            .snapshots()
            .map { it.toObjects(Examination::class.java) }
    }

    suspend fun saveExamination(exam: Examination) {
        val docRef = if (exam.examId.isNotBlank()) {
            db.collection("examinations").document(exam.examId)
        } else {
            db.collection("examinations").document()
        }
        val data = exam.copy(examId = docRef.id, schoolId = FIXED_SCHOOL_ID)
        docRef.set(data).await()
    }

    suspend fun updateExamStatus(examId: String, status: String) {
        db.collection("examinations").document(examId).update("status", status).await()
        if (status == "PUBLISHED") {
            // Also update all results for this exam to published = true
            val resultsSnapshot = db.collection("exam_results")
                .whereEqualTo("examId", examId)
                .get()
                .await()
            val batch = db.batch()
            for (doc in resultsSnapshot.documents) {
                batch.update(doc.reference, "isPublished", true)
            }
            batch.commit().await()
        }
    }

    // --- Exam Results & Competition Ranking ---
    fun observeExamResults(examId: String): Flow<List<ExamResult>> {
        return db.collection("exam_results")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .whereEqualTo("examId", examId)
            .snapshots()
            .map { it.toObjects(ExamResult::class.java) }
    }

    fun observeStudentResults(studentId: String): Flow<List<ExamResult>> {
        return db.collection("exam_results")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .whereEqualTo("studentId", studentId)
            .snapshots()
            .map { it.toObjects(ExamResult::class.java) }
    }

    companion object {
        /**
         * Calculates Competition Ranking (1, 1, 3, 4, 4, 6) and Grade calculation
         */
        fun computeRankingAndGrades(
            results: List<ExamResult>,
            config: SchoolConfig
        ): List<ExamResult> {
            val sorted = results.sortedByDescending { it.totalObtained }
            val updated = mutableListOf<ExamResult>()

            var currentRank = 1L
            for (i in sorted.indices) {
                val item = sorted[i]
                if (i > 0 && item.totalObtained == sorted[i - 1].totalObtained) {
                    // Same rank as previous student
                    val previousRank = updated[i - 1].position
                    val grade = calculateGrade(item.percentage, config.gradingScale)
                    updated.add(item.copy(position = previousRank, grade = grade))
                } else {
                    currentRank = (i + 1).toLong()
                    val grade = calculateGrade(item.percentage, config.gradingScale)
                    updated.add(item.copy(position = currentRank, grade = grade))
                }
            }
            return updated
        }

        fun calculateGrade(percentage: Double, scale: Map<String, Long>): String {
            val aPlus = scale["A_PLUS"] ?: 90L
            val a = scale["A"] ?: 80L
            val b = scale["B"] ?: 70L
            val c = scale["C"] ?: 60L
            val d = scale["D"] ?: 50L

            return when {
                percentage >= aPlus -> "A+"
                percentage >= a -> "A"
                percentage >= b -> "B"
                percentage >= c -> "C"
                percentage >= d -> "D"
                else -> "F"
            }
        }
    }

    suspend fun saveExamResultsBatch(results: List<ExamResult>) {
        val config = getSchoolConfig()
        val rankedResults = computeRankingAndGrades(results, config)
        val batch = db.batch()
        for (res in rankedResults) {
            val docId = "${res.examId}_${res.studentId}"
            val ref = db.collection("exam_results").document(docId)
            val data = res.copy(resultId = docId, schoolId = FIXED_SCHOOL_ID)
            batch.set(ref, data)
        }
        batch.commit().await()
    }

    // --- Fees & Payments ---
    fun observeFees(): Flow<List<Fee>> {
        return db.collection("fees")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .snapshots()
            .map { it.toObjects(Fee::class.java) }
    }

    fun observeFeesByClass(classId: String): Flow<List<Fee>> {
        return db.collection("fees")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .whereEqualTo("classId", classId)
            .snapshots()
            .map { it.toObjects(Fee::class.java) }
    }

    suspend fun addFee(fee: Fee, studentsInClass: List<Student>) {
        val feeRef = if (fee.feeId.isNotBlank()) db.collection("fees").document(fee.feeId) else db.collection("fees").document()
        val data = fee.copy(feeId = feeRef.id, schoolId = FIXED_SCHOOL_ID)
        feeRef.set(data).await()

        // Generate FeePayment records for every student in the class
        val batch = db.batch()
        for (student in studentsInClass) {
            val paymentId = "${feeRef.id}_${student.studentId}"
            val paymentRef = db.collection("fee_payments").document(paymentId)
            val payment = FeePayment(
                paymentId = paymentId,
                schoolId = FIXED_SCHOOL_ID,
                feeId = feeRef.id,
                feeName = fee.feeName,
                studentId = student.studentId,
                studentName = student.fullName,
                classId = student.classId,
                className = student.className,
                amount = fee.amount,
                amountPaid = 0.0,
                balance = fee.amount,
                status = "UNPAID"
            )
            batch.set(paymentRef, payment)
        }
        batch.commit().await()
    }

    fun observeFeePayments(): Flow<List<FeePayment>> {
        return db.collection("fee_payments")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .snapshots()
            .map { it.toObjects(FeePayment::class.java) }
    }

    fun observeStudentFeePayments(studentId: String): Flow<List<FeePayment>> {
        return db.collection("fee_payments")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .whereEqualTo("studentId", studentId)
            .snapshots()
            .map { it.toObjects(FeePayment::class.java) }
    }

    suspend fun recordPayment(paymentId: String, newPaidAmount: Double, receiptNo: String, date: String) {
        val docRef = db.collection("fee_payments").document(paymentId)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            val payment = snapshot.toObject(FeePayment::class.java) ?: return
            val totalPaid = payment.amountPaid + newPaidAmount
            val remaining = (payment.amount - totalPaid).coerceAtLeast(0.0)
            val status = if (remaining <= 0.0) "PAID" else "PARTIALLY_PAID"

            docRef.update(
                mapOf(
                    "amountPaid" to totalPaid,
                    "balance" to remaining,
                    "status" to status,
                    "receiptNo" to receiptNo,
                    "paymentDate" to date
                )
            ).await()
        }
    }

    // --- Timetable ---
    fun observeTimetable(classId: String): Flow<List<TimetableSlot>> {
        return db.collection("timetables")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .whereEqualTo("classId", classId)
            .snapshots()
            .map { it.toObjects(TimetableSlot::class.java) }
    }

    suspend fun addTimetableSlot(slot: TimetableSlot) {
        val docRef = if (slot.timetableId.isNotBlank()) db.collection("timetables").document(slot.timetableId) else db.collection("timetables").document()
        val data = slot.copy(timetableId = docRef.id, schoolId = FIXED_SCHOOL_ID)
        docRef.set(data).await()
    }

    suspend fun deleteTimetableSlot(slotId: String) {
        db.collection("timetables").document(slotId).delete().await()
    }

    // --- Homework ---
    fun observeHomework(classId: String): Flow<List<Homework>> {
        return db.collection("homework")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .whereEqualTo("classId", classId)
            .snapshots()
            .map { it.toObjects(Homework::class.java) }
    }

    suspend fun addHomework(hw: Homework) {
        val docRef = if (hw.homeworkId.isNotBlank()) db.collection("homework").document(hw.homeworkId) else db.collection("homework").document()
        val data = hw.copy(homeworkId = docRef.id, schoolId = FIXED_SCHOOL_ID)
        docRef.set(data).await()
    }

    suspend fun deleteHomework(hwId: String) {
        db.collection("homework").document(hwId).delete().await()
    }

    // --- Remarks ---
    fun observeRemarksForStudent(studentId: String): Flow<List<StudentRemark>> {
        return db.collection("remarks")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .whereEqualTo("studentId", studentId)
            .snapshots()
            .map { it.toObjects(StudentRemark::class.java) }
    }

    suspend fun addRemark(remark: StudentRemark) {
        val docRef = if (remark.remarkId.isNotBlank()) db.collection("remarks").document(remark.remarkId) else db.collection("remarks").document()
        val data = remark.copy(remarkId = docRef.id, schoolId = FIXED_SCHOOL_ID)
        docRef.set(data).await()
    }

    // --- Notifications ---
    fun observeNotifications(role: String, studentIds: List<String> = emptyList()): Flow<List<SchoolNotification>> {
        return db.collection("notifications")
            .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SchoolNotification::class.java).filter { notif ->
                    notif.targetRole == "ALL" || notif.targetRole == role || (role == "PARENT" && notif.targetStudentId in studentIds)
                }
            }
    }

    suspend fun addNotification(notif: SchoolNotification) {
        val docRef = if (notif.notificationId.isNotBlank()) db.collection("notifications").document(notif.notificationId) else db.collection("notifications").document()
        val data = notif.copy(notificationId = docRef.id, schoolId = FIXED_SCHOOL_ID)
        docRef.set(data).await()
    }

    suspend fun markNotificationAsRead(notifId: String) {
        db.collection("notifications").document(notifId).update("isRead", true).await()
    }
}
