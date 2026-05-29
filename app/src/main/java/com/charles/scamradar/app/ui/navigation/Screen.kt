package com.charles.scamradar.app.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Scan : Screen("scan")
    data object Scanning : Screen("scanning/{message}/{scanMode}") {
        fun createRoute(message: String, scanMode: String): String {
            return "scanning/$message/$scanMode"
        }
    }
    data object Result : Screen("result/{scanResultJson}")
    data object Library : Screen("library") {
        const val detailRoute = "library/{patternId}"
        fun createDetailRoute(patternId: Int): String = "library/$patternId"
    }
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Help : Screen("help")
    data object AppTutorial : Screen("app_tutorial")
    data object UrlScan : Screen("urlscan")
    data object UrlScanning : Screen("urlscanning/{url}") {
        fun createRoute(encodedUrl: String): String = "urlscanning/$encodedUrl"
    }
    data object Today : Screen("today")
    data object TrustScore : Screen("trustscore")
    data object FamilyOnboarding : Screen("family")
    data object FamilyCreate : Screen("family/create")
    data object FamilyJoin : Screen("family/join?code={code}") {
        fun createRoute(code: String = ""): String = "family/join?code=$code"
    }
    data object FamilyActivity : Screen("family/activity")
}
