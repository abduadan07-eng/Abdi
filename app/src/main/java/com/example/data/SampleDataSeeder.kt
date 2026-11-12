package com.example.data

import android.util.Log
import com.example.data.models.*
import com.example.data.repository.SchoolRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object SampleDataSeeder {
    suspend fun seedInitialDataIfEmpty(repository: SchoolRepository, db: FirebaseFirestore) {
        try {
            val classesSnapshot = db.collection("classes")
                .whereEqualTo("schoolId", FIXED_SCHOOL_ID)
                .limit(1)
                .get()
                .await()

            if (classesSnapshot.isEmpty) {
                Log.d("Seeder", "Seeding initial school data for Iftin Academy...")

                // 1. Classes
                val class1 = ClassRoom(
                    classId = "class_form1a",
                    schoolId = FIXED_SCHOOL_ID,
                    name = "Form 1A",
                    classTeacherName = "Ustadh Ahmed Ali",
                    subjects = listOf("Somali", "English", "Mathematics", "Science", "Islamic Studies"),
                    academicYear = "2025-2026",
                    accessCode = "4819",
                    studentCount = 4
                )
                val class2 = ClassRoom(
                    classId = "class_grade8",
                    schoolId = FIXED_SCHOOL_ID,
                    name = "Grade 8",
                    classTeacherName = "Ustaad Maryam Jama",
                    subjects = listOf("Somali", "English", "Mathematics", "Science", "Social Studies"),
                    academicYear = "2025-2026",
                    accessCode = "7294",
                    studentCount = 2
                )
                db.collection("classes").document(class1.classId).set(class1).await()
                db.collection("classes").document(class2.classId).set(class2).await()

                // 2. Teachers
                val t1 = Teacher(
                    teacherId = "teacher_1",
                    schoolId = FIXED_SCHOOL_ID,
                    fullName = "Ustadh Ahmed Ali",
                    phone = "+252 61 711 2233",
                    email = "ahmed.ali@iftinschool.so",
                    assignedClasses = listOf("class_form1a"),
                    subjects = listOf("Mathematics", "Science")
                )
                val t2 = Teacher(
                    teacherId = "teacher_2",
                    schoolId = FIXED_SCHOOL_ID,
                    fullName = "Ustaad Maryam Jama",
                    phone = "+252 61 822 4455",
                    email = "maryam.jama@iftinschool.so",
                    assignedClasses = listOf("class_grade8"),
                    subjects = listOf("Somali", "Islamic Studies")
                )
                db.collection("teachers").document(t1.teacherId).set(t1).await()
                db.collection("teachers").document(t2.teacherId).set(t2).await()

                // 3. Students (with clear linking codes for parents)
                val s1 = Student(
                    studentId = "IFT-101",
                    schoolId = FIXED_SCHOOL_ID,
                    fullName = "Abdi Mohamed Farah",
                    fatherName = "Mohamed Farah",
                    motherName = "Fadumo Ali",
                    parentPhone = "+252 61 900 1122",
                    gender = "M",
                    classId = "class_form1a",
                    className = "Form 1A",
                    admissionDate = "2025-08-15",
                    linkingCode = "749201"
                )
                val s2 = Student(
                    studentId = "IFT-102",
                    schoolId = FIXED_SCHOOL_ID,
                    fullName = "Amina Hassan Warsame",
                    fatherName = "Hassan Warsame",
                    motherName = "Ayan Yusuf",
                    parentPhone = "+252 61 900 3344",
                    gender = "F",
                    classId = "class_form1a",
                    className = "Form 1A",
                    admissionDate = "2025-08-15",
                    linkingCode = "831942"
                )
                val s3 = Student(
                    studentId = "IFT-103",
                    schoolId = FIXED_SCHOOL_ID,
                    fullName = "Khadar Omar Guled",
                    fatherName = "Omar Guled",
                    motherName = "Halimo Abdi",
                    parentPhone = "+252 61 900 5566",
                    gender = "M",
                    classId = "class_form1a",
                    className = "Form 1A",
                    admissionDate = "2025-08-16",
                    linkingCode = "652190"
                )
                val s4 = Student(
                    studentId = "IFT-104",
                    schoolId = FIXED_SCHOOL_ID,
                    fullName = "Sahra Osman Shire",
                    fatherName = "Osman Shire",
                    motherName = "Deqa Noor",
                    parentPhone = "+252 61 900 7788",
                    gender = "F",
                    classId = "class_form1a",
                    className = "Form 1A",
                    admissionDate = "2025-08-17",
                    linkingCode = "941320"
                )
                db.collection("students").document(s1.studentId).set(s1).await()
                db.collection("students").document(s2.studentId).set(s2).await()
                db.collection("students").document(s3.studentId).set(s3).await()
                db.collection("students").document(s4.studentId).set(s4).await()

                // 4. Examination
                val exam1 = Examination(
                    examId = "exam_midterm_1",
                    schoolId = FIXED_SCHOOL_ID,
                    name = "Imtixaanka Nus-Sannadka (Midterm Exam)",
                    examType = "MIDTERM",
                    classId = "class_form1a",
                    className = "Form 1A",
                    academicYear = "2025-2026",
                    startDate = "2026-10-10",
                    endDate = "2026-10-15",
                    subjects = listOf(
                        ExamSubject("Somali", 100.0),
                        ExamSubject("English", 100.0),
                        ExamSubject("Mathematics", 100.0),
                        ExamSubject("Science", 100.0),
                        ExamSubject("Islamic Studies", 100.0)
                    ),
                    description = "Official Midterm Examination for Form 1A",
                    status = "PUBLISHED"
                )
                db.collection("examinations").document(exam1.examId).set(exam1).await()

                // 5. Exam Results demonstrating competition ranking (Section 7.8: 1, 1, 3, 4)
                val r1 = ExamResult(
                    resultId = "exam_midterm_1_IFT-101",
                    schoolId = FIXED_SCHOOL_ID,
                    examId = exam1.examId,
                    examName = exam1.name,
                    studentId = s1.studentId,
                    studentName = s1.fullName,
                    classId = class1.classId,
                    className = class1.name,
                    subjectMarks = mapOf("Somali" to 95.0, "English" to 92.0, "Mathematics" to 98.0, "Science" to 96.0, "Islamic Studies" to 99.0),
                    totalObtained = 480.0,
                    totalMax = 500.0,
                    percentage = 96.0,
                    average = 96.0,
                    grade = "A+",
                    position = 1L,
                    teacherRemarks = "Excellent student! Consistently dedicated and disciplined.",
                    isPublished = true
                )
                val r2 = ExamResult(
                    resultId = "exam_midterm_1_IFT-102",
                    schoolId = FIXED_SCHOOL_ID,
                    examId = exam1.examId,
                    examName = exam1.name,
                    studentId = s2.studentId,
                    studentName = s2.fullName,
                    classId = class1.classId,
                    className = class1.name,
                    subjectMarks = mapOf("Somali" to 96.0, "English" to 95.0, "Mathematics" to 94.0, "Science" to 97.0, "Islamic Studies" to 98.0),
                    totalObtained = 480.0,
                    totalMax = 500.0,
                    percentage = 96.0,
                    average = 96.0,
                    grade = "A+",
                    position = 1L, // Equal marks -> same position 1
                    teacherRemarks = "Outstanding academic achievement.",
                    isPublished = true
                )
                val r3 = ExamResult(
                    resultId = "exam_midterm_1_IFT-103",
                    schoolId = FIXED_SCHOOL_ID,
                    examId = exam1.examId,
                    examName = exam1.name,
                    studentId = s3.studentId,
                    studentName = s3.fullName,
                    classId = class1.classId,
                    className = class1.name,
                    subjectMarks = mapOf("Somali" to 90.0, "English" to 88.0, "Mathematics" to 92.0, "Science" to 94.0, "Islamic Studies" to 96.0),
                    totalObtained = 460.0,
                    totalMax = 500.0,
                    percentage = 92.0,
                    average = 92.0,
                    grade = "A+",
                    position = 3L, // Skips position 2 per competition ranking!
                    teacherRemarks = "Very good performance, keep working hard.",
                    isPublished = true
                )
                val r4 = ExamResult(
                    resultId = "exam_midterm_1_IFT-104",
                    schoolId = FIXED_SCHOOL_ID,
                    examId = exam1.examId,
                    examName = exam1.name,
                    studentId = s4.studentId,
                    studentName = s4.fullName,
                    classId = class1.classId,
                    className = class1.name,
                    subjectMarks = mapOf("Somali" to 88.0, "English" to 85.0, "Mathematics" to 90.0, "Science" to 91.0, "Islamic Studies" to 96.0),
                    totalObtained = 450.0,
                    totalMax = 500.0,
                    percentage = 90.0,
                    average = 90.0,
                    grade = "A+",
                    position = 4L,
                    teacherRemarks = "Good effort, well done.",
                    isPublished = true
                )
                db.collection("exam_results").document(r1.resultId).set(r1).await()
                db.collection("exam_results").document(r2.resultId).set(r2).await()
                db.collection("exam_results").document(r3.resultId).set(r3).await()
                db.collection("exam_results").document(r4.resultId).set(r4).await()

                // 6. Fees
                val fee1 = Fee(
                    feeId = "fee_nov_tuition",
                    schoolId = FIXED_SCHOOL_ID,
                    feeName = "Bisha Nofembar - Fiiga Dugsiga (November Tuition)",
                    classId = class1.classId,
                    className = class1.name,
                    amount = 20.0,
                    dueDate = "2026-11-20",
                    description = "Monthly Tuition Fee"
                )
                db.collection("fees").document(fee1.feeId).set(fee1).await()

                val p1 = FeePayment(
                    paymentId = "${fee1.feeId}_${s1.studentId}",
                    schoolId = FIXED_SCHOOL_ID,
                    feeId = fee1.feeId,
                    feeName = fee1.feeName,
                    studentId = s1.studentId,
                    studentName = s1.fullName,
                    classId = class1.classId,
                    className = class1.name,
                    amount = 20.0,
                    amountPaid = 20.0,
                    balance = 0.0,
                    status = "PAID",
                    paymentDate = "2026-11-05",
                    receiptNo = "RCP-84912"
                )
                val p2 = FeePayment(
                    paymentId = "${fee1.feeId}_${s2.studentId}",
                    schoolId = FIXED_SCHOOL_ID,
                    feeId = fee1.feeId,
                    feeName = fee1.feeName,
                    studentId = s2.studentId,
                    studentName = s2.fullName,
                    classId = class1.classId,
                    className = class1.name,
                    amount = 20.0,
                    amountPaid = 0.0,
                    balance = 20.0,
                    status = "UNPAID"
                )
                db.collection("fee_payments").document(p1.paymentId).set(p1).await()
                db.collection("fee_payments").document(p2.paymentId).set(p2).await()

                // 7. Homework
                val hw1 = Homework(
                    homeworkId = "hw_math_1",
                    schoolId = FIXED_SCHOOL_ID,
                    classId = class1.classId,
                    className = class1.name,
                    subject = "Mathematics",
                    title = "Algebraic Equations Chapter 4",
                    description = "Complete exercises 4.1 to 4.5 in your textbook.",
                    assignedDate = "2026-11-10",
                    dueDate = "2026-11-15",
                    teacherName = "Ustadh Ahmed Ali"
                )
                db.collection("homework").document(hw1.homeworkId).set(hw1).await()

                // 8. Timetable
                val tt1 = TimetableSlot(
                    timetableId = "tt_1",
                    schoolId = FIXED_SCHOOL_ID,
                    classId = class1.classId,
                    className = class1.name,
                    day = "Monday",
                    subject = "Mathematics",
                    teacherName = "Ustadh Ahmed Ali",
                    startTime = "07:30 AM",
                    endTime = "08:15 AM",
                    roomNo = "Hall A"
                )
                val tt2 = TimetableSlot(
                    timetableId = "tt_2",
                    schoolId = FIXED_SCHOOL_ID,
                    classId = class1.classId,
                    className = class1.name,
                    day = "Monday",
                    subject = "Somali Language",
                    teacherName = "Ustaad Maryam Jama",
                    startTime = "08:15 AM",
                    endTime = "09:00 AM",
                    roomNo = "Hall A"
                )
                db.collection("timetables").document(tt1.timetableId).set(tt1).await()
                db.collection("timetables").document(tt2.timetableId).set(tt2).await()

                // 9. Remarks
                val rmk1 = StudentRemark(
                    remarkId = "rmk_1",
                    schoolId = FIXED_SCHOOL_ID,
                    studentId = s1.studentId,
                    studentName = s1.fullName,
                    classId = class1.classId,
                    teacherId = t1.teacherId,
                    teacherName = t1.fullName,
                    date = "2026-11-08",
                    category = "GOOD_PERFORMANCE",
                    message = "Abdi showed exceptional participation in mathematics this week!"
                )
                db.collection("remarks").document(rmk1.remarkId).set(rmk1).await()

                // 10. Notification
                val notif1 = SchoolNotification(
                    notificationId = "notif_welcome",
                    schoolId = FIXED_SCHOOL_ID,
                    targetRole = "ALL",
                    title = "Ku soo dhowow Sannad Dugsiyeedka 2025-2026",
                    message = "Waxbarashadu waxay u socotaa si heer sare ah dhammaan fasalada Iftin Academy.",
                    date = "2026-11-10"
                )
                db.collection("notifications").document(notif1.notificationId).set(notif1).await()

                Log.d("Seeder", "Initial data seeded successfully!")
            }
        } catch (e: Exception) {
            Log.e("Seeder", "Error during seeding: ${e.message}", e)
        }
    }
}
