package com.charles.scamradar.app.ui.navigation

/**
 * Deep links surfaced by the launching Activity that the NavHost needs to consume
 * once it has constructed its NavController. Currently used to hand off a scan
 * result from [com.charles.scamradar.app.ui.quickverdict.QuickVerdictActivity] so
 * tapping "Full report" lands directly on the Result screen.
 */
sealed interface PendingDeepLink {
    data class OpenResult(val scanResultJson: String) : PendingDeepLink
    data class JoinFamily(val code: String) : PendingDeepLink
}
