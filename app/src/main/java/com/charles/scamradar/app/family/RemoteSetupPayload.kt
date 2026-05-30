package com.charles.scamradar.app.family

import com.google.gson.Gson
import android.util.Base64

data class RemoteSetupPayload(
    val podCode: String,
    val memberLabel: String,
    val seniorMode: Boolean = true,
    val careMode: Boolean = true,
    val emergencyContact: String = "",
    val version: Int = 1,
) {
    fun encode(): String {
        val json = Gson().toJson(this)
        return Base64.encodeToString(json.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
    }

    companion object {
        fun decode(encoded: String): RemoteSetupPayload? {
            return runCatching {
                val bytes = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP)
                Gson().fromJson(String(bytes), RemoteSetupPayload::class.java)
            }.getOrNull()
        }

        fun shareUrl(payload: RemoteSetupPayload): String {
            return "scamradar://remotesetup/${payload.encode()}"
        }
    }
}
