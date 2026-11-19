package com.example.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    LIST,
    GET
}

data class FirestoreErrorInfo(
    val error: String,
    val operationType: OperationType,
    val path: String?,
    val authInfo: AuthInfo
) {
    data class AuthInfo(
        val userId: String?,
        val email: String?,
        val emailVerified: Boolean?,
        val isAnonymous: Boolean?,
        val providerIds: List<String>
    )

    fun toJsonString(): String {
        val providersArray = JSONArray().apply {
            authInfo.providerIds.forEach { put(it) }
        }
        val authJson = JSONObject().apply {
            put("userId", authInfo.userId)
            put("email", authInfo.email)
            put("emailVerified", authInfo.emailVerified)
            put("isAnonymous", authInfo.isAnonymous)
            put("providerIds", providersArray)
        }
        return JSONObject().apply {
            put("error", error)
            put("operationType", operationType.name.lowercase())
            put("path", path)
            put("authInfo", authJson)
        }.toString()
    }
}

object FirestoreErrorHandler {
    private const val TAG = "FirestoreError"

    fun handleFirestoreError(
        error: Throwable,
        operationType: OperationType,
        path: String?,
        auth: FirebaseAuth = FirebaseAuth.getInstance()
    ): Nothing {
        val currentUser = auth.currentUser
        val errInfo = FirestoreErrorInfo(
            error = error.message ?: error.toString(),
            operationType = operationType,
            path = path,
            authInfo = FirestoreErrorInfo.AuthInfo(
                userId = currentUser?.uid,
                email = currentUser?.email,
                emailVerified = currentUser?.isEmailVerified,
                isAnonymous = currentUser?.isAnonymous,
                providerIds = currentUser?.providerData?.map { it.providerId } ?: emptyList()
            )
        )
        val json = errInfo.toJsonString()
        Log.e(TAG, "Firestore Operation Failed: $json", error)
        throw IllegalStateException(friendlyMessage(error), error)
    }

    fun friendlyMessage(error: Throwable): String {
        val firestoreEx = error as? FirebaseFirestoreException
        return when (firestoreEx?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Access denied by security rules. Please verify your Google Sign-In session."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Cloud database temporarily unavailable. Changes will sync when reconnected."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "Requested record was not found."
            FirebaseFirestoreException.Code.ALREADY_EXISTS ->
                "This record already exists."
            else -> error.message ?: "Unexpected cloud database error."
        }
    }
}
