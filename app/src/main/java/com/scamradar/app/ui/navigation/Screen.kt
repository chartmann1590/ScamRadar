package com.scamradar.app.ui.navigation

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
}
