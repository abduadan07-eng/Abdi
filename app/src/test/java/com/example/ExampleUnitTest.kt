package com.example

import com.example.data.models.ExamResult
import com.example.data.models.FIXED_SCHOOL_ID
import com.example.data.models.SchoolConfig
import com.example.data.repository.SchoolRepository
import com.example.localization.AppStrings
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSingleSchoolIdInvariant() {
        assertEquals("iftin_school_2026", FIXED_SCHOOL_ID)
    }

    @Test
    fun testCompetitionRankingWithEqualMarks() {
        // Section 7.8 requirement:
        // Abdi - 480 - Position 1
        // Adan - 480 - Position 1
        // Mohamed - 460 - Position 3
        // Ahmed - 450 - Position 4
        // Example: 1, 1, 3, 4, 4, 6
        val results = listOf(
            ExamResult(studentId = "S1", studentName = "Abdi", totalObtained = 480.0, totalMax = 500.0, percentage = 96.0),
            ExamResult(studentId = "S2", studentName = "Adan", totalObtained = 480.0, totalMax = 500.0, percentage = 96.0),
            ExamResult(studentId = "S3", studentName = "Mohamed", totalObtained = 460.0, totalMax = 500.0, percentage = 92.0),
            ExamResult(studentId = "S4", studentName = "Ahmed", totalObtained = 450.0, totalMax = 500.0, percentage = 90.0),
            ExamResult(studentId = "S5", studentName = "Ali", totalObtained = 450.0, totalMax = 500.0, percentage = 90.0),
            ExamResult(studentId = "S6", studentName = "Khadar", totalObtained = 400.0, totalMax = 500.0, percentage = 80.0)
        )

        val ranked = SchoolRepository.computeRankingAndGrades(results, SchoolConfig())

        assertEquals(1L, ranked[0].position) // Abdi
        assertEquals(1L, ranked[1].position) // Adan (ties Abdi -> 1)
        assertEquals(3L, ranked[2].position) // Mohamed (skips 2 -> 3)
        assertEquals(4L, ranked[3].position) // Ahmed (4)
        assertEquals(4L, ranked[4].position) // Ali (ties Ahmed -> 4)
        assertEquals(6L, ranked[5].position) // Khadar (skips 5 -> 6)

        assertEquals("A+", ranked[0].grade)
        assertEquals("A", ranked[5].grade)
    }

    @Test
    fun testLocalizationSomaliAndEnglish() {
        assertEquals("Maamulaha", AppStrings.get("admin", "so"))
        assertEquals("Administrator", AppStrings.get("admin", "en"))

        assertEquals("Waalidka", AppStrings.get("parent", "so"))
        assertEquals("Parent", AppStrings.get("parent", "en"))

        assertEquals("Jooga", AppStrings.get("present", "so"))
        assertEquals("Present", AppStrings.get("present", "en"))

        assertEquals("Maqan", AppStrings.get("absent", "so"))
        assertEquals("Absent", AppStrings.get("absent", "en"))
    }
}
