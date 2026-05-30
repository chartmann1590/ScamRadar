package com.charles.scamradar.app.family

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object PodKeyVault {

    private const val KEYSTORE = "AndroidKeyStore"

    fun ensureKey(podCode: String): SecretKey {
        val alias = aliasFor(podCode)
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = ks.getKey(alias, null) as? SecretKey
        if (existing != null) return existing

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEYSTORE)
        keyGen.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        return keyGen.generateKey()
    }

    fun delete(podCode: String) {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        runCatching { ks.deleteEntry(aliasFor(podCode)) }
    }

    private fun aliasFor(podCode: String) = "scamradar_pod_${podCode.uppercase()}"
}
