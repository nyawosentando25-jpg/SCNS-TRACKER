package com.example.data.model

import kotlin.math.roundToInt

object ScnsConstants {
    const val ORG_NAME = "SCNS TUTORIAL"
    const val ORG_SUBTITLE = "SEKWANELE CROSSNIGHTS • EST. 2027"
    const val MOTTO_ZULU = "✦ Woza 2027 sophasa ngenkani ✦"
    const val MOTTO_ENGLISH = "Crush Exams, Own Success!"
    const val VENUE = "eZimisele Primary, Umlazi D"
    const val BANK_REF = "CAPITEC SCNS TUTORIAL 2336973185"
    const val CREATOR_CREDIT = "Ntando Nyawose"

    const val DEFAULT_CHAIR_NAME = "Mr Thembelani Sithole"
    const val DEFAULT_CHAIR_CELL = "0813600636"
    const val MAX_EXECUTIVES = 4
    const val DEPOSIT_AMOUNT_ZAR = 700L
    const val SEVEN_DAYS_MS = 7L * 24L * 60L * 60L * 1000L

    val GRADES = listOf("8", "9", "10", "11", "12")
    val DAYS = listOf("Saturday", "Sunday")
    val STATUSES = listOf("Accepted", "Rejected", "Waiting List")
    val TERMS = listOf("Term 1", "Term 2", "Term 3", "Term 4")
    val MONTHS_2027 = listOf(
        "Jan 2027", "Feb 2027", "Mar 2027", "Apr 2027", "May 2027",
        "Jun 2027", "Jul 2027", "Aug 2027", "Sep 2027", "Oct 2027"
    )
    val EXECUTIVE_ROLES = listOf(
        "Chairperson",
        "Secretary",
        "Treasurer",
        "Marketing & Media Officer / Coordinator"
    )
    val OTP_SENDERS = listOf("Chairperson", "Secretary", "Treasurer", "Any Executive")

    val SAFETY_CHIPS = listOf(
        "eZimisele Primary, Umlazi D",
        "Safety First",
        "SAPS Visits",
        "On-Premises Security"
    )
}

data class ScnsSubject(
    val canonical: String,
    val shortName: String,
    val validGrades: Set<String>,
    val gradeLabel: String
)

object ScnsSubjectCatalog {
    val ALL_SUBJECTS = listOf(
        ScnsSubject("Mathematics", "Maths", setOf("8", "9", "10", "11", "12"), "Grade 8 to 12"),
        ScnsSubject("Mathematical Literacy", "Maths Lit", setOf("10", "11", "12"), "Grade 10 to 12"),
        ScnsSubject("Life Sciences", "Life Sci", setOf("10", "11", "12"), "Grade 10 to 12"),
        ScnsSubject("Physical Sciences", "Physical Sci", setOf("10", "11", "12"), "Grade 10 to 12"),
        ScnsSubject("Accounting", "Accounting", setOf("10", "11", "12"), "Grade 10 to 12"),
        ScnsSubject("Economic and Management Sciences", "EMS", setOf("8", "9"), "Grade 8 to 9"),
        ScnsSubject("Natural Sciences", "NS", setOf("8", "9"), "Grade 8 to 9")
    )

    fun subjectsForGrade(grade: String): List<ScnsSubject> {
        if (grade.isBlank()) return ALL_SUBJECTS
        return ALL_SUBJECTS.filter { grade in it.validGrades }
    }

    fun normalizeSubject(raw: String): String {
        val cleaned = raw.trim().lowercase()
        return when (cleaned) {
            "maths", "math", "mathematics" -> "Mathematics"
            "maths lit", "math lit", "mathematical literacy", "maths literacy" -> "Mathematical Literacy"
            "ems", "economic and management sciences", "economics and management sciences" -> "Economic and Management Sciences"
            "ns", "natural sciences", "natural science" -> "Natural Sciences"
            "life sci", "life science", "life sciences" -> "Life Sciences"
            "physical sci", "physical science", "physical sciences", "physics" -> "Physical Sciences"
            "acc", "accounting" -> "Accounting"
            else -> raw.trim()
        }
    }

    fun shortLabel(canonicalOrRaw: String): String {
        val canonical = normalizeSubject(canonicalOrRaw)
        return ALL_SUBJECTS.firstOrNull { it.canonical.equals(canonical, ignoreCase = true) }?.shortName
            ?: canonicalOrRaw.trim()
    }

    fun isSubjectValidForGrade(subject: String, grade: String): Boolean {
        if (grade.isBlank() || subject.isBlank()) return true
        val canonical = normalizeSubject(subject)
        val found = ALL_SUBJECTS.firstOrNull { it.canonical.equals(canonical, ignoreCase = true) } ?: return true
        return grade in found.validGrades
    }

    fun defaultGradeForSubject(subject: String): String {
        val canonical = normalizeSubject(subject)
        val found = ALL_SUBJECTS.firstOrNull { it.canonical.equals(canonical, ignoreCase = true) }
        return found?.validGrades?.firstOrNull() ?: "10"
    }
}

data class ExamStats(
    val total: Int,
    val percentage: Int,
    val ratingLabel: String
)

object ScnsCalculators {

    fun parseSubjectList(subjectsCsv: String): List<String> =
        subjectsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * Exact SCNS yearly tuition fee formula:
     * n = max(1, subjectsCount)
     * fee = n * R2,400 + R100 registration - (R500 bundle discount if n >= 3)
     */
    fun totalDue(subjectsCsv: String): Long {
        val count = parseSubjectList(subjectsCsv).size.coerceAtLeast(1).toLong()
        val raw = count * 2400L + 100L
        return if (count >= 3L) raw - 500L else raw
    }

    fun paidTotal(learner: LearnerRecord): Long =
        learner.pay.sumOf { it.amt.coerceAtLeast(0L) }

    fun monthlyPaid(learner: LearnerRecord): Long =
        learner.pay.filter { it.t.equals("Monthly", ignoreCase = true) }.sumOf { it.amt.coerceAtLeast(0L) }

    fun termlyPaid(learner: LearnerRecord): Long =
        learner.pay.filter { !it.t.equals("Monthly", ignoreCase = true) }.sumOf { it.amt.coerceAtLeast(0L) }

    fun outstanding(learner: LearnerRecord): Long =
        (totalDue(learner.subjects) - paidTotal(learner)).coerceAtLeast(0L)

    fun isOwing(learner: LearnerRecord): Boolean =
        totalDue(learner.subjects) - paidTotal(learner) > 0L

    fun nextRegNo(existing: List<LearnerRecord>): String {
        val maxNum = existing.maxOfOrNull { learner ->
            val match = Regex("""(\d+)$""").find(learner.regNo.trim())
            match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        } ?: 0
        return "SCNS-2027-" + (maxNum + 1).toString().padStart(3, '0')
    }

    fun examKey(subject: String, term: String): String {
        val normSubj = ScnsSubjectCatalog.normalizeSubject(subject)
            .replace(Regex("""[^A-Za-z0-9]+"""), "_")
            .trim('_')
        val normTerm = term.trim().replace(Regex("""[^A-Za-z0-9]+"""), "_").trim('_')
        return "${normSubj}_${normTerm}"
    }

    fun computeExamStats(entry: ExamEntry): ExamStats {
        val t1 = entry.t1.coerceIn(0, 100)
        val t2 = entry.t2.coerceIn(0, 100)
        val t3 = entry.t3.coerceIn(0, 100)
        val t4 = entry.t4.coerceIn(0, 100)
        val total = t1 + t2 + t3 + t4
        val pct = ((total / 400.0) * 100.0).roundToInt()
        val rating = when {
            pct >= 80 -> "Excellent"
            pct >= 70 -> "Very Good"
            pct >= 60 -> "Good"
            pct >= 50 -> "Fair - Needs Support"
            else -> "At Risk - Extra Help"
        }
        return ExamStats(total = total, percentage = pct, ratingLabel = rating)
    }

    fun learnerMatchesSubject(learner: LearnerRecord, filterSubject: String): Boolean {
        if (filterSubject.isBlank()) return true
        val target = ScnsSubjectCatalog.normalizeSubject(filterSubject)
        val learnerSubjects = parseSubjectList(learner.subjects).map { ScnsSubjectCatalog.normalizeSubject(it) }
        return learnerSubjects.any { it.equals(target, ignoreCase = true) }
    }

    fun primarySubjectOf(learner: LearnerRecord): String =
        parseSubjectList(learner.subjects).firstOrNull()?.let { ScnsSubjectCatalog.normalizeSubject(it) }
            ?: "Mathematics"

    /**
     * Parses pasted/typed batch learner list lines:
     * strips leading numbers, dots, parentheses, dashes (e.g., "1. Ndlovu Sipho" -> "Ndlovu" + "Sipho")
     */
    fun parseBatchLines(rawText: String): List<Pair<String, String>> {
        return rawText.lineSequence()
            .map { line ->
                line.trim()
                    .replace(Regex("""^\d+[\).\s-]+"""), "")
                    .trim()
            }
            .filter { it.isNotEmpty() }
            .mapNotNull { cleanLine ->
                val parts = cleanLine.split(Regex("""\s+""")).filter { it.isNotBlank() }
                when {
                    parts.isEmpty() -> null
                    parts.size == 1 -> parts[0] to parts[0]
                    else -> parts[0] to parts.drop(1).joinToString(" ")
                }
            }
            .toList()
    }

    fun deduplicateAttendance(
        entries: List<AttendanceEntry>,
        fallbackSubject: String
    ): Pair<List<AttendanceEntry>, Int> {
        val seen = linkedSetOf<String>()
        val kept = mutableListOf<AttendanceEntry>()
        var removed = 0
        for (item in entries) {
            val subj = ScnsSubjectCatalog.normalizeSubject(
                item.subject.ifBlank { fallbackSubject }
            )
            val key = "${item.d.trim()}|${item.day.trim()}|${subj.lowercase()}"
            if (seen.add(key)) {
                kept.add(item.copy(subject = subj))
            } else {
                removed++
            }
        }
        return kept to removed
    }

    fun normalizeCell(raw: String): String =
        raw.replace(Regex("""\s+|-|\(|\)"""), "").trim()

    fun isChairpersonMatch(
        name: String,
        surname: String,
        cell: String,
        config: ChairpersonConfig
    ): Boolean {
        val cleanInputCell = normalizeCell(cell)
        val cleanChairCell = normalizeCell(config.chairCell)
        val defaultCell = normalizeCell(ScnsConstants.DEFAULT_CHAIR_CELL)
        val cleanPrev = normalizeCell(config.prevChairCell)
        val withinGrace = cleanPrev.isNotEmpty() && System.currentTimeMillis() <= config.prevCellUntilMs
        val cellMatches = cleanInputCell == cleanChairCell ||
            cleanInputCell == defaultCell ||
            (withinGrace && cleanInputCell == cleanPrev)
        val nameLower = name.trim().lowercase()
        val surnameLower = surname.trim().lowercase()
        return cellMatches && nameLower.contains("thembelani") && surnameLower.contains("sithole")
    }

    fun trialDaysLeft(config: ChairpersonConfig, nowMs: Long = System.currentTimeMillis()): Int {
        if (config.trialStartMs <= 0L) return 0
        val elapsed = nowMs - config.trialStartMs
        if (elapsed >= ScnsConstants.SEVEN_DAYS_MS) return 0
        val remainingMs = ScnsConstants.SEVEN_DAYS_MS - elapsed
        return ((remainingMs + 86_399_999L) / 86_400_000L).toInt().coerceIn(1, 7)
    }

    fun formatZar(amount: Long): String {
        val formatted = "%,d".format(amount)
        return "R $formatted"
    }
}
