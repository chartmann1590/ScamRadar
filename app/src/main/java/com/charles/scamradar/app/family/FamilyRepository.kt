package com.charles.scamradar.app.family

import android.content.Context
import com.charles.scamradar.app.community.AnonymousAuthBootstrapper
import com.charles.scamradar.app.community.ReportSanitizer
import com.charles.scamradar.app.data.model.ScanResult
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class FamilyShare(
    val verdict: String,
    val scamType: String,
    val confidence: Double,
    val excerpt: String,
    val sharedByLabel: String,
    val sharedAt: Long
)

class FamilyRepository(context: Context) {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val codeGen = FamilyCodeGenerator(context)

    sealed interface CreateOutcome {
        data class Created(val code: String) : CreateOutcome
        data class Failed(val reason: String) : CreateOutcome
    }

    sealed interface JoinOutcome {
        data class Joined(val code: String, val memberLabel: String? = null) : JoinOutcome
        data object PodFull : JoinOutcome
        data object NotFound : JoinOutcome
        data class Failed(val reason: String) : JoinOutcome
    }

    suspend fun createFamily(): CreateOutcome {
        val uid = AnonymousAuthBootstrapper.ensureSignedIn()
            ?: return CreateOutcome.Failed("Couldn't sign in.")

        repeat(MAX_GENERATION_ATTEMPTS) { attempt ->
            val candidate = if (attempt < 4) codeGen.generate()
            else codeGen.generateWithCollisionSuffix(codeGen.generate())
            val familyRef = firestore.collection("families").document(candidate)
            val exists = runCatching { familyRef.get().await().exists() }.getOrDefault(true)
            if (exists) return@repeat
            val createResult = runCatching {
                firestore.runTransaction { tx ->
                    tx.set(
                        familyRef,
                        mapOf(
                            "memberCount" to 1,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        familyRef.collection("members").document(uid),
                        mapOf(
                            "memberLabel" to "Member 1",
                            "joinedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    null
                }.await()
            }
            if (createResult.isSuccess) return CreateOutcome.Created(candidate)
        }
        return CreateOutcome.Failed("All attempted codes collided. Try again.")
    }

    suspend fun joinFamily(rawCode: String): JoinOutcome {
        val code = codeGen.normalize(rawCode)
        if (!codeGen.isValidFormat(code)) return JoinOutcome.NotFound

        val uid = AnonymousAuthBootstrapper.ensureSignedIn()
            ?: return JoinOutcome.Failed("Couldn't sign in.")

        val familyRef = firestore.collection("families").document(code)
        val snap = runCatching { familyRef.get().await() }.getOrNull()
            ?: return JoinOutcome.Failed("Network error.")
        if (!snap.exists()) return JoinOutcome.NotFound
        val memberCount = (snap.getLong("memberCount") ?: 0L).toInt()
        if (memberCount >= 8) return JoinOutcome.PodFull

        val result = runCatching {
            firestore.runTransaction { tx ->
                val latest = tx.get(familyRef)
                if (!latest.exists()) {
                    throw IllegalStateException("not_found")
                }
                val latestCount = (latest.getLong("memberCount") ?: 0L).toInt()
                if (latestCount >= 8) {
                    throw IllegalStateException("pod_full")
                }
                val memberLabel = "Member ${latestCount + 1}"
                tx.set(
                    familyRef.collection("members").document(uid),
                    mapOf(
                        "memberLabel" to memberLabel,
                        "joinedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(familyRef, "memberCount", latestCount + 1)
                memberLabel
            }.await()
        }
        if (result.isFailure) {
            return when (result.exceptionOrNull()?.message) {
                "not_found" -> JoinOutcome.NotFound
                "pod_full" -> JoinOutcome.PodFull
                else -> JoinOutcome.Failed(result.exceptionOrNull()?.message ?: "Failed to join.")
            }
        }

        return JoinOutcome.Joined(code, result.getOrNull())
    }

    suspend fun leaveFamily(code: String) {
        val uid = AnonymousAuthBootstrapper.ensureSignedIn() ?: return
        runCatching {
            val familyRef = firestore.collection("families").document(code)
            val memberRef = familyRef.collection("members").document(uid)
            firestore.runTransaction { tx ->
                val familyDoc = tx.get(familyRef)
                if (!familyDoc.exists()) return@runTransaction null
                val memberDoc = tx.get(memberRef)
                if (!memberDoc.exists()) return@runTransaction null
                val memberCount = (familyDoc.getLong("memberCount") ?: 1L).toInt()
                tx.delete(memberRef)
                tx.update(familyRef, "memberCount", (memberCount - 1).coerceAtLeast(0))
                null
            }.await()
        }
    }

    suspend fun shareWithFamily(code: String, result: ScanResult): Boolean {
        val uid = AnonymousAuthBootstrapper.ensureSignedIn() ?: return false
        val sanitized = ReportSanitizer.sanitize(result.originalMessage)
        if (!sanitized.isUsable) return false
        val memberSnap = runCatching {
            firestore.collection("families").document(code)
                .collection("members").document(uid).get().await()
        }.getOrNull() ?: return false
        val label = memberSnap.getString("memberLabel") ?: "Member"

        return runCatching {
            firestore.collection("families").document(code).collection("shares").add(
                mapOf(
                    "verdict" to result.verdict.name,
                    "scamType" to result.scamType.name,
                    "confidence" to result.confidence.toDouble(),
                    "sanitizedExcerpt" to sanitized.excerpt,
                    "sharedByLabel" to label,
                    "sharedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        }.getOrDefault(false)
    }

    fun observeShares(code: String): Flow<List<FamilyShare>> = callbackFlow {
        val registration = firestore.collection("families").document(code)
            .collection("shares")
            .orderBy("sharedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val items = snap.documents.mapNotNull { doc ->
                    val verdict = doc.getString("verdict") ?: return@mapNotNull null
                    FamilyShare(
                        verdict = verdict,
                        scamType = doc.getString("scamType").orEmpty(),
                        confidence = doc.getDouble("confidence") ?: 0.0,
                        excerpt = doc.getString("sanitizedExcerpt").orEmpty(),
                        sharedByLabel = doc.getString("sharedByLabel").orEmpty(),
                        sharedAt = doc.getTimestamp("sharedAt")?.toDate()?.time ?: 0L
                    )
                }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    companion object {
        private const val MAX_GENERATION_ATTEMPTS = 6
    }
}
