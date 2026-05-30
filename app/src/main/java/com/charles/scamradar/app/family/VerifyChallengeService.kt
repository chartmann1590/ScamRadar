package com.charles.scamradar.app.family

import android.util.Base64
import com.charles.scamradar.app.community.AnonymousAuthBootstrapper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import javax.crypto.Mac

data class VerifyChallenge(
    val podCode: String,
    val memberLabel: String,
    val uid: String,
    val timestamp: Long,
    val nonce: String,
)

data class SignedChallenge(
    val payload: VerifyChallenge,
    val signature: String,
)

object VerifyChallengeService {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun ensureRegistered(podCode: String, memberLabel: String) {
        val uid = AnonymousAuthBootstrapper.ensureSignedIn() ?: return
        PodKeyVault.ensureKey(podCode)
        firestore.collection("families").document(podCode)
            .collection("verifiers").document(uid)
            .set(
                mapOf(
                    "uid" to uid,
                    "memberLabel" to memberLabel,
                    "registeredAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            .await()
    }

    suspend fun sign(podCode: String, memberLabel: String): SignedChallenge? {
        val uid = AnonymousAuthBootstrapper.ensureSignedIn() ?: return null
        val key = runCatching { PodKeyVault.ensureKey(podCode) }.getOrNull() ?: return null
        val payload = VerifyChallenge(
            podCode = podCode,
            memberLabel = memberLabel.ifBlank { "Member" },
            uid = uid,
            timestamp = System.currentTimeMillis(),
            nonce = java.util.UUID.randomUUID().toString(),
        )
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        val payloadJson = Gson().toJson(payload)
        val sig = mac.doFinal(payloadJson.toByteArray())
        return SignedChallenge(
            payload = payload,
            signature = Base64.encodeToString(sig, Base64.URL_SAFE or Base64.NO_WRAP),
        )
    }

    suspend fun verify(encoded: String): Result {
        val parsed = decode(encoded) ?: return Result.MalformedLink
        val (challenge, sig) = parsed
        val key = runCatching { PodKeyVault.ensureKey(challenge.podCode) }.getOrNull()
            ?: return Result.UnknownPod

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        val expected = mac.doFinal(Gson().toJson(challenge).toByteArray())
        val actual = Base64.decode(sig, Base64.URL_SAFE or Base64.NO_WRAP)
        if (!constantTimeEquals(expected, actual)) return Result.SignatureMismatch

        val ageHours = (System.currentTimeMillis() - challenge.timestamp) / 3_600_000L
        if (ageHours > MAX_AGE_HOURS) return Result.Expired

        return Result.Verified(challenge.memberLabel, challenge.podCode)
    }

    fun encode(signed: SignedChallenge): String {
        val packed = mapOf("p" to signed.payload, "s" to signed.signature)
        val json = Gson().toJson(packed)
        return Base64.encodeToString(json.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
    }

    fun shareUrl(signed: SignedChallenge): String {
        return "https://verify.scamradar.app/v/${encode(signed)}"
    }

    private fun decode(encoded: String): Pair<VerifyChallenge, String>? {
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP)
            val raw = Gson().fromJson(String(bytes), com.google.gson.JsonObject::class.java)
            val payload = Gson().fromJson(raw["p"], VerifyChallenge::class.java)
            val sig = raw["s"].asString
            payload to sig
        }.getOrNull()
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    sealed class Result {
        data class Verified(val memberLabel: String, val podCode: String) : Result()
        data object UnknownPod : Result()
        data object SignatureMismatch : Result()
        data object Expired : Result()
        data object MalformedLink : Result()
    }

    private const val MAX_AGE_HOURS = 72L
}
