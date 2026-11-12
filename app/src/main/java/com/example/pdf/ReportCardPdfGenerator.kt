package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.models.ExamResult
import com.example.data.models.Examination
import com.example.data.models.SchoolConfig
import com.example.data.models.Student
import com.example.localization.AppStrings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportCardPdfGenerator {

    fun generateReportCard(
        context: Context,
        school: SchoolConfig,
        student: Student,
        exam: Examination,
        result: ExamResult,
        language: String = "so"
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (in points: 595 x 842)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Navy Blue Brand Color
        val navyColor = Color.rgb(26, 54, 93)
        val goldColor = Color.rgb(214, 158, 46)
        val lightGray = Color.rgb(240, 244, 248)
        val darkText = Color.rgb(30, 41, 59)
        val subtleText = Color.rgb(100, 116, 139)

        // Draw Header Border & Background
        paint.color = navyColor
        canvas.drawRect(0f, 0f, 595f, 110f, paint)

        // Gold Accent Line
        paint.color = goldColor
        paint.strokeWidth = 4f
        canvas.drawLine(0f, 110f, 595f, 110f, paint)

        // School Name & Header
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(school.name.uppercase(Locale.ROOT), 297.5f, 42f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(school.motto, 297.5f, 62f, paint)

        paint.textSize = 9f
        canvas.drawText("${school.address} | Tel: ${school.phone} | Email: ${school.email}", 297.5f, 82f, paint)

        // Title: Student Official Report Card
        paint.color = navyColor
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val reportTitle = if (language == "so") "KAARKA NATIIJADA ARDAYGA (OFFICIAL REPORT CARD)" else "OFFICIAL STUDENT REPORT CARD"
        canvas.drawText(reportTitle, 297.5f, 138f, paint)

        // Student Info Card Box
        paint.color = lightGray
        canvas.drawRoundRect(40f, 150f, 555f, 230f, 8f, 8f, paint)

        paint.color = navyColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(40f, 150f, 555f, 230f, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Student Details text
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 10f
        paint.color = darkText

        val nameLabel = if (language == "so") "Magaca Ardayga:" else "Student Name:"
        val idLabel = if (language == "so") "Aqoonsiga (ID):" else "Student ID:"
        val classLabel = if (language == "so") "Fasalka:" else "Class:"
        val yearLabel = if (language == "so") "Sannadka:" else "Academic Year:"
        val examLabel = if (language == "so") "Imtixaanka:" else "Examination:"
        val dateLabel = if (language == "so") "Taariikhda:" else "Report Date:"

        // Column 1
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(nameLabel, 55f, 175f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(student.fullName, 145f, 175f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(idLabel, 55f, 195f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(student.studentId, 145f, 195f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(classLabel, 55f, 215f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(student.className, 145f, 215f, paint)

        // Column 2
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(examLabel, 330f, 175f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(exam.name, 415f, 175f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(yearLabel, 330f, 195f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(exam.academicYear, 415f, 195f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(dateLabel, 330f, 215f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        canvas.drawText(formattedDate, 415f, 215f, paint)

        // Table Header
        val tableTop = 250f
        paint.color = navyColor
        canvas.drawRect(40f, tableTop, 555f, tableTop + 25f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(if (language == "so") "MAADDADA (SUBJECT)" else "SUBJECT", 55f, tableTop + 16f, paint)
        canvas.drawText(if (language == "so") "UGU BADAN (MAX)" else "MAX MARKS", 240f, tableTop + 16f, paint)
        canvas.drawText(if (language == "so") "LA HELAY (OBTAINED)" else "OBTAINED", 340f, tableTop + 16f, paint)
        canvas.drawText(if (language == "so") "HEERKA (GRADE)" else "GRADE", 460f, tableTop + 16f, paint)

        // Table Rows
        var currentY = tableTop + 25f
        paint.color = darkText
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val subjectList = exam.subjects
        for (i in subjectList.indices) {
            val sub = subjectList[i]
            val obtained = result.subjectMarks[sub.name] ?: 0.0

            // Alternating row background
            if (i % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(40f, currentY, 555f, currentY + 22f, paint)
            }

            // Row text
            paint.color = darkText
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(sub.name, 55f, currentY + 15f, paint)
            canvas.drawText("${sub.maxMarks}", 240f, currentY + 15f, paint)
            canvas.drawText("${obtained}", 340f, currentY + 15f, paint)

            val subPct = if (sub.maxMarks > 0) (obtained / sub.maxMarks) * 100 else 0.0
            val subGrade = when {
                subPct >= 90 -> "A+"
                subPct >= 80 -> "A"
                subPct >= 70 -> "B"
                subPct >= 60 -> "C"
                subPct >= 50 -> "D"
                else -> "F"
            }
            canvas.drawText(subGrade, 460f, currentY + 15f, paint)

            // Divider line
            paint.color = Color.rgb(226, 232, 240)
            paint.strokeWidth = 0.5f
            canvas.drawLine(40f, currentY + 22f, 555f, currentY + 22f, paint)

            currentY += 22f
        }

        // Total Row
        paint.color = Color.rgb(235, 248, 255)
        canvas.drawRect(40f, currentY, 555f, currentY + 26f, paint)
        paint.color = navyColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (language == "so") "WADARTA GUUD (TOTAL)" else "TOTAL", 55f, currentY + 17f, paint)
        canvas.drawText("${result.totalMax}", 240f, currentY + 17f, paint)
        canvas.drawText("${result.totalObtained}", 340f, currentY + 17f, paint)
        canvas.drawText("${result.grade}", 460f, currentY + 17f, paint)

        currentY += 35f

        // Performance Summary Badges (Percentage, Grade, Position / Competition Rank)
        paint.color = lightGray
        canvas.drawRoundRect(40f, currentY, 555f, currentY + 70f, 8f, 8f, paint)

        // Box 1: Percentage
        paint.color = navyColor
        paint.textSize = 11f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (language == "so") "BOQOLKIIBA (%)" else "PERCENTAGE", 120f, currentY + 25f, paint)
        paint.textSize = 18f
        paint.color = Color.rgb(43, 108, 176)
        canvas.drawText(String.format(Locale.US, "%.1f%%", result.percentage), 120f, currentY + 52f, paint)

        // Box 2: Overall Grade
        paint.color = navyColor
        paint.textSize = 11f
        canvas.drawText(if (language == "so") "HEERKA GUUD" else "OVERALL GRADE", 297f, currentY + 25f, paint)
        paint.textSize = 18f
        paint.color = if (result.grade == "F") Color.RED else Color.rgb(47, 133, 90)
        canvas.drawText(result.grade, 297f, currentY + 52f, paint)

        // Box 3: Competition Position
        paint.color = navyColor
        paint.textSize = 11f
        canvas.drawText(if (language == "so") "KAALINTA FASALKA" else "CLASS POSITION", 470f, currentY + 25f, paint)
        paint.textSize = 18f
        paint.color = goldColor
        canvas.drawText("#${result.position}", 470f, currentY + 52f, paint)

        currentY += 85f

        // Teacher Remarks Section
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(40f, currentY, 555f, currentY + 55f, 6f, 6f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 10f
        paint.color = navyColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (language == "so") "Faallada Macallinka:" else "Teacher Remarks:", 55f, currentY + 20f, paint)

        paint.color = darkText
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val remarksText = if (result.teacherRemarks.isNotBlank()) result.teacherRemarks else "Arday dadaal badan oo mustaqbal fiican leh."
        canvas.drawText(remarksText, 55f, currentY + 40f, paint)

        currentY += 80f

        // Signatures & Official Stamp section
        paint.color = subtleText
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.strokeWidth = 1f
        canvas.drawLine(60f, currentY + 30f, 200f, currentY + 30f, paint)
        canvas.drawLine(395f, currentY + 30f, 535f, currentY + 30f, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 9f
        canvas.drawText(if (language == "so") "Saxiixa Macallinka Fasalka" else "Class Teacher Signature", 130f, currentY + 45f, paint)
        canvas.drawText(if (language == "so") "Saxiixa & Shaabadda Maamulka" else "Principal Signature & Stamp", 465f, currentY + 45f, paint)

        // Footer note
        paint.textSize = 8f
        paint.color = subtleText
        val footerText = if (language == "so") "Warbixintan waxaa si rasmi ah u soo saaray Dugsiga Iftin Academy. Wax ka beddelku waa mamnuuc." else "This report was officially generated by Iftin Academy. Any alteration renders it void."
        canvas.drawText(footerText, 297.5f, 820f, paint)

        pdfDocument.finishPage(page)

        // Save PDF to cache
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val fileName = "ReportCard_${student.studentId}_${exam.name.replace(" ", "_")}.pdf"
        val outputFile = File(reportsDir, fileName)

        return try {
            val fos = FileOutputStream(outputFile)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun shareReportCardPdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Report Card"))
    }
}
