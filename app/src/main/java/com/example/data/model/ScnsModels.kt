package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class AttendanceEntry(
    val d: String = "",
    val day: String = "Saturday",
    val s: String = "Present",
    val subject: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "d" to d,
        "day" to day,
        "s" to s,
        "subject" to subject
    )
}

@Keep
@IgnoreExtraProperties
data class PaymentEntry(
    val d: String = "",
    val t: String = "Termly",
    val month: String = "",
    val term: String = "Term 1",
    val amt: Long = 0L,
    val note: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "d" to d,
        "t" to t,
        "month" to month,
        "term" to term,
        "amt" to amt,
        "note" to note
    )
}

@Keep
@IgnoreExtraProperties
data class ExamEntry(
    val subject: String = "",
    val term: String = "Term 1",
    val date: String = "",
    val t1: Int = 0,
    val t2: Int = 0,
    val t3: Int = 0,
    val t4: Int = 0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "subject" to subject,
        "term" to term,
        "date" to date,
        "t1" to t1.coerceIn(0, 100),
        "t2" to t2.coerceIn(0, 100),
        "t3" to t3.coerceIn(0, 100),
        "t4" to t4.coerceIn(0, 100)
    )
}

@Keep
@IgnoreExtraProperties
data class LearnerRecord(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val regNo: String = "",
    val names: String = "",
    val surname: String = "",
    val grade: String = "10",
    val school: String = "",
    val sex: String = "",
    val race: String = "",
    val dob: String = "",
    val age: String = "",
    val admDate: String = "",
    val subjects: String = "",
    val status: String = "Accepted",
    val depPaid: Boolean = false,
    val depDate: String = "",
    val parent: String = "",
    val docRef: String = "",
    val att: List<AttendanceEntry> = emptyList(),
    val pay: List<PaymentEntry> = emptyList(),
    val exams: Map<String, ExamEntry> = emptyMap(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "ownerId" to ownerUid,
        "regNo" to regNo.trim(),
        "names" to names.trim(),
        "surname" to surname.trim(),
        "grade" to grade.ifBlank { "10" },
        "school" to school.trim(),
        "sex" to sex.trim(),
        "race" to race.trim(),
        "dob" to dob.trim(),
        "age" to age.trim(),
        "admDate" to admDate.trim(),
        "subjects" to subjects.trim(),
        "status" to status.ifBlank { "Accepted" },
        "depPaid" to depPaid,
        "depDate" to depDate.trim(),
        "parent" to parent.trim(),
        "docRef" to docRef.trim(),
        "att" to att.take(400).map { it.toMap() },
        "pay" to pay.take(200).map { it.toMap() },
        "exams" to exams.entries.take(60).associate { it.key to it.value.toMap() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "regNo" to regNo.trim(),
        "names" to names.trim(),
        "surname" to surname.trim(),
        "grade" to grade.ifBlank { "10" },
        "school" to school.trim(),
        "sex" to sex.trim(),
        "race" to race.trim(),
        "dob" to dob.trim(),
        "age" to age.trim(),
        "admDate" to admDate.trim(),
        "subjects" to subjects.trim(),
        "status" to status.ifBlank { "Accepted" },
        "depPaid" to depPaid,
        "depDate" to depDate.trim(),
        "parent" to parent.trim(),
        "docRef" to docRef.trim(),
        "att" to att.take(400).map { it.toMap() },
        "pay" to pay.take(200).map { it.toMap() },
        "exams" to exams.entries.take(60).associate { it.key to it.value.toMap() },
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ExecutiveMember(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val surname: String = "",
    val idNumber: String = "",
    val cell: String = "",
    val pin: String = "2027",
    val role: String = "Secretary",
    val approved: Boolean = false,
    val otp: String = "",
    val otpPending: Boolean = false,
    val termLabel: String = "Term 1",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "ownerId" to ownerUid,
        "name" to name.trim(),
        "surname" to surname.trim(),
        "idNumber" to idNumber.trim(),
        "cell" to cell.trim(),
        "pin" to pin.trim().take(4).padStart(4, '0'),
        "role" to role,
        "approved" to approved,
        "otp" to otp.trim(),
        "otpPending" to otpPending,
        "termLabel" to termLabel.trim(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.trim(),
        "surname" to surname.trim(),
        "idNumber" to idNumber.trim(),
        "cell" to cell.trim(),
        "pin" to pin.trim().take(4).padStart(4, '0'),
        "role" to role,
        "approved" to approved,
        "otp" to otp.trim(),
        "otpPending" to otpPending,
        "termLabel" to termLabel.trim(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ChairpersonConfig(
    @DocumentId val id: String = "chairperson",
    val ownerId: String = "",
    val chairName: String = ScnsConstants.DEFAULT_CHAIR_NAME,
    val chairCell: String = ScnsConstants.DEFAULT_CHAIR_CELL,
    val prevChairCell: String = "",
    val prevCellUntilMs: Long = 0L,
    val otpSender: String = "Chairperson",
    val trialStartMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "ownerId" to ownerUid,
        "chairName" to chairName.ifBlank { ScnsConstants.DEFAULT_CHAIR_NAME }.trim(),
        "chairCell" to chairCell.ifBlank { ScnsConstants.DEFAULT_CHAIR_CELL }.trim(),
        "prevChairCell" to prevChairCell.trim(),
        "prevCellUntilMs" to prevCellUntilMs.coerceAtLeast(0L),
        "otpSender" to otpSender.ifBlank { "Chairperson" },
        "trialStartMs" to trialStartMs.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "chairName" to chairName.ifBlank { ScnsConstants.DEFAULT_CHAIR_NAME }.trim(),
        "chairCell" to chairCell.ifBlank { ScnsConstants.DEFAULT_CHAIR_CELL }.trim(),
        "prevChairCell" to prevChairCell.trim(),
        "prevCellUntilMs" to prevCellUntilMs.coerceAtLeast(0L),
        "otpSender" to otpSender.ifBlank { "Chairperson" },
        "trialStartMs" to trialStartMs.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ManualSaveSnapshot(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val label: String = "",
    val savedAtDisplay: String = "",
    val learnerCount: Long = 0L,
    val totalCollected: Long = 0L,
    val snapshotJson: String = "[]",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "ownerId" to ownerUid,
        "label" to label.trim(),
        "savedAtDisplay" to savedAtDisplay.trim(),
        "learnerCount" to learnerCount.coerceAtLeast(0L),
        "totalCollected" to totalCollected.coerceAtLeast(0L),
        "snapshotJson" to snapshotJson,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

data class LocalPhotoImportRecord(
    val id: String,
    val uriString: String,
    val label: String,
    val createdAtDisplay: String
)
