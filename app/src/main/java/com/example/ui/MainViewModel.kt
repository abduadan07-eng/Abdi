package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.*
import com.example.data.repository.SchoolRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SchoolRepository(application)

    // Language state: default "so" (Somali), alternative "en" (English)
    private val _language = MutableStateFlow("so")
    val language: StateFlow<String> = _language.asStateFlow()

    // Authentication & Role State
    private val _currentUser = MutableStateFlow(Firebase.auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Portal Selection: "HOME", "ADMIN", "TEACHER", "PARENT"
    private val _currentPortal = MutableStateFlow("HOME")
    val currentPortal: StateFlow<String> = _currentPortal.asStateFlow()

    // Selected Sub-Screen / Tab within Portal
    private val _currentTab = MutableStateFlow("DASHBOARD")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // School Config
    private val _schoolConfig = MutableStateFlow(SchoolConfig())
    val schoolConfig: StateFlow<SchoolConfig> = _schoolConfig.asStateFlow()

    // Realtime Lists
    private val _teachers = MutableStateFlow<List<Teacher>>(emptyList())
    val teachers: StateFlow<List<Teacher>> = _teachers.asStateFlow()

    private val _classes = MutableStateFlow<List<ClassRoom>>(emptyList())
    val classes: StateFlow<List<ClassRoom>> = _classes.asStateFlow()

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _examinations = MutableStateFlow<List<Examination>>(emptyList())
    val examinations: StateFlow<List<Examination>> = _examinations.asStateFlow()

    private val _allAttendance = MutableStateFlow<List<DailyAttendance>>(emptyList())
    val allAttendance: StateFlow<List<DailyAttendance>> = _allAttendance.asStateFlow()

    private val _fees = MutableStateFlow<List<Fee>>(emptyList())
    val fees: StateFlow<List<Fee>> = _fees.asStateFlow()

    private val _feePayments = MutableStateFlow<List<FeePayment>>(emptyList())
    val feePayments: StateFlow<List<FeePayment>> = _feePayments.asStateFlow()

    private val _notifications = MutableStateFlow<List<SchoolNotification>>(emptyList())
    val notifications: StateFlow<List<SchoolNotification>> = _notifications.asStateFlow()

    // Teacher specific selected class
    private val _selectedClass = MutableStateFlow<ClassRoom?>(null)
    val selectedClass: StateFlow<ClassRoom?> = _selectedClass.asStateFlow()

    // Parent specific linked students and selected child
    private val _parentProfile = MutableStateFlow<ParentProfile?>(null)
    val parentProfile: StateFlow<ParentProfile?> = _parentProfile.asStateFlow()

    private val _linkedStudents = MutableStateFlow<List<Student>>(emptyList())
    val linkedStudents: StateFlow<List<Student>> = _linkedStudents.asStateFlow()

    private val _selectedChild = MutableStateFlow<Student?>(null)
    val selectedChild: StateFlow<Student?> = _selectedChild.asStateFlow()

    // Status / Message toast
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSchoolConfig()
        observeData()
    }

    fun setLanguage(lang: String) {
        _language.value = lang
    }

    fun selectPortal(portal: String) {
        _currentPortal.value = portal
        _currentTab.value = "DASHBOARD"
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun selectTeacherClass(classRoom: ClassRoom?) {
        _selectedClass.value = classRoom
    }

    fun selectChild(student: Student?) {
        _selectedChild.value = student
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    private fun loadSchoolConfig() {
        viewModelScope.launch {
            try {
                val cfg = repository.getSchoolConfig()
                _schoolConfig.value = cfg
                val db = FirebaseFirestore.getInstance(
                    getApplication<Application>().getString(com.example.R.string.firestore_database_id)
                )
                com.example.data.SampleDataSeeder.seedInitialDataIfEmpty(repository, db)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshAuthUser() {
        val user = Firebase.auth.currentUser
        _currentUser.value = user
        if (user != null) {
            viewModelScope.launch {
                val prof = repository.getUserProfile(user.uid)
                _userProfile.value = prof
                if (prof?.role == "PARENT") {
                    loadParentProfile(user.uid)
                }
            }
        } else {
            _userProfile.value = null
            _parentProfile.value = null
            _linkedStudents.value = emptyList()
            _selectedChild.value = null
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.observeTeachers().collectLatest { _teachers.value = it }
        }
        viewModelScope.launch {
            repository.observeClasses().collectLatest {
                _classes.value = it
                if (_selectedClass.value == null && it.isNotEmpty()) {
                    _selectedClass.value = it.first()
                }
            }
        }
        viewModelScope.launch {
            repository.observeStudents().collectLatest { _students.value = it }
        }
        viewModelScope.launch {
            repository.observeExaminations().collectLatest { _examinations.value = it }
        }
        viewModelScope.launch {
            repository.observeAllAttendance().collectLatest { _allAttendance.value = it }
        }
        viewModelScope.launch {
            repository.observeFees().collectLatest { _fees.value = it }
        }
        viewModelScope.launch {
            repository.observeFeePayments().collectLatest { _feePayments.value = it }
        }
        viewModelScope.launch {
            repository.observeNotifications("ALL").collectLatest { _notifications.value = it }
        }
    }

    fun loadParentProfile(parentId: String) {
        viewModelScope.launch {
            repository.observeParentProfile(parentId).collectLatest { profile ->
                _parentProfile.value = profile
                if (profile != null) {
                    val kids = repository.getStudentsByIds(profile.linkedStudentIds)
                    _linkedStudents.value = kids
                    if (_selectedChild.value == null && kids.isNotEmpty()) {
                        _selectedChild.value = kids.first()
                    }
                }
            }
        }
    }

    // --- Actions ---

    fun verifyAdminPasscode(enteredPasscode: String, onSuccess: () -> Unit, onError: () -> Unit) {
        val master = _schoolConfig.value.adminPasscode
        val user = Firebase.auth.currentUser
        if (user != null && (enteredPasscode.trim() == master || _userProfile.value?.role == "ADMIN")) {
            viewModelScope.launch {
                val profile = UserProfile(
                    userId = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: "Admin",
                    role = "ADMIN",
                    schoolId = FIXED_SCHOOL_ID,
                    language = _language.value
                )
                repository.saveUserProfile(profile)
                _userProfile.value = profile
                onSuccess()
            }
        } else {
            onError()
        }
    }

    fun createParentRoom(onSuccess: () -> Unit) {
        val user = Firebase.auth.currentUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = UserProfile(
                    userId = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: "Parent",
                    role = "PARENT",
                    schoolId = FIXED_SCHOOL_ID,
                    language = _language.value
                )
                repository.saveUserProfile(profile)
                _userProfile.value = profile
                loadParentProfile(user.uid)
                onSuccess()
            } catch (e: Exception) {
                _toastMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun linkChild(studentId: String, linkingCode: String, onResult: (Boolean) -> Unit) {
        val user = Firebase.auth.currentUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val found = repository.findStudentByLinkingCode(studentId, linkingCode)
                if (found != null) {
                    repository.linkChildToParent(user.uid, found.studentId)
                    loadParentProfile(user.uid)
                    _toastMessage.value = "Ardayga ${found.fullName} si guul leh ayaa loogu xiriiriyay!"
                    onResult(true)
                } else {
                    _toastMessage.value = "Lama helin arday leh ID-gan iyo Furahan!"
                    onResult(false)
                }
            } catch (e: Exception) {
                _toastMessage.value = e.localizedMessage
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Teacher Management
    fun addTeacher(fullName: String, phone: String, email: String, classes: List<String>, subjects: List<String>) {
        viewModelScope.launch {
            val t = Teacher(
                fullName = fullName,
                phone = phone,
                email = email,
                assignedClasses = classes,
                subjects = subjects
            )
            repository.addTeacher(t)
            _toastMessage.value = "Macallinka waa la keydiyay"
        }
    }

    fun deleteTeacher(teacherId: String) {
        viewModelScope.launch {
            repository.deleteTeacher(teacherId)
            _toastMessage.value = "Macallinka waa la tiray"
        }
    }

    // Class Management
    fun addClass(name: String, teacherName: String, subjects: List<String>, year: String) {
        viewModelScope.launch {
            val accessCode = (1000 + (Math.random() * 9000).toInt()).toString()
            val c = ClassRoom(
                name = name,
                classTeacherName = teacherName,
                subjects = subjects,
                academicYear = year,
                accessCode = accessCode
            )
            repository.addClass(c)
            _toastMessage.value = "Fasalka waa la keydiyay"
        }
    }

    fun deleteClass(classId: String) {
        viewModelScope.launch {
            repository.deleteClass(classId)
            _toastMessage.value = "Fasalka waa la tiray"
        }
    }

    // Student Management
    fun addStudent(
        fullName: String,
        fatherName: String,
        motherName: String,
        phone: String,
        gender: String,
        classId: String,
        className: String,
        admissionDate: String
    ) {
        viewModelScope.launch {
            val s = Student(
                fullName = fullName,
                fatherName = fatherName,
                motherName = motherName,
                parentPhone = phone,
                gender = gender,
                classId = classId,
                className = className,
                admissionDate = admissionDate
            )
            repository.addStudent(s)
            _toastMessage.value = "Ardayga waa la diiwaangeliyay"
        }
    }

    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            repository.deleteStudent(studentId)
            _toastMessage.value = "Ardayga waa la tiray"
        }
    }

    // Attendance Management
    fun markAttendance(classId: String, className: String, date: String, records: List<AttendanceRecord>) {
        viewModelScope.launch {
            val present = records.count { it.status == "PRESENT" }.toLong()
            val absent = records.count { it.status == "ABSENT" }.toLong()
            val late = records.count { it.status == "LATE" }.toLong()
            val excused = records.count { it.status == "EXCUSED" }.toLong()

            val teacherName = _userProfile.value?.name ?: "Teacher"
            val attendance = DailyAttendance(
                classId = classId,
                className = className,
                date = date,
                teacherId = _currentUser.value?.uid ?: "",
                teacherName = teacherName,
                records = records,
                presentCount = present,
                absentCount = absent,
                lateCount = late,
                excusedCount = excused
            )
            repository.saveAttendance(attendance)
            _toastMessage.value = "Xaadiriska waa la keydiyay"
        }
    }

    // Examination Management
    fun createExamination(
        name: String,
        type: String,
        classId: String,
        className: String,
        subjects: List<ExamSubject>,
        startDate: String,
        endDate: String,
        desc: String
    ) {
        viewModelScope.launch {
            val exam = Examination(
                name = name,
                examType = type,
                classId = classId,
                className = className,
                subjects = subjects,
                startDate = startDate,
                endDate = endDate,
                description = desc,
                status = "DRAFT",
                createdByTeacherId = _currentUser.value?.uid ?: ""
            )
            repository.saveExamination(exam)
            _toastMessage.value = "Imtixaanka waa la abuuray"
        }
    }

    fun updateExamStatus(examId: String, status: String) {
        viewModelScope.launch {
            repository.updateExamStatus(examId, status)
            _toastMessage.value = "Xaaladda imtixaanka waa la cusbooneysiiyay: $status"
        }
    }

    fun submitExamResults(exam: Examination, results: List<ExamResult>) {
        viewModelScope.launch {
            repository.saveExamResultsBatch(results)
            repository.updateExamStatus(exam.examId, "SUBMITTED")
            _toastMessage.value = "Natiijooyinka si guul leh ayaa loo gudbiyay!"
        }
    }

    // Fees Management
    fun addFee(feeName: String, classId: String, className: String, amount: Double, dueDate: String, desc: String) {
        viewModelScope.launch {
            val studentsInClass = _students.value.filter { it.classId == classId }
            val fee = Fee(
                feeName = feeName,
                classId = classId,
                className = className,
                amount = amount,
                dueDate = dueDate,
                description = desc
            )
            repository.addFee(fee, studentsInClass)
            _toastMessage.value = "Fiiga cusub waa la xilsaaray fasalka"
        }
    }

    fun recordFeePayment(paymentId: String, amount: Double, receiptNo: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.recordPayment(paymentId, amount, receiptNo, dateStr)
            _toastMessage.value = "Lacag bixinta waa la keydiyay"
        }
    }

    // Timetable Management
    fun addTimetableSlot(classId: String, className: String, day: String, subject: String, teacher: String, start: String, end: String, room: String) {
        viewModelScope.launch {
            val slot = TimetableSlot(
                classId = classId,
                className = className,
                day = day,
                subject = subject,
                teacherName = teacher,
                startTime = start,
                endTime = end,
                roomNo = room
            )
            repository.addTimetableSlot(slot)
            _toastMessage.value = "Jadwalka waa la kordhiyay"
        }
    }

    fun deleteTimetableSlot(slotId: String) {
        viewModelScope.launch {
            repository.deleteTimetableSlot(slotId)
            _toastMessage.value = "Qaybta jadwalka waa la tiray"
        }
    }

    // Homework Management
    fun addHomework(classId: String, className: String, subject: String, title: String, desc: String, due: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val hw = Homework(
                classId = classId,
                className = className,
                subject = subject,
                title = title,
                description = desc,
                assignedDate = dateStr,
                dueDate = due,
                teacherName = _userProfile.value?.name ?: "Teacher"
            )
            repository.addHomework(hw)
            _toastMessage.value = "Shaqo-guriga waa la diray"
        }
    }

    fun deleteHomework(hwId: String) {
        viewModelScope.launch {
            repository.deleteHomework(hwId)
            _toastMessage.value = "Shaqo-guriga waa la tiray"
        }
    }

    // Remarks Management
    fun addRemark(studentId: String, studentName: String, classId: String, category: String, message: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val remark = StudentRemark(
                studentId = studentId,
                studentName = studentName,
                classId = classId,
                teacherId = _currentUser.value?.uid ?: "",
                teacherName = _userProfile.value?.name ?: "Teacher",
                date = dateStr,
                category = category,
                message = message
            )
            repository.addRemark(remark)
            _toastMessage.value = "Fariinta waa la keydiyay"
        }
    }

    // Notifications
    fun sendNotification(title: String, message: String, targetRole: String, targetStudentId: String = "") {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val notif = SchoolNotification(
                targetRole = targetRole,
                targetStudentId = targetStudentId,
                title = title,
                message = message,
                date = dateStr
            )
            repository.addNotification(notif)
            _toastMessage.value = "Ogeysiiska waa la diray"
        }
    }

    // Update School Settings
    fun updateSchoolSettings(name: String, address: String, phone: String, email: String, year: String, passcode: String) {
        viewModelScope.launch {
            val current = _schoolConfig.value
            val updated = current.copy(
                name = name,
                address = address,
                phone = phone,
                email = email,
                academicYear = year,
                adminPasscode = if (passcode.isNotBlank()) passcode else current.adminPasscode
            )
            repository.updateSchoolConfig(updated)
            _schoolConfig.value = updated
            _toastMessage.value = "Hagaajinta dugsiga waa la keydiyay"
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        Firebase.auth.signOut()
        _currentUser.value = null
        _userProfile.value = null
        _parentProfile.value = null
        _linkedStudents.value = emptyList()
        _selectedChild.value = null
        _currentPortal.value = "HOME"
        onSignedOut()
    }
}
