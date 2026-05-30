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

    data object Premium : Screen("premium")

    data object Achievements : Screen("achievements")

    data object ShieldSettings : Screen("shield/settings")
    data object ShieldAlert : Screen("shield/alert/{payload}") {
        fun createRoute(encodedPayload: String): String = "shield/alert/$encodedPayload"
    }

    data object RecoveryWizard : Screen("recovery/wizard/{scanResultJson}") {
        fun createRoute(encodedScanJson: String): String = "recovery/wizard/$encodedScanJson"
    }
    data object RecoveryHub : Screen("recovery/hub")
    data object AuthorityReport : Screen("recovery/authorities/{scamType}/{country}") {
        fun createRoute(scamType: String, country: String): String =
            "recovery/authorities/$scamType/$country"
    }

    data object SeniorHome : Screen("senior/home")
    data object SeniorResult : Screen("senior/result/{scanResultJson}") {
        fun createRoute(encodedJson: String): String = "senior/result/$encodedJson"
    }
    data object SeniorHistory : Screen("senior/history")

    data object VerifyChallenge : Screen("family/verify/challenge")
    data object VerifyResult : Screen("family/verify/result/{payload}") {
        fun createRoute(payload: String): String = "family/verify/result/$payload"
    }

    data object RemoteSetupCreate : Screen("family/remotesetup/create")
    data object RemoteSetupApply : Screen("family/remotesetup/apply/{payload}") {
        fun createRoute(payload: String): String = "family/remotesetup/apply/$payload"
    }

    data object WeeklyDigest : Screen("family/digest")
}
