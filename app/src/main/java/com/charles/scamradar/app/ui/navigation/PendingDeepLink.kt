package com.charles.scamradar.app.ui.navigation

sealed interface PendingDeepLink {
    data class OpenResult(val scanResultJson: String) : PendingDeepLink
    data class JoinFamily(val code: String) : PendingDeepLink
    data class ApplyRemoteSetup(val payload: String) : PendingDeepLink
    data class OpenShieldAlert(val pkg: String) : PendingDeepLink
    data class OpenLibraryPattern(val scamType: String) : PendingDeepLink
    data class OpenVerifyResult(val payload: String) : PendingDeepLink
    data object OpenDigest : PendingDeepLink
}
