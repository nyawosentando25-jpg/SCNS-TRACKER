package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AttendanceEntry
import com.example.data.model.ChairpersonConfig
import com.example.data.model.ExamEntry
import com.example.data.model.ExecutiveMember
import com.example.data.model.LearnerRecord
import com.example.data.model.ManualSaveSnapshot
import com.example.data.model.PaymentEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ScnsRepository(
    context: Context,
    authOverride: FirebaseAuth? = null
) {
    private val appContext = context.applicationContext
    private val auth: FirebaseAuth by lazy { authOverride ?: FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(
            appContext.getString(R.string.firestore_database_id)
        )
    }

    fun ensureInitialized() {
        firestore
    }

    private fun learnersCollection(uid: String) =
        firestore.collection("users").document(uid).collection("learners")

    private fun executivesCollection(uid: String) =
        firestore.collection("users").document(uid).collection("executives")

    private fun chairDoc(uid: String) =
        firestore.collection("users").document(uid).collection("settings").document("chairperson")

    private fun manualSavesCollection(uid: String) =
        firestore.collection("users").document(uid).collection("manualSaves")

    fun observeLearners(uid: String): Flow<List<LearnerRecord>> = callbackFlow {
        val path = "users/$uid/learners"
        val listener = learnersCollection(uid)
            .orderBy("regNo", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(IllegalStateException(FirestoreErrorHandler.friendlyMessage(error), error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toLearnerRecord() } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observeExecutives(uid: String): Flow<List<ExecutiveMember>> = callbackFlow {
        val listener = executivesCollection(uid)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(IllegalStateException(FirestoreErrorHandler.friendlyMessage(error), error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toExecutiveMember() } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observeChairpersonConfig(uid: String): Flow<ChairpersonConfig> = callbackFlow {
        val listener = chairDoc(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(IllegalStateException(FirestoreErrorHandler.friendlyMessage(error), error))
                    return@addSnapshotListener
                }
                val config = snapshot?.toChairpersonConfig() ?: ChairpersonConfig(ownerId = uid)
                trySend(config)
            }
        awaitClose { listener.remove() }
    }

    fun observeManualSaves(uid: String): Flow<List<ManualSaveSnapshot>> = callbackFlow {
        val listener = manualSavesCollection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(25)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(IllegalStateException(FirestoreErrorHandler.friendlyMessage(error), error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toManualSaveSnapshot() } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun upsertLearner(uid: String, learner: LearnerRecord): Result<String> = runCatching {
        val isCreate = learner.id.isBlank()
        val ref = if (isCreate) learnersCollection(uid).document() else learnersCollection(uid).document(learner.id)
        val path = "users/$uid/learners/${ref.id}"
        try {
            if (isCreate) {
                ref.set(learner.toCreateMap(uid)).await()
            } else {
                ref.update(learner.toUpdateMap()).await()
            }
            ref.id
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(
                e,
                if (isCreate) OperationType.CREATE else OperationType.UPDATE,
                path,
                auth
            )
        }
    }

    suspend fun batchUpsertLearners(uid: String, learners: List<LearnerRecord>): Result<Int> = runCatching {
        if (learners.isEmpty()) return@runCatching 0
        var count = 0
        for (chunk in learners.chunked(400)) {
            val batch = firestore.batch()
            for (learner in chunk) {
                val isCreate = learner.id.isBlank()
                val ref = if (isCreate) learnersCollection(uid).document() else learnersCollection(uid).document(learner.id)
                if (isCreate) {
                    batch.set(ref, learner.toCreateMap(uid))
                } else {
                    batch.update(ref, learner.toUpdateMap())
                }
                count++
            }
            try {
                batch.commit().await()
            } catch (e: Exception) {
                FirestoreErrorHandler.handleFirestoreError(
                    e,
                    OperationType.UPDATE,
                    "users/$uid/learners",
                    auth
                )
            }
        }
        count
    }

    suspend fun deleteLearner(uid: String, learnerId: String): Result<Unit> = runCatching {
        val path = "users/$uid/learners/$learnerId"
        try {
            learnersCollection(uid).document(learnerId).delete().await()
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, path, auth)
        }
    }

    suspend fun saveChairpersonConfig(uid: String, config: ChairpersonConfig): Result<Unit> = runCatching {
        val path = "users/$uid/settings/chairperson"
        val docRef = chairDoc(uid)
        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(config.toCreateMap(uid)).await()
            } else {
                docRef.update(config.toUpdateMap()).await()
            }
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, path, auth)
        }
    }

    suspend fun upsertExecutive(uid: String, exec: ExecutiveMember): Result<String> = runCatching {
        val isCreate = exec.id.isBlank()
        val ref = if (isCreate) executivesCollection(uid).document() else executivesCollection(uid).document(exec.id)
        val path = "users/$uid/executives/${ref.id}"
        try {
            if (isCreate) {
                ref.set(exec.toCreateMap(uid)).await()
            } else {
                ref.update(exec.toUpdateMap()).await()
            }
            ref.id
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(
                e,
                if (isCreate) OperationType.CREATE else OperationType.UPDATE,
                path,
                auth
            )
        }
    }

    suspend fun deleteExecutive(uid: String, execId: String): Result<Unit> = runCatching {
        val path = "users/$uid/executives/$execId"
        try {
            executivesCollection(uid).document(execId).delete().await()
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, path, auth)
        }
    }

    suspend fun createManualSave(uid: String, snapshot: ManualSaveSnapshot): Result<String> = runCatching {
        val ref = manualSavesCollection(uid).document()
        val path = "users/$uid/manualSaves/${ref.id}"
        try {
            ref.set(snapshot.toCreateMap(uid)).await()
            ref.id
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, path, auth)
        }
    }

    suspend fun deleteManualSave(uid: String, saveId: String): Result<Unit> = runCatching {
        val path = "users/$uid/manualSaves/$saveId"
        try {
            manualSavesCollection(uid).document(saveId).delete().await()
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, path, auth)
        }
    }

    private fun DocumentSnapshot.toLearnerRecord(): LearnerRecord? {
        if (!exists()) return null
        val sb = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        val rawAtt = get("att") as? List<*> ?: emptyList<Any>()
        val attList = rawAtt.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            AttendanceEntry(
                d = (m["d"] as? String).orEmpty(),
                day = (m["day"] as? String).orEmpty().ifBlank { "Saturday" },
                s = (m["s"] as? String).orEmpty().ifBlank { "Present" },
                subject = (m["subject"] as? String).orEmpty()
            )
        }
        val rawPay = get("pay") as? List<*> ?: emptyList<Any>()
        val payList = rawPay.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            PaymentEntry(
                d = (m["d"] as? String).orEmpty(),
                t = (m["t"] as? String).orEmpty().ifBlank { "Termly" },
                month = (m["month"] as? String).orEmpty(),
                term = (m["term"] as? String).orEmpty(),
                amt = (m["amt"] as? Number)?.toLong() ?: 0L,
                note = (m["note"] as? String).orEmpty()
            )
        }
        val rawExams = get("exams") as? Map<*, *> ?: emptyMap<Any, Any>()
        val examsMap = buildMap {
            for ((k, v) in rawExams) {
                val key = k as? String ?: continue
                val m = v as? Map<*, *> ?: continue
                put(
                    key,
                    ExamEntry(
                        subject = (m["subject"] as? String).orEmpty(),
                        term = (m["term"] as? String).orEmpty().ifBlank { "Term 1" },
                        date = (m["date"] as? String).orEmpty(),
                        t1 = (m["t1"] as? Number)?.toInt() ?: 0,
                        t2 = (m["t2"] as? Number)?.toInt() ?: 0,
                        t3 = (m["t3"] as? Number)?.toInt() ?: 0,
                        t4 = (m["t4"] as? Number)?.toInt() ?: 0
                    )
                )
            }
        }
        return LearnerRecord(
            id = id,
            ownerId = getString("ownerId").orEmpty(),
            regNo = getString("regNo").orEmpty(),
            names = getString("names").orEmpty(),
            surname = getString("surname").orEmpty(),
            grade = getString("grade").orEmpty().ifBlank { "10" },
            school = getString("school").orEmpty(),
            sex = getString("sex").orEmpty(),
            race = getString("race").orEmpty(),
            dob = getString("dob").orEmpty(),
            age = getString("age").orEmpty(),
            admDate = getString("admDate").orEmpty(),
            subjects = getString("subjects").orEmpty(),
            status = getString("status").orEmpty().ifBlank { "Accepted" },
            depPaid = getBoolean("depPaid") ?: false,
            depDate = getString("depDate").orEmpty(),
            parent = getString("parent").orEmpty(),
            docRef = getString("docRef").orEmpty(),
            att = attList,
            pay = payList,
            exams = examsMap,
            createdAt = getTimestamp("createdAt", sb),
            updatedAt = getTimestamp("updatedAt", sb)
        )
    }

    private fun DocumentSnapshot.toExecutiveMember(): ExecutiveMember? {
        if (!exists()) return null
        val sb = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return ExecutiveMember(
            id = id,
            ownerId = getString("ownerId").orEmpty(),
            name = getString("name").orEmpty(),
            surname = getString("surname").orEmpty(),
            idNumber = getString("idNumber").orEmpty(),
            cell = getString("cell").orEmpty(),
            pin = getString("pin").orEmpty(),
            role = getString("role").orEmpty().ifBlank { "Secretary" },
            approved = getBoolean("approved") ?: false,
            otp = getString("otp").orEmpty(),
            otpPending = getBoolean("otpPending") ?: false,
            termLabel = getString("termLabel").orEmpty().ifBlank { "Term 1" },
            createdAt = getTimestamp("createdAt", sb),
            updatedAt = getTimestamp("updatedAt", sb)
        )
    }

    private fun DocumentSnapshot.toChairpersonConfig(): ChairpersonConfig {
        if (!exists()) return ChairpersonConfig()
        val sb = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return ChairpersonConfig(
            id = id,
            ownerId = getString("ownerId").orEmpty(),
            chairName = getString("chairName").orEmpty().ifBlank { ChairpersonConfig().chairName },
            chairCell = getString("chairCell").orEmpty().ifBlank { ChairpersonConfig().chairCell },
            prevChairCell = getString("prevChairCell").orEmpty(),
            prevCellUntilMs = (get("prevCellUntilMs") as? Number)?.toLong() ?: 0L,
            otpSender = getString("otpSender").orEmpty().ifBlank { "Chairperson" },
            trialStartMs = (get("trialStartMs") as? Number)?.toLong() ?: 0L,
            createdAt = getTimestamp("createdAt", sb),
            updatedAt = getTimestamp("updatedAt", sb)
        )
    }

    private fun DocumentSnapshot.toManualSaveSnapshot(): ManualSaveSnapshot? {
        if (!exists()) return null
        val sb = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return ManualSaveSnapshot(
            id = id,
            ownerId = getString("ownerId").orEmpty(),
            label = getString("label").orEmpty(),
            savedAtDisplay = getString("savedAtDisplay").orEmpty(),
            learnerCount = (get("learnerCount") as? Number)?.toLong() ?: 0L,
            totalCollected = (get("totalCollected") as? Number)?.toLong() ?: 0L,
            snapshotJson = getString("snapshotJson").orEmpty().ifBlank { "[]" },
            createdAt = getTimestamp("createdAt", sb)
        )
    }
}
