package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface GoogleSignInOutcome {
    data class Success(val user: FirebaseUser) : GoogleSignInOutcome
    data object Cancelled : GoogleSignInOutcome
    data class NoAccount(val message: String) : GoogleSignInOutcome
    data class Error(val message: String) : GoogleSignInOutcome
}

class GoogleAuthManager(
    context: Context,
    authOverride: FirebaseAuth? = null
) {
    private val appContext = context.applicationContext
    private val auth: FirebaseAuth by lazy { authOverride ?: FirebaseAuth.getInstance() }
    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(appContext)
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun observeAuthUser(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun attemptSilentSignIn(activityContext: Context = appContext): GoogleSignInOutcome {
        if (auth.currentUser != null) {
            return GoogleSignInOutcome.Success(auth.currentUser!!)
        }
        val webClientId = runCatching {
            appContext.getString(R.string.default_web_client_id)
        }.getOrNull().orEmpty()
        if (webClientId.isBlank()) {
            return GoogleSignInOutcome.NoAccount("Please sign in with Google to continue.")
        }
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(context = activityContext, request = request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCred).await()
                val user = authResult.user
                if (user != null) {
                    GoogleSignInOutcome.Success(user)
                } else {
                    GoogleSignInOutcome.NoAccount("No authorized session found.")
                }
            } else {
                GoogleSignInOutcome.NoAccount("No authorized Google credential found.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Silent Google Sign-In cancelled or dismissed: ${e.message}", e)
            GoogleSignInOutcome.Cancelled
        } catch (_: NoCredentialException) {
            GoogleSignInOutcome.NoAccount("Please tap Sign in with Google to continue.")
        } catch (e: Exception) {
            Log.d(TAG, "Silent sign-in skipped: ${e.message}")
            GoogleSignInOutcome.NoAccount("Please sign in with Google to sync SCNS records.")
        }
    }

    suspend fun signInInteractive(activityContext: Context): GoogleSignInOutcome {
        return try {
            val webClientId = appContext.getString(R.string.default_web_client_id)
            val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCred).await()
                val user = authResult.user
                if (user != null) {
                    GoogleSignInOutcome.Success(user)
                } else {
                    GoogleSignInOutcome.Error("Google sign-in returned no Firebase user.")
                }
            } else {
                GoogleSignInOutcome.Error("Unexpected credential type received.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            GoogleSignInOutcome.Cancelled
        } catch (e: NoCredentialException) {
            GoogleSignInOutcome.NoAccount(
                e.message ?: "No Google account available on this device. Add a Google account in Settings."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Interactive Google sign-in failed", e)
            GoogleSignInOutcome.Error(e.message ?: "Google sign-in failed. Please try again.")
        }
    }

    suspend fun signOut() {
        withContext(NonCancellable) {
            auth.signOut()
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed clearing CredentialManager state: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "GoogleAuthManager"
    }
}
