package com.example.util

import com.example.data.model.LearnerRecord
import com.example.data.model.ScnsCalculators
import com.example.data.model.ScnsSubjectCatalog
import org.json.JSONArray
import org.json.JSONObject

enum class CsvExportScope(val fileBase: String, val label: String) {
    LEARNERS("SCNS_Learners", "Learner List (CSV)"),
    ATTENDANCE("SCNS_Attendance", "Attendance Register (CSV)"),
    PAYMENTS("SCNS_Payments", "Payment Tracker (CSV)"),
    ALL("SCNS_All_Records", "Everything (CSV)")
}

object ScnsCsvExporter {

    private fun csvCell(value: Any?): String {
        val raw = value?.toString().orEmpty()
        val escaped = raw.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun buildCsv(scope: CsvExportScope, learners: List<LearnerRecord>): String {
        val sb = StringBuilder()
        when (scope) {
            CsvExportScope.LEARNERS -> {
                sb.appendLine(
                    "RegNo,Names,Surname,Grade,School,Sex,Race,DOB,Age,AdmissionDate,Subjects,Status,DepositPaid,DepositDate,ParentGuardian,DocRef,TotalDue,TotalPaid,Balance"
                )
                for (l in learners) {
                    val due = ScnsCalculators.totalDue(l.subjects)
                    val paid = ScnsCalculators.paidTotal(l)
                    val bal = (due - paid).coerceAtLeast(0L)
                    sb.appendLine(
                        listOf(
                            l.regNo, l.names, l.surname, l.grade, l.school, l.sex, l.race,
                            l.dob, l.age, l.admDate, l.subjects, l.status,
                            if (l.depPaid) "Yes" else "No", l.depDate, l.parent, l.docRef,
                            due, paid, bal
                        ).joinToString(",") { csvCell(it) }
                    )
                }
            }

            CsvExportScope.ATTENDANCE -> {
                sb.appendLine("RegNo,Names,Surname,Grade,Date,Day,Subject,Status")
                for (l in learners) {
                    val fallback = ScnsCalculators.primarySubjectOf(l)
                    for (a in l.att) {
                        val subj = ScnsSubjectCatalog.normalizeSubject(a.subject.ifBlank { fallback })
                        sb.appendLine(
                            listOf(l.regNo, l.names, l.surname, l.grade, a.d, a.day, subj, a.s)
                                .joinToString(",") { csvCell(it) }
                        )
                    }
                }
            }

            CsvExportScope.PAYMENTS -> {
                sb.appendLine("RegNo,Names,Surname,Grade,Date,Type,Month,Term,AmountZAR,Note,TotalDue,TotalPaid,Balance")
                for (l in learners) {
                    val due = ScnsCalculators.totalDue(l.subjects)
                    val paid = ScnsCalculators.paidTotal(l)
                    val bal = (due - paid).coerceAtLeast(0L)
                    if (l.pay.isEmpty()) {
                        sb.appendLine(
                            listOf(l.regNo, l.names, l.surname, l.grade, "", "", "", "", 0, "", due, paid, bal)
                                .joinToString(",") { csvCell(it) }
                        )
                    } else {
                        for (p in l.pay) {
                            sb.appendLine(
                                listOf(l.regNo, l.names, l.surname, l.grade, p.d, p.t, p.month, p.term, p.amt, p.note, due, paid, bal)
                                    .joinToString(",") { csvCell(it) }
                            )
                        }
                    }
                }
            }

            CsvExportScope.ALL -> {
                sb.appendLine("=== LEARNERS SUMMARY ===")
                sb.append(buildCsv(CsvExportScope.LEARNERS, learners))
                sb.appendLine()
                sb.appendLine("=== ATTENDANCE REGISTER ===")
                sb.append(buildCsv(CsvExportScope.ATTENDANCE, learners))
                sb.appendLine()
                sb.appendLine("=== PAYMENT TRACKER ===")
                sb.append(buildCsv(CsvExportScope.PAYMENTS, learners))
                sb.appendLine()
                sb.appendLine("=== EXAMS / TESTS ===")
                sb.appendLine("RegNo,Names,Surname,Grade,Subject,Term,Date,Test1,Test2,Test3,Test4,Total400,Percentage,Rating")
                for (l in learners) {
                    for (e in l.exams.values) {
                        val stats = ScnsCalculators.computeExamStats(e)
                        sb.appendLine(
                            listOf(
                                l.regNo, l.names, l.surname, l.grade, e.subject, e.term, e.date,
                                e.t1, e.t2, e.t3, e.t4, stats.total, "${stats.percentage}%", stats.ratingLabel
                            ).joinToString(",") { csvCell(it) }
                        )
                    }
                }
            }
        }
        return sb.toString()
    }

    fun serializeLearnersToJson(learners: List<LearnerRecord>): String {
        val arr = JSONArray()
        for (l in learners) {
            val obj = JSONObject().apply {
                put("regNo", l.regNo)
                put("names", l.names)
                put("surname", l.surname)
                put("grade", l.grade)
                put("school", l.school)
                put("sex", l.sex)
                put("race", l.race)
                put("dob", l.dob)
                put("age", l.age)
                put("admDate", l.admDate)
                put("subjects", l.subjects)
                put("status", l.status)
                put("depPaid", l.depPaid)
                put("depDate", l.depDate)
                put("parent", l.parent)
                put("docRef", l.docRef)
                put("att", JSONArray().apply {
                    l.att.forEach { a ->
                        put(JSONObject().apply {
                            put("d", a.d)
                            put("day", a.day)
                            put("s", a.s)
                            put("subject", a.subject)
                        })
                    }
                })
                put("pay", JSONArray().apply {
                    l.pay.forEach { p ->
                        put(JSONObject().apply {
                            put("d", p.d)
                            put("t", p.t)
                            put("month", p.month)
                            put("term", p.term)
                            put("amt", p.amt)
                            put("note", p.note)
                        })
                    }
                })
                put("exams", JSONObject().apply {
                    l.exams.forEach { (k, e) ->
                        put(k, JSONObject().apply {
                            put("subject", e.subject)
                            put("term", e.term)
                            put("date", e.date)
                            put("t1", e.t1)
                            put("t2", e.t2)
                            put("t3", e.t3)
                            put("t4", e.t4)
                        })
                    }
                })
            }
            arr.put(obj)
        }
        return arr.toString()
    }
}
