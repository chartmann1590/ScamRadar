package com.charles.scamradar.app.community

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object AnonymousAuthBootstrapper {

    @Volatile private var initialized = false

    suspend fun ensureSignedIn(): String? {
        if (!initialized) {
            initAppCheck()
            initialized = true
        }
        val auth = FirebaseAuth.getInstance()
        val current = auth.currentUser
        return if (current != null) {
            current.uid
        } else {
            runCatching {
                auth.signInAnonymously().await().user?.uid
            }.getOrNull()
        }
    }

    private fun initAppCheck() {
        runCatching {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
