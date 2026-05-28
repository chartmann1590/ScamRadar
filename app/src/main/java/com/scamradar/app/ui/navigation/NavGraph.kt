package com.scamradar.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.scamradar.app.classifier.ClassifierRouter
import com.scamradar.app.data.db.AppDatabase
import com.scamradar.app.data.db.ScanHistoryEntity
import com.scamradar.app.data.datastore.UserPrefs
import com.scamradar.app.data.model.ClassifierTier
import com.scamradar.app.data.model.RedFlag
import com.scamradar.app.data.model.ScamType
import com.scamradar.app.data.model.ScanMode
import com.scamradar.app.data.model.ScanResult
import com.scamradar.app.data.model.Verdict
import com.scamradar.app.download.ModelDownloadService
import com.scamradar.app.download.ModelManager
import com.scamradar.app.ui.components.BottomNavBar
import com.scamradar.app.ui.screens.history.HistoryScreen
import com.scamradar.app.ui.screens.home.HomeScreen
import com.scamradar.app.ui.screens.library.LibraryScreen
import com.scamradar.app.ui.screens.onboarding.OnboardingScreen
import com.scamradar.app.ui.screens.result.ResultScreen
import com.scamradar.app.ui.screens.scanning.ScanningScreen
import com.scamradar.app.ui.screens.settings.SettingsScreen
import java.io.File
import kotlinx.coroutines.launch

private val bottomNavRoutes = setOf("scan", "library", "library/{patternId}", "history", "settings")

@Composable
fun ScamRadarNavHost(
    userPrefs: UserPrefs,
    filesDir: File,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val onboardingComplete by userPrefs.onboardingComplete.collectAsState(initial = false)
    val scanCountToday by userPrefs.scanCountToday.collectAsState(initial = 0)
    val downloadProgress by ModelDownloadService.downloadProgress.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val showBottomBar = currentRoute in bottomNavRoutes

    val classifierRouter = remember { ClassifierRouter(context) }
    val database = remember { AppDatabase.getInstance(context) }
    val gson = remember { Gson() }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("scan") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingComplete) Screen.Scan.route else Screen.Onboarding.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                val modelDeviceClass = remember {
                    when (ModelManager.getDeviceClass(context)) {
                        com.scamradar.app.download.DeviceClass.FULL ->
                            com.scamradar.app.ui.screens.onboarding.DeviceClass.FULL
                        com.scamradar.app.download.DeviceClass.LITE_RECOMMENDED ->
                            com.scamradar.app.ui.screens.onboarding.DeviceClass.LITE_RECOMMENDED
                        com.scamradar.app.download.DeviceClass.LITE_ONLY ->
                            com.scamradar.app.ui.screens.onboarding.DeviceClass.LITE_ONLY
                    }
                }
                val onboardingDownloadState = when (downloadProgress.state) {
                    com.scamradar.app.download.DownloadState.IDLE ->
                        com.scamradar.app.ui.screens.onboarding.DownloadState.IDLE
                    com.scamradar.app.download.DownloadState.DOWNLOADING ->
                        com.scamradar.app.ui.screens.onboarding.DownloadState.DOWNLOADING
                    com.scamradar.app.download.DownloadState.PAUSED ->
                        com.scamradar.app.ui.screens.onboarding.DownloadState.PAUSED
                    com.scamradar.app.download.DownloadState.COMPLETED ->
                        com.scamradar.app.ui.screens.onboarding.DownloadState.COMPLETED
                    com.scamradar.app.download.DownloadState.FAILED ->
                        com.scamradar.app.ui.screens.onboarding.DownloadState.FAILED
                }
                val progressFraction = if (downloadProgress.totalBytes > 0L) {
                    (downloadProgress.bytesDownloaded.toFloat() / downloadProgress.totalBytes.toFloat())
                        .coerceIn(0f, 1f)
                } else {
                    null
                }

                LaunchedEffect(downloadProgress.state) {
                    if (downloadProgress.state == com.scamradar.app.download.DownloadState.COMPLETED) {
                        userPrefs.setModelDownloaded(true)
                    }
                }

                OnboardingScreen(
                    onComplete = {
                        coroutineScope.launch {
                            userPrefs.setOnboardingComplete(true)
                        }
                        navController.navigate(Screen.Scan.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onStartDownload = {
                        ModelDownloadService.startDownload(context)
                    },
                    onPauseDownload = {
                        if (downloadProgress.state == com.scamradar.app.download.DownloadState.PAUSED) {
                            ModelDownloadService.resumeDownload(context)
                        } else {
                            ModelDownloadService.pauseDownload(context)
                        }
                    },
                    deviceClass = modelDeviceClass,
                    downloadProgress = progressFraction,
                    downloadState = onboardingDownloadState,
                    bytesDownloaded = downloadProgress.bytesDownloaded,
                    totalBytes = downloadProgress.totalBytes
                )
            }

            composable(Screen.Scan.route) {
                HomeScreen(
                    navController = navController,
                    scanCountToday = scanCountToday
                )
            }

            composable(
                route = Screen.Scanning.route,
                arguments = listOf(
                    navArgument("message") { type = NavType.StringType },
                    navArgument("scanMode") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val message = NavArgCodec.decode(
                    backStackEntry.arguments?.getString("message") ?: ""
                )
                val scanModeStr = backStackEntry.arguments?.getString("scanMode") ?: "TEXT"
                val scanMode = try { ScanMode.valueOf(scanModeStr) } catch (_: Exception) { ScanMode.TEXT }

                ScanningScreen(
                    message = message,
                    scanMode = scanMode,
                    classifierRouter = classifierRouter,
                    onResult = { scanResult ->
                        coroutineScope.launch {
                            database.scanHistoryDao().insert(ScanHistoryEntity(scanResult, scanMode))
                        }
                        val json = NavArgCodec.encode(gson.toJson(scanResult))
                        navController.navigate("result/$json") {
                            popUpTo(Screen.Scan.route)
                        }
                    },
                    onError = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Result.route,
                arguments = listOf(
                    navArgument("scanResultJson") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val json = NavArgCodec.decode(
                    backStackEntry.arguments?.getString("scanResultJson") ?: ""
                )
                val scanResult = gson.fromJson(json, ScanResult::class.java)

                ResultScreen(
                    scanResult = scanResult,
                    onScanAgain = {
                        navController.navigate(Screen.Scan.route) {
                            popUpTo(Screen.Scan.route) { inclusive = true }
                        }
                    },
                    onBack = {
                        navController.navigate(Screen.Scan.route) {
                            popUpTo(Screen.Scan.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen()
            }

            composable(
                route = Screen.Library.detailRoute,
                arguments = listOf(
                    navArgument("patternId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                LibraryScreen(
                    initialPatternId = backStackEntry.arguments?.getInt("patternId")
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onOpenResult = { entity ->
                        val scanResult = entity.toScanResult(gson)
                        val json = NavArgCodec.encode(gson.toJson(scanResult))
                        navController.navigate("result/$json")
                    },
                    onRescan = { entity ->
                        val encoded = NavArgCodec.encode(entity.originalMessage)
                        navController.navigate("scanning/$encoded/${entity.scanMode}")
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

private fun ScanHistoryEntity.toScanResult(gson: Gson): ScanResult {
    val redFlags = runCatching {
        val type = object : com.google.gson.reflect.TypeToken<List<RedFlag>>() {}.type
        gson.fromJson<List<RedFlag>>(redFlagsJson, type)
    }.getOrDefault(emptyList())

    return ScanResult(
        verdict = runCatching { Verdict.valueOf(verdict) }.getOrDefault(Verdict.SAFE),
        confidence = confidence,
        scamType = runCatching { ScamType.valueOf(scamType) }.getOrDefault(ScamType.NONE),
        redFlags = redFlags,
        aiGeneratedIndicators = emptyList(),
        recommendedAction = recommendedAction,
        originalMessage = originalMessage,
        timestamp = timestamp,
        classifierTier = runCatching { ClassifierTier.valueOf(classifierTier) }.getOrDefault(ClassifierTier.LITE)
    )
}
