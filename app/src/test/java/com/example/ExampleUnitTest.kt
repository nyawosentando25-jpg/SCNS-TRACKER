package com.example

import com.example.data.model.AttendanceEntry
import com.example.data.model.ChairpersonConfig
import com.example.data.model.ExamEntry
import com.example.data.model.LearnerRecord
import com.example.data.model.PaymentEntry
import com.example.data.model.ScnsCalculators
import com.example.data.model.ScnsConstants
import com.example.data.model.ScnsSubjectCatalog
import com.example.util.CsvExportScope
import com.example.util.ScnsCsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun totalDue_calculatesSingleMultipleAndBundleDiscount() {
        // 1 subject (or blank defaulting to 1): 1 * 2400 + 100 = 2500
        assertEquals(2500L, ScnsCalculators.totalDue(""))
        assertEquals(2500L, ScnsCalculators.totalDue("Mathematics"))

        // 2 subjects: 2 * 2400 + 100 = 4900
        assertEquals(4900L, ScnsCalculators.totalDue("Mathematics, Physical Sciences"))

        // 3 subjects (R500 bundle discount): 3 * 2400 + 100 - 500 = 6800
        assertEquals(
            6800L,
            ScnsCalculators.totalDue("Mathematics, Physical Sciences, Life Sciences")
        )

        // 4 subjects (R500 bundle discount): 4 * 2400 + 100 - 500 = 9200
        assertEquals(
            9200L,
            ScnsCalculators.totalDue("Mathematics, Physical Sciences, Life Sciences, Accounting")
        )
    }

    @Test
    fun nextRegNo_incrementsHighestSequenceNumber() {
        assertEquals("SCNS-2027-001", ScnsCalculators.nextRegNo(emptyList()))

        val existing = listOf(
            LearnerRecord(regNo = "SCNS-2027-001"),
            LearnerRecord(regNo = "SCNS-2027-014"),
            LearnerRecord(regNo = "SCNS-2027-009")
        )
        assertEquals("SCNS-2027-015", ScnsCalculators.nextRegNo(existing))
    }

    @Test
    fun computeExamStats_evaluatesAllRatingThresholds() {
        val atRisk = ScnsCalculators.computeExamStats(ExamEntry(t1 = 40, t2 = 45, t3 = 48, t4 = 50))
        assertEquals(183, atRisk.total)
        assertEquals(46, atRisk.percentage)
        assertEquals("At Risk - Extra Help", atRisk.ratingLabel)

        val fair = ScnsCalculators.computeExamStats(ExamEntry(t1 = 50, t2 = 55, t3 = 52, t4 = 58))
        assertEquals(54, fair.percentage)
        assertEquals("Fair - Needs Support", fair.ratingLabel)

        val good = ScnsCalculators.computeExamStats(ExamEntry(t1 = 60, t2 = 62, t3 = 65, t4 = 68))
        assertEquals(64, good.percentage)
        assertEquals("Good", good.ratingLabel)

        val veryGood = ScnsCalculators.computeExamStats(ExamEntry(t1 = 70, t2 = 72, t3 = 75, t4 = 78))
        assertEquals(74, veryGood.percentage)
        assertEquals("Very Good", veryGood.ratingLabel)

        val excellent = ScnsCalculators.computeExamStats(ExamEntry(t1 = 80, t2 = 85, t3 = 90, t4 = 95))
        assertEquals(350, excellent.total)
        assertEquals(88, excellent.percentage)
        assertEquals("Excellent", excellent.ratingLabel)
    }

    @Test
    fun parseBatchLines_stripsPrefixesAndHandlesMultiWordNames() {
        val raw = """
            1. Ndlovu Sipho
            2) Khumalo Thabo Junior
            3 - Mkhize
            
            4.   Zungu   Ayanda Naledi  
        """.trimIndent()

        val parsed = ScnsCalculators.parseBatchLines(raw)
        assertEquals(4, parsed.size)
        assertEquals("Ndlovu" to "Sipho", parsed[0])
        assertEquals("Khumalo" to "Thabo Junior", parsed[1])
        assertEquals("Mkhize" to "Mkhize", parsed[2])
        assertEquals("Zungu" to "Ayanda Naledi", parsed[3])
    }

    @Test
    fun deduplicateAttendance_andNormalizeSubject() {
        assertEquals("Mathematics", ScnsSubjectCatalog.normalizeSubject("maths"))
        assertEquals("Mathematical Literacy", ScnsSubjectCatalog.normalizeSubject("Maths Lit"))
        assertEquals("Economic and Management Sciences", ScnsSubjectCatalog.normalizeSubject("EMS"))
        assertEquals("Natural Sciences", ScnsSubjectCatalog.normalizeSubject("NS"))
        assertEquals("Physical Sciences", ScnsSubjectCatalog.normalizeSubject("physics"))

        val entries = listOf(
            AttendanceEntry(d = "2027-02-06", day = "Saturday", s = "Present", subject = "maths"),
            AttendanceEntry(d = "2027-02-06", day = "Saturday", s = "Absent", subject = ""),
            AttendanceEntry(d = "2027-02-07", day = "Sunday", s = "Present", subject = "Physical Sci")
        )

        val (deduped, removed) = ScnsCalculators.deduplicateAttendance(
            entries = entries,
            fallbackSubject = "Mathematics"
        )
        assertEquals(1, removed)
        assertEquals(2, deduped.size)
        assertEquals("Mathematics", deduped[0].subject)
        assertEquals("Physical Sciences", deduped[1].subject)
    }

    @Test
    fun paymentsAndChairpersonHelpers_computeBalancesAndAccess() {
        val learner = LearnerRecord(
            subjects = "Mathematics, Physical Sciences",
            pay = listOf(
                PaymentEntry(t = "Termly", amt = 700L),
                PaymentEntry(t = "Monthly", amt = 500L)
            )
        )
        assertEquals(4900L, ScnsCalculators.totalDue(learner.subjects))
        assertEquals(1200L, ScnsCalculators.paidTotal(learner))
        assertEquals(500L, ScnsCalculators.monthlyPaid(learner))
        assertEquals(700L, ScnsCalculators.termlyPaid(learner))
        assertEquals(3700L, ScnsCalculators.outstanding(learner))
        assertTrue(ScnsCalculators.isOwing(learner))

        val config = ChairpersonConfig()
        assertTrue(
            ScnsCalculators.isChairpersonMatch(
                name = "Mr Thembelani",
                surname = "Sithole",
                cell = "081 360 0636",
                config = config
            )
        )
        assertFalse(
            ScnsCalculators.isChairpersonMatch(
                name = "Other",
                surname = "User",
                cell = "0813600636",
                config = config
            )
        )
        assertEquals(
            3,
            ScnsCalculators.trialDaysLeft(
                config.copy(trialStartMs = 1_000_000L),
                nowMs = 1_000_000L + (4L * 86_400_000L) + 1_000L
            )
        )
        assertEquals(0, ScnsCalculators.trialDaysLeft(config.copy(trialStartMs = 1_000_000L), nowMs = 1_000_000L + ScnsConstants.SEVEN_DAYS_MS))
    }

    @Test
    fun csvExporter_escapesQuotesAndFormatsScopes() {
        val learner = LearnerRecord(
            regNo = "SCNS-2027-001",
            names = "Sipho \"Speedy\"",
            surname = "Ndlovu",
            grade = "12",
            school = "Umlazi High, D-Sec",
            subjects = "Mathematics, Physical Sciences",
            depPaid = true,
            pay = listOf(PaymentEntry(d = "2027-02-01", t = "Termly", term = "Term 1", amt = 700L, note = "Deposit")),
            att = listOf(AttendanceEntry(d = "2027-02-06", day = "Saturday", s = "Present", subject = "maths")),
            exams = mapOf(
                "Mathematics_Term_1" to ExamEntry(
                    subject = "Mathematics",
                    term = "Term 1",
                    date = "2027-03-10",
                    t1 = 80,
                    t2 = 85,
                    t3 = 90,
                    t4 = 85
                )
            )
        )

        val learnersCsv = ScnsCsvExporter.buildCsv(CsvExportScope.LEARNERS, listOf(learner))
        assertTrue(learnersCsv.contains("\"Sipho \"\"Speedy\"\"\""))
        assertTrue(learnersCsv.contains("\"Umlazi High, D-Sec\""))
        assertTrue(learnersCsv.contains("\"4900\",\"700\",\"4200\""))

        val allCsv = ScnsCsvExporter.buildCsv(CsvExportScope.ALL, listOf(learner))
        assertTrue(allCsv.contains("=== LEARNERS SUMMARY ==="))
        assertTrue(allCsv.contains("=== ATTENDANCE REGISTER ==="))
        assertTrue(allCsv.contains("=== PAYMENT TRACKER ==="))
        assertTrue(allCsv.contains("=== EXAMS / TESTS ==="))
        assertTrue(allCsv.contains("\"340\",\"85%\",\"Excellent\""))
    }
}
