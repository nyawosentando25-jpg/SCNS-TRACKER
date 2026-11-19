package com.example.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.R
import com.example.data.model.AttendanceEntry
import com.example.data.model.ChairpersonConfig
import com.example.data.model.ExamEntry
import com.example.data.model.LearnerRecord
import com.example.data.model.ScnsCalculators
import com.example.data.model.ScnsConstants
import com.example.data.model.ScnsSubjectCatalog
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PdfExportScope(val fileBase: String, val title: String) {
    ALL("SCNS_All_Stored_Info", "SCNS Complete Learner Records"),
    LEARNERS("SCNS_Learners_List", "SCNS Learner List"),
    ATTENDANCE("SCNS_Attendance_Register", "SCNS Attendance Register"),
    PAYMENTS("SCNS_Payment_Tracker", "SCNS Payment Tracker"),
    EXAMS_RESULTS("SCNS_Exams_Results", "SCNS Exams Results Table"),
    EXAMS_PROGRESS("SCNS_Exams_Progress", "SCNS Exams Progress Summary"),
    REGISTERED_MARKS("SCNS_Registered_Marks", "SCNS Registered Learners Marks")
}

object ScnsPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 28f
    private const val NAVY = 0xFF1A2A5E.toInt()
    private const val GOLD = 0xFFF5C542.toInt()
    private const val EMERALD = 0xFF138D75.toInt()
    private const val CRIMSON = 0xFFC0392B.toInt()
    private const val MUTED = 0xFF5A6582.toInt()
    private const val ROW_ALT = 0xFFF4F6FB.toInt()
    private const val BORDER = 0xFFD5DCEB.toInt()

    private fun todayStr(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    private fun newPage(doc: PdfDocument, pageNum: Int): PdfDocument.Page =
        doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())

    private fun drawHeader(
        context: Context,
        canvas: Canvas,
        title: String,
        subtitle: String
    ): Float {
        val navyPaint = Paint().apply { color = NAVY; isAntiAlias = true }
        val goldPaint = Paint().apply { color = GOLD; isAntiAlias = true }
        val whitePaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 15f
        }
        val subPaint = Paint().apply {
            color = 0xFFE6ECFF.toInt()
            isAntiAlias = true
            textSize = 9.5f
        }

        canvas.drawRoundRect(
            RectF(MARGIN, 22f, PAGE_WIDTH - MARGIN, 86f),
            10f, 10f, navyPaint
        )
        canvas.drawRect(MARGIN, 82f, PAGE_WIDTH - MARGIN, 86f, goldPaint)

        runCatching {
            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.scns_logo)
            if (bmp != null) {
                val dest = RectF(MARGIN + 10f, 30f, MARGIN + 56f, 76f)
                canvas.drawOval(
                    RectF(dest.left - 2f, dest.top - 2f, dest.right + 2f, dest.bottom + 2f),
                    goldPaint
                )
                canvas.drawBitmap(bmp, null, dest, null)
            }
        }

        val textX = MARGIN + 68f
        canvas.drawText(ScnsConstants.ORG_NAME, textX, 44f, whitePaint)
        whitePaint.textSize = 11.5f
        whitePaint.color = GOLD
        canvas.drawText(title, textX, 60f, whitePaint)
        canvas.drawText("$subtitle  •  Generated: ${todayStr()}", textX, 74f, subPaint)

        return 100f
    }

    private fun drawAuthorisationFooter(
        canvas: Canvas,
        config: ChairpersonConfig,
        pageNumber: Int
    ) {
        val top = PAGE_HEIGHT - 74f
        val boxPaint = Paint().apply {
            color = 0xFFFFFBF0.toInt()
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = NAVY
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val boldPaint = Paint().apply {
            color = NAVY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 8f
            isAntiAlias = true
        }

        val rect = RectF(MARGIN, top, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 22f)
        canvas.drawRoundRect(rect, 6f, 6f, boxPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)
        canvas.drawText(
            "OFFICE USE AUTHORISATION — ${ScnsConstants.ORG_NAME} (${ScnsConstants.VENUE})",
            MARGIN + 10f, top + 14f, boldPaint
        )
        canvas.drawText(
            "Chairperson: ${config.chairName} (${config.chairCell})   |   Bank Ref: ${ScnsConstants.BANK_REF}",
            MARGIN + 10f, top + 27f, textPaint
        )
        canvas.drawText(
            "Authorised Signature: ________________________   Date: ______________   Page $pageNumber",
            MARGIN + 10f, top + 42f, textPaint
        )
    }

    private fun drawTablePdf(
        context: Context,
        title: String,
        subtitle: String,
        headers: List<String>,
        colFractions: List<Float>,
        rows: List<List<String>>,
        config: ChairpersonConfig
    ): ByteArray {
        val doc = PdfDocument()
        val usableWidth = PAGE_WIDTH - MARGIN * 2f
        val rowHeight = 20f
        val maxY = PAGE_HEIGHT - 88f
        var pageNum = 1
        var page = newPage(doc, pageNum)
        var canvas = page.canvas
        var y = drawHeader(context, canvas, title, subtitle)

        val headerBg = Paint().apply { color = NAVY }
        val headerText = Paint().apply {
            color = Color.WHITE
            textSize = 8.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val cellText = Paint().apply {
            color = 0xFF182038.toInt()
            textSize = 7.8f
            isAntiAlias = true
        }
        val altPaint = Paint().apply { color = ROW_ALT }
        val linePaint = Paint().apply { color = BORDER; strokeWidth = 0.6f }

        fun drawTableHeader() {
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBg)
            var x = MARGIN + 4f
            headers.forEachIndexed { idx, h ->
                val w = usableWidth * colFractions[idx]
                canvas.drawText(clipText(h, headerText, w - 6f), x, y + 13.5f, headerText)
                x += w
            }
            y += rowHeight
        }

        drawTableHeader()

        if (rows.isEmpty()) {
            canvas.drawText("No records matched this view.", MARGIN + 6f, y + 18f, cellText)
        } else {
            rows.forEachIndexed { rowIdx, row ->
                if (y + rowHeight > maxY) {
                    drawAuthorisationFooter(canvas, config, pageNum)
                    doc.finishPage(page)
                    pageNum++
                    page = newPage(doc, pageNum)
                    canvas = page.canvas
                    y = drawHeader(context, canvas, title, subtitle)
                    drawTableHeader()
                }
                if (rowIdx % 2 == 1) {
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, altPaint)
                }
                var x = MARGIN + 4f
                row.forEachIndexed { colIdx, value ->
                    val w = usableWidth * colFractions[colIdx]
                    canvas.drawText(clipText(value, cellText, w - 6f), x, y + 13.5f, cellText)
                    x += w
                }
                canvas.drawLine(MARGIN, y + rowHeight, PAGE_WIDTH - MARGIN, y + rowHeight, linePaint)
                y += rowHeight
            }
        }

        drawAuthorisationFooter(canvas, config, pageNum)
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    fun generateScopePdf(
        context: Context,
        scope: PdfExportScope,
        learners: List<LearnerRecord>,
        config: ChairpersonConfig,
        subjectFilter: String = "",
        gradeFilter: String = "",
        termFilter: String = "Term 1"
    ): ByteArray {
        return when (scope) {
            PdfExportScope.LEARNERS -> {
                val headers = listOf("Reg No", "Learner Name", "Gr", "Subjects", "Status", "Dep", "Paid", "Owing")
                val widths = listOf(0.16f, 0.22f, 0.05f, 0.23f, 0.11f, 0.06f, 0.09f, 0.08f)
                val rows = learners.map { l ->
                    val due = ScnsCalculators.totalDue(l.subjects)
                    val paid = ScnsCalculators.paidTotal(l)
                    val bal = (due - paid).coerceAtLeast(0L)
                    listOf(
                        l.regNo, "${l.surname}, ${l.names}", l.grade, l.subjects.ifBlank { "-" },
                        l.status, if (l.depPaid) "Yes" else "No", "R$paid", "R$bal"
                    )
                }
                drawTablePdf(context, scope.title, "${learners.size} Learners • ${ScnsConstants.VENUE}", headers, widths, rows, config)
            }

            PdfExportScope.ATTENDANCE -> {
                val headers = listOf("Reg No", "Learner Name", "Gr", "Date", "Day", "Subject", "Status")
                val widths = listOf(0.16f, 0.23f, 0.05f, 0.13f, 0.12f, 0.21f, 0.10f)
                val rows = buildList {
                    for (l in learners) {
                        val fallback = ScnsCalculators.primarySubjectOf(l)
                        for (a in l.att) {
                            add(
                                listOf(
                                    l.regNo, "${l.surname}, ${l.names}", l.grade,
                                    a.d, a.day,
                                    ScnsSubjectCatalog.shortLabel(a.subject.ifBlank { fallback }),
                                    a.s
                                )
                            )
                        }
                    }
                }
                drawTablePdf(context, scope.title, "${rows.size} Entries • ${ScnsConstants.VENUE}", headers, widths, rows, config)
            }

            PdfExportScope.PAYMENTS -> {
                val headers = listOf("Reg No", "Learner", "Gr", "Monthly", "Termly", "Total Paid", "Due", "Balance")
                val widths = listOf(0.16f, 0.24f, 0.05f, 0.11f, 0.11f, 0.11f, 0.11f, 0.11f)
                val rows = learners.map { l ->
                    val m = ScnsCalculators.monthlyPaid(l)
                    val t = ScnsCalculators.termlyPaid(l)
                    val total = ScnsCalculators.paidTotal(l)
                    val due = ScnsCalculators.totalDue(l.subjects)
                    val bal = (due - total).coerceAtLeast(0L)
                    listOf(l.regNo, "${l.surname}, ${l.names}", l.grade, "R$m", "R$t", "R$total", "R$due", "R$bal")
                }
                val collected = learners.sumOf { ScnsCalculators.paidTotal(it) }
                drawTablePdf(context, scope.title, "Total Collected: R$collected • ${ScnsConstants.BANK_REF}", headers, widths, rows, config)
            }

            PdfExportScope.EXAMS_RESULTS, PdfExportScope.EXAMS_PROGRESS, PdfExportScope.REGISTERED_MARKS -> {
                val headers = listOf("Reg No", "Learner", "Gr", "Subject", "Term", "T1/T2/T3/T4", "Total", "% / Rating")
                val widths = listOf(0.15f, 0.21f, 0.05f, 0.17f, 0.08f, 0.14f, 0.08f, 0.12f)
                val filteredLearners = learners.filter { l ->
                    (gradeFilter.isBlank() || l.grade == gradeFilter) &&
                        (subjectFilter.isBlank() || ScnsCalculators.learnerMatchesSubject(l, subjectFilter))
                }
                val rows = buildList {
                    for (l in filteredLearners) {
                        for (e in l.exams.values) {
                            val matchSubj = subjectFilter.isBlank() ||
                                ScnsSubjectCatalog.normalizeSubject(e.subject).equals(
                                    ScnsSubjectCatalog.normalizeSubject(subjectFilter), ignoreCase = true
                                )
                            val matchTerm = scope != PdfExportScope.EXAMS_RESULTS || e.term == termFilter
                            if (matchSubj && matchTerm) {
                                val st = ScnsCalculators.computeExamStats(e)
                                add(
                                    listOf(
                                        l.regNo, "${l.surname}, ${l.names}", l.grade,
                                        ScnsSubjectCatalog.shortLabel(e.subject), e.term,
                                        "${e.t1}/${e.t2}/${e.t3}/${e.t4}",
                                        "${st.total}/400",
                                        "${st.percentage}% ${st.ratingLabel.substringBefore(" ")}"
                                    )
                                )
                            }
                        }
                    }
                }
                drawTablePdf(context, scope.title, "Filter: Gr ${gradeFilter.ifBlank { "All" }} • ${subjectFilter.ifBlank { "All Subjects" }}", headers, widths, rows, config)
            }

            PdfExportScope.ALL -> {
                val headers = listOf("Reg No", "Learner Name", "Gr", "Subjects", "Status", "Att", "Paid", "Owing")
                val widths = listOf(0.16f, 0.22f, 0.05f, 0.21f, 0.11f, 0.07f, 0.09f, 0.09f)
                val rows = learners.map { l ->
                    val due = ScnsCalculators.totalDue(l.subjects)
                    val paid = ScnsCalculators.paidTotal(l)
                    val bal = (due - paid).coerceAtLeast(0L)
                    val presentCount = l.att.count { it.s.equals("Present", ignoreCase = true) }
                    listOf(
                        l.regNo, "${l.surname}, ${l.names}", l.grade, l.subjects.ifBlank { "-" },
                        l.status, "${presentCount}/${l.att.size}", "R$paid", "R$bal"
                    )
                }
                val collected = learners.sumOf { ScnsCalculators.paidTotal(it) }
                drawTablePdf(context, scope.title, "${learners.size} Learners • Total Collected: R$collected", headers, widths, rows, config)
            }
        }
    }

    fun generateFilteredAttendancePdf(
        context: Context,
        title: String,
        subtitle: String,
        entries: List<Pair<LearnerRecord, AttendanceEntry>>,
        config: ChairpersonConfig
    ): ByteArray {
        val headers = listOf("Reg No", "Learner Name", "Gr", "Date", "Day", "Subject", "Status")
        val widths = listOf(0.16f, 0.23f, 0.05f, 0.13f, 0.12f, 0.21f, 0.10f)
        val rows = entries.map { (l, a) ->
            listOf(
                l.regNo, "${l.surname}, ${l.names}", l.grade, a.d, a.day,
                ScnsSubjectCatalog.shortLabel(a.subject.ifBlank { ScnsCalculators.primarySubjectOf(l) }),
                a.s
            )
        }
        return drawTablePdf(context, title, subtitle, headers, widths, rows, config)
    }

    fun generateIndividualLearnerPdf(
        context: Context,
        learner: LearnerRecord,
        config: ChairpersonConfig
    ): ByteArray {
        val doc = PdfDocument()
        val page = newPage(doc, 1)
        val canvas = page.canvas
        var y = drawHeader(
            context,
            canvas,
            "Individual Learner Record — ${learner.regNo}",
            "${learner.names} ${learner.surname} • Grade ${learner.grade}"
        )

        val sectionPaint = Paint().apply {
            color = NAVY
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = 0xFF1C2338.toInt()
            textSize = 9.5f
            isAntiAlias = true
        }
        val boxBg = Paint().apply { color = ROW_ALT }
        val borderPaint = Paint().apply {
            color = BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val due = ScnsCalculators.totalDue(learner.subjects)
        val paid = ScnsCalculators.paidTotal(learner)
        val bal = (due - paid).coerceAtLeast(0L)

        val infoRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 104f)
        canvas.drawRoundRect(infoRect, 8f, 8f, boxBg)
        canvas.drawRoundRect(infoRect, 8f, 8f, borderPaint)
        canvas.drawText("LEARNER PROFILE & FEES", MARGIN + 10f, y + 16f, sectionPaint)
        canvas.drawText("Full Name: ${learner.names} ${learner.surname}    |    Reg No: ${learner.regNo}    |    Grade: ${learner.grade}", MARGIN + 10f, y + 32f, bodyPaint)
        canvas.drawText("School: ${learner.school.ifBlank { "-" }}    |    Sex: ${learner.sex.ifBlank { "-" }}    |    Age: ${learner.age.ifBlank { "-" }}", MARGIN + 10f, y + 47f, bodyPaint)
        canvas.drawText("Subjects: ${learner.subjects.ifBlank { "-" }}", MARGIN + 10f, y + 62f, bodyPaint)
        canvas.drawText("Status: ${learner.status}    |    Deposit R700: ${if (learner.depPaid) "Yes (${learner.depDate})" else "No"}", MARGIN + 10f, y + 77f, bodyPaint)
        canvas.drawText("Total Due: R$due   |   Total Paid: R$paid   |   Outstanding: R$bal   |   Parent: ${learner.parent.ifBlank { "-" }}", MARGIN + 10f, y + 92f, bodyPaint)

        y += 120f
        canvas.drawText("RECENT ATTENDANCE (${learner.att.size} records)", MARGIN, y, sectionPaint)
        y += 14f
        if (learner.att.isEmpty()) {
            canvas.drawText("No attendance entries recorded.", MARGIN + 6f, y, bodyPaint)
            y += 18f
        } else {
            learner.att.takeLast(10).forEach { a ->
                canvas.drawText("• ${a.d} (${a.day}) — ${a.subject.ifBlank { "General" }} : ${a.s}", MARGIN + 6f, y, bodyPaint)
                y += 13f
            }
        }

        y += 12f
        canvas.drawText("PAYMENT HISTORY (${learner.pay.size} payments)", MARGIN, y, sectionPaint)
        y += 14f
        if (learner.pay.isEmpty()) {
            canvas.drawText("No payments recorded yet.", MARGIN + 6f, y, bodyPaint)
            y += 18f
        } else {
            learner.pay.takeLast(10).forEach { p ->
                val period = if (p.t.equals("Monthly", true)) p.month else p.term
                val notePart = if (p.note.isNotBlank()) " [${p.note}]" else ""
                canvas.drawText("• ${p.d} — ${p.t} ($period): R${p.amt}$notePart", MARGIN + 6f, y, bodyPaint)
                y += 13f
            }
        }

        y += 12f
        canvas.drawText("EXAMS SUMMARY (${learner.exams.size} entries)", MARGIN, y, sectionPaint)
        y += 14f
        if (learner.exams.isEmpty()) {
            canvas.drawText("No exam marks recorded yet.", MARGIN + 6f, y, bodyPaint)
        } else {
            learner.exams.values.take(6).forEach { e ->
                val st = ScnsCalculators.computeExamStats(e)
                canvas.drawText(
                    "• ${e.subject} (${e.term}): T1=${e.t1}, T2=${e.t2}, T3=${e.t3}, T4=${e.t4}  →  ${st.total}/400 (${st.percentage}% - ${st.ratingLabel})",
                    MARGIN + 6f, y, bodyPaint
                )
                y += 13f
            }
        }

        drawAuthorisationFooter(canvas, config, 1)
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    fun generateIndividualPaymentPdf(
        context: Context,
        learner: LearnerRecord,
        config: ChairpersonConfig
    ): ByteArray {
        val headers = listOf("Date", "Type", "Month / Term", "Amount", "Note / Receipt")
        val widths = listOf(0.16f, 0.15f, 0.22f, 0.15f, 0.32f)
        val rows = learner.pay.map { p ->
            val period = if (p.t.equals("Monthly", true)) p.month else p.term
            listOf(p.d, p.t, period, "R${p.amt}", p.note.ifBlank { "-" })
        }
        val due = ScnsCalculators.totalDue(learner.subjects)
        val paid = ScnsCalculators.paidTotal(learner)
        val bal = (due - paid).coerceAtLeast(0L)
        return drawTablePdf(
            context,
            "Payment Statement — ${learner.names} ${learner.surname}",
            "${learner.regNo} • Gr ${learner.grade} • Paid: R$paid / Due: R$due • Balance: R$bal",
            headers, widths, rows, config
        )
    }

    /**
     * Exact One-Page Individual School Progress Report PDF for SCNS Tutorial.
     */
    fun generateSchoolProgressReportPdf(
        context: Context,
        learner: LearnerRecord,
        subject: String,
        term: String,
        config: ChairpersonConfig
    ): ByteArray {
        val canonicalSubject = ScnsSubjectCatalog.normalizeSubject(subject)
        val key = ScnsCalculators.examKey(canonicalSubject, term)
        val entry = learner.exams[key] ?: ExamEntry(subject = canonicalSubject, term = term)
        val stats = ScnsCalculators.computeExamStats(entry)

        val doc = PdfDocument()
        val page = newPage(doc, 1)
        val canvas = page.canvas

        var y = drawHeader(
            context,
            canvas,
            "OFFICIAL LEARNER SCHOOL PROGRESS REPORT",
            "$canonicalSubject • $term • ${ScnsConstants.VENUE}"
        )

        val sectionPaint = Paint().apply {
            color = NAVY
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = MUTED
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = 0xFF141B33.toInt()
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val boxBg = Paint().apply { color = ROW_ALT }
        val borderPaint = Paint().apply {
            color = NAVY
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }

        // 1. Learner Particulars Card
        val profileRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 96f)
        canvas.drawRoundRect(profileRect, 8f, 8f, boxBg)
        canvas.drawRoundRect(profileRect, 8f, 8f, borderPaint)

        canvas.drawText("LEARNER DETAILS", MARGIN + 12f, y + 18f, sectionPaint)
        canvas.drawText("Reg No:", MARGIN + 12f, y + 36f, labelPaint)
        canvas.drawText(learner.regNo, MARGIN + 65f, y + 36f, valuePaint)
        canvas.drawText("Grade:", 330f, y + 36f, labelPaint)
        canvas.drawText("Grade ${learner.grade}", 378f, y + 36f, valuePaint)

        canvas.drawText("Learner:", MARGIN + 12f, y + 54f, labelPaint)
        canvas.drawText("${learner.names} ${learner.surname}", MARGIN + 65f, y + 54f, valuePaint)
        canvas.drawText("School:", 330f, y + 54f, labelPaint)
        canvas.drawText(learner.school.ifBlank { "-" }, 378f, y + 54f, valuePaint)

        canvas.drawText("Subject:", MARGIN + 12f, y + 72f, labelPaint)
        canvas.drawText(canonicalSubject, MARGIN + 65f, y + 72f, valuePaint)
        canvas.drawText("Term:", 330f, y + 72f, labelPaint)
        canvas.drawText("$term (${entry.date.ifBlank { todayStr().take(10) }})", 378f, y + 72f, valuePaint)

        canvas.drawText("Parent/Guardian: ${learner.parent.ifBlank { "-" }}", MARGIN + 12f, y + 89f, labelPaint)

        y += 116f

        // 2. Assessment Breakdown (4 Tests out of 100)
        canvas.drawText("ASSESSMENT SCORES (EACH OUT OF 100)", MARGIN, y, sectionPaint)
        y += 12f

        val cardGap = 10f
        val cardW = (PAGE_WIDTH - MARGIN * 2f - cardGap * 3f) / 4f
        val scores = listOf("Test 1" to entry.t1, "Test 2" to entry.t2, "Test 3" to entry.t3, "Test 4" to entry.t4)
        scores.forEachIndexed { idx, (label, score) ->
            val left = MARGIN + idx * (cardW + cardGap)
            val rect = RectF(left, y, left + cardW, y + 66f)
            canvas.drawRoundRect(rect, 8f, 8f, Paint().apply { color = Color.WHITE })
            canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
            canvas.drawText(label, left + 12f, y + 20f, labelPaint)
            val scorePaint = Paint().apply {
                color = if (score >= 50) NAVY else CRIMSON
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("$score / 100", left + 12f, y + 48f, scorePaint)
        }

        y += 88f

        // 3. Summary & Rating Banner
        val ratingColor = when {
            stats.percentage >= 70 -> EMERALD
            stats.percentage >= 50 -> 0xFFD9820B.toInt()
            else -> CRIMSON
        }
        val summaryRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 78f)
        canvas.drawRoundRect(summaryRect, 10f, 10f, Paint().apply { color = 0xFFFFFDE7.toInt() })
        canvas.drawRoundRect(summaryRect, 10f, 10f, borderPaint)

        canvas.drawText("TOTAL MARKS:  ${stats.total} / 400", MARGIN + 14f, y + 26f, sectionPaint)
        val pctPaint = Paint().apply {
            color = ratingColor
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("${stats.percentage}%", MARGIN + 14f, y + 58f, pctPaint)

        val pillRect = RectF(220f, y + 22f, PAGE_WIDTH - MARGIN - 14f, y + 58f)
        canvas.drawRoundRect(pillRect, 8f, 8f, Paint().apply { color = ratingColor })
        val pillTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Performance Rating: ${stats.ratingLabel}", 234f, y + 45f, pillTextPaint)

        y += 102f

        // 4. Tutor & Parent Comments + Signatures
        canvas.drawText("EDUCATOR / TUTOR FEEDBACK & SIGNATURES", MARGIN, y, sectionPaint)
        y += 18f
        val linePaint = Paint().apply { color = 0xFF7B87A6.toInt(); strokeWidth = 0.9f }
        canvas.drawText("Tutor Comment:", MARGIN, y, labelPaint)
        canvas.drawLine(MARGIN + 90f, y + 2f, PAGE_WIDTH - MARGIN, y + 2f, linePaint)
        y += 26f
        canvas.drawLine(MARGIN, y + 2f, PAGE_WIDTH - MARGIN, y + 2f, linePaint)
        y += 32f

        canvas.drawText("Parent / Guardian Comment:", MARGIN, y, labelPaint)
        canvas.drawLine(MARGIN + 140f, y + 2f, PAGE_WIDTH - MARGIN, y + 2f, linePaint)
        y += 26f
        canvas.drawLine(MARGIN, y + 2f, PAGE_WIDTH - MARGIN, y + 2f, linePaint)
        y += 40f

        canvas.drawText("Tutor Signature: __________________________", MARGIN, y, bodyLabelPaint())
        canvas.drawText("Parent Signature: __________________________", 315f, y, bodyLabelPaint())
        y += 24f
        canvas.drawText("Official SCNS Stamp / Verification Date: ________________________", MARGIN, y, bodyLabelPaint())

        drawAuthorisationFooter(canvas, config, 1)
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun bodyLabelPaint() = Paint().apply {
        color = NAVY
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private fun clipText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        val bounds = Rect()
        while (end > 1) {
            val candidate = text.substring(0, end) + "…"
            paint.getTextBounds(candidate, 0, candidate.length, bounds)
            if (paint.measureText(candidate) <= maxWidth) return candidate
            end--
        }
        return text.take(1)
    }
}
