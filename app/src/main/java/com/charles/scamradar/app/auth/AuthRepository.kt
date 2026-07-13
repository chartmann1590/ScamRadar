package com.charles.scamradar.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.charles.scamradar.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class AuthRepository(private val appContext: Context) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val currentUser: FirebaseUser? get() = auth.currentUser

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    sealed interface AuthOutcome {
        data class Success(val user: FirebaseUser) : AuthOutcome
        data class Failed(val message: String) : AuthOutcome
    }

    suspend fun signUpWithEmail(email: String, password: String): AuthOutcome {
        return runCatching {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            AuthOutcome.Success(result.user!!)
        }.getOrElse { AuthOutcome.Failed(friendlyMessage(it)) }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthOutcome {
        return runCatching {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            AuthOutcome.Success(result.user!!)
        }.getOrElse { AuthOutcome.Failed(friendlyMessage(it)) }
    }

    suspend fun sendPasswordReset(email: String): Boolean {
        return runCatching { auth.sendPasswordResetEmail(email.trim()).await(); true }.getOrDefault(false)
    }

    suspend fun signInWithGoogle(activityContext: Context): AuthOutcome {
        return runCatching {
            val credentialManager = CredentialManager.create(activityContext)
            val rawNonce = UUID.randomUUID().toString()
            val digest = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
            val hashedNonce = digest.joinToString("") { "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                error("Unexpected credential type.")
            }
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            AuthOutcome.Success(authResult.user!!)
        }.getOrElse { AuthOutcome.Failed(friendlyMessage(it)) }
    }

    fun signOut() {
        auth.signOut()
    }

    /** Play Store account-deletion policy requires an in-app path to delete
     * the account, not just local data. Firestore family membership docs are
     * left in place (they're keyed by uid and become orphaned but contain no
     * personal data) — deleting the auth account itself is what matters here. */
    suspend fun deleteAccount(): Boolean {
        val user = auth.currentUser ?: return false
        return runCatching { user.delete().await(); true }.getOrDefault(false)
    }

    private fun friendlyMessage(t: Throwable): String {
        return t.message?.substringBefore(":")?.takeIf { it.isNotBlank() } ?: "Something went wrong."
    }
}
