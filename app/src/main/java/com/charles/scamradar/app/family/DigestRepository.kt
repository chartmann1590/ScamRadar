package com.charles.scamradar.app.family

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class WeeklyDigest(
    val weekStarting: String,
    val totalShares: Int,
    val likelyScams: Int,
    val suspicious: Int,
    val topScamTypes: List<String>,
    val perMember: List<MemberStat>,
    val generatedAt: Long,
)

data class MemberStat(
    val memberLabel: String,
    val scansThisWeek: Int,
    val scamsCaught: Int,
)

class DigestRepository(context: Context) {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun observeLatest(podCode: String): Flow<WeeklyDigest?> = callbackFlow {
        if (podCode.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val reg = firestore.collection("families").document(podCode)
            .collection("digests")
            .orderBy("weekStarting", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, _ ->
                if (snap == null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val first = snap.documents.firstOrNull()
                if (first == null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(
                    WeeklyDigest(
                        weekStarting = first.getString("weekStarting").orEmpty(),
                        totalShares = (first.getLong("totalShares") ?: 0).toInt(),
                        likelyScams = (first.getLong("likelyScams") ?: 0).toInt(),
                        suspicious = (first.getLong("suspicious") ?: 0).toInt(),
                        topScamTypes = (first.get("topScamTypes") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                        perMember = (first.get("perMember") as? List<*>)?.mapNotNull { raw ->
                            val map = raw as? Map<*, *> ?: return@mapNotNull null
                            MemberStat(
                                memberLabel = map["memberLabel"] as? String ?: "Member",
                                scansThisWeek = (map["scansThisWeek"] as? Number)?.toInt() ?: 0,
                                scamsCaught = (map["scamsCaught"] as? Number)?.toInt() ?: 0,
                            )
                        }.orEmpty(),
                        generatedAt = first.getTimestamp("generatedAt")?.toDate()?.time ?: 0L,
                    )
                )
            }
        awaitClose { reg.remove() }
    }
}
