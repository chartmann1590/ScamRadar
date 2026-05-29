package com.charles.scamradar.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.charles.scamradar.app.ads.InterstitialController
import com.charles.scamradar.app.classifier.ClassifierRouter
import com.charles.scamradar.app.data.db.AppDatabase
import com.charles.scamradar.app.data.db.ScanHistoryEntity
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.data.datastore.UserPrefs.Companion.AUTO_SHARE_LIKELY_AND_SUSPICIOUS
import com.charles.scamradar.app.data.model.ClassifierTier
import com.charles.scamradar.app.data.model.RedFlag
import com.charles.scamradar.app.data.model.ScamType
import com.charles.scamradar.app.data.model.ScanMode
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.Verdict
import com.charles.scamradar.app.download.ModelDownloadService
import com.charles.scamradar.app.download.ModelManager
import com.charles.scamradar.app.ui.components.AdBanner
import com.charles.scamradar.app.ui.components.BottomNavBar
import com.charles.scamradar.app.ui.screens.help.HelpScreen
import com.charles.scamradar.app.ui.screens.history.HistoryScreen
import com.charles.scamradar.app.ui.screens.home.HomeScreen
import com.charles.scamradar.app.ui.screens.library.LibraryScreen
import com.charles.scamradar.app.ui.screens.onboarding.OnboardingScreen
import com.charles.scamradar.app.ui.screens.result.ResultScreen
import com.charles.scamradar.app.ui.screens.scanning.ScanningScreen
import com.charles.scamradar.app.ui.screens.settings.SettingsScreen
import com.charles.scamradar.app.ui.screens.family.FamilyActivityScreen
import com.charles.scamradar.app.ui.screens.family.FamilyCreateScreen
import com.charles.scamradar.app.ui.screens.family.FamilyJoinScreen
import com.charles.scamradar.app.ui.screens.family.FamilyOnboardingScreen
import com.charles.scamradar.app.ui.screens.stats.TrustScoreScreen
import com.charles.scamradar.app.ui.screens.today.TodayScreen
import com.charles.scamradar.app.ui.screens.urlscan.UrlScanScreen
import com.charles.scamradar.app.ui.screens.urlscan.UrlScanningScreen
import com.charles.scamradar.app.data.model.UrlScanMetadata
import com.charles.scamradar.app.family.FamilyRepository
import com.charles.scamradar.app.widget.ScamRadarWidget
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val bottomNavRoutes = setOf("scan", "today", "library", "library/{patternId}", "history", "settings")

@Composable
fun ScamRadarNavHost(
    userPrefs: UserPrefs,
    filesDir: File,
    modifier: Modifier = Modifier,
    pendingDeepLink: PendingDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val onboardingComplete by userPrefs.onboardingComplete.collectAsState(initial = false)
    val careMode by userPrefs.careMode.collectAsState(initial = false)
    val scanCountToday by userPrefs.scanCountToday.collectAsState(initial = 0)
    val downloadProgress by ModelDownloadService.downloadProgress.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val showBottomBar = currentRoute in bottomNavRoutes
    val showBanner = currentRoute.isNotEmpty() && currentRoute != Screen.Onboarding.route && !careMode

    val classifierRouter = remember { ClassifierRouter(context) }
    val database = remember { AppDatabase.getInstance(context) }
    val gson = remember { Gson() }

    LaunchedEffect(pendingDeepLink, onboardingComplete) {
        val link = pendingDeepLink ?: return@LaunchedEffect
        if (!onboardingComplete) return@LaunchedEffect
        when (link) {
            is PendingDeepLink.OpenResult -> {
                val encoded = NavArgCodec.encode(link.scanResultJson)
                navController.navigate("result/$encoded") {
                    popUpTo(Screen.Scan.route)
                }
            }
            is PendingDeepLink.JoinFamily -> {
                navController.navigate(Screen.FamilyJoin.createRoute(link.code))
            }
        }
        onDeepLinkConsumed()
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val bottomBarModifier = if (showBottomBar) {
                Modifier
            } else {
                Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            }
            Column(modifier = bottomBarModifier) {
                if (showBanner) {
                    AdBanner()
                }
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
                        com.charles.scamradar.app.download.DeviceClass.FULL ->
                            com.charles.scamradar.app.ui.screens.onboarding.DeviceClass.FULL
                        com.charles.scamradar.app.download.DeviceClass.LITE_RECOMMENDED ->
                            com.charles.scamradar.app.ui.screens.onboarding.DeviceClass.LITE_RECOMMENDED
                        com.charles.scamradar.app.download.DeviceClass.LITE_ONLY ->
                            com.charles.scamradar.app.ui.screens.onboarding.DeviceClass.LITE_ONLY
                    }
                }
                val onboardingDownloadState = when (downloadProgress.state) {
                    com.charles.scamradar.app.download.DownloadState.IDLE ->
                        com.charles.scamradar.app.ui.screens.onboarding.DownloadState.IDLE
                    com.charles.scamradar.app.download.DownloadState.DOWNLOADING ->
                        com.charles.scamradar.app.ui.screens.onboarding.DownloadState.DOWNLOADING
                    com.charles.scamradar.app.download.DownloadState.PAUSED ->
                        com.charles.scamradar.app.ui.screens.onboarding.DownloadState.PAUSED
                    com.charles.scamradar.app.download.DownloadState.COMPLETED ->
                        com.charles.scamradar.app.ui.screens.onboarding.DownloadState.COMPLETED
                    com.charles.scamradar.app.download.DownloadState.FAILED ->
                        com.charles.scamradar.app.ui.screens.onboarding.DownloadState.FAILED
                }
                val progressFraction = if (downloadProgress.totalBytes > 0L) {
                    (downloadProgress.bytesDownloaded.toFloat() / downloadProgress.totalBytes.toFloat())
                        .coerceIn(0f, 1f)
                } else {
                    null
                }

                LaunchedEffect(downloadProgress.state) {
                    if (downloadProgress.state == com.charles.scamradar.app.download.DownloadState.COMPLETED) {
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
                        if (downloadProgress.state == com.charles.scamradar.app.download.DownloadState.PAUSED) {
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
                            ScamRadarWidget.refreshAll(context)

                            val careModeEnabled = userPrefs.careMode.first()
                            val careModeAutoShare = userPrefs.careModeAutoShare.first()
                            val autoShareThreshold = userPrefs.careModeAutoShareThreshold.first()
                            val familyCode = userPrefs.familyCode.first()
                            val shouldAutoShare = scanResult.verdict == Verdict.LIKELY_SCAM ||
                                (autoShareThreshold == AUTO_SHARE_LIKELY_AND_SUSPICIOUS && scanResult.verdict == Verdict.SUSPICIOUS)
                            if (careModeEnabled && careModeAutoShare && familyCode.isNotBlank() && shouldAutoShare) {
                                runCatching {
                                    FamilyRepository(context).shareWithFamily(familyCode, scanResult)
                                }
                            }
                        }
                        val json = NavArgCodec.encode(gson.toJson(scanResult))
                        val navigateToResult = {
                            navController.navigate("result/$json") {
                                popUpTo(Screen.Scan.route)
                            }
                        }
                        val activity = context as? android.app.Activity
                        if (activity != null && !careMode) {
                            InterstitialController.maybeShow(activity, navigateToResult)
                        } else {
                            navigateToResult()
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
                    },
                    userPrefs = userPrefs,
                    careMode = careMode
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
                SettingsScreen(
                    onOpenFamily = {
                        navController.navigate(Screen.FamilyOnboarding.route)
                    },
                    onOpenTrustScore = {
                        navController.navigate(Screen.TrustScore.route)
                    },
                    onOpenHelp = {
                        navController.navigate(Screen.Help.route)
                    },
                    onReplayTutorial = {
                        navController.navigate(Screen.AppTutorial.route)
                    }
                )
            }

            composable(Screen.Help.route) {
                HelpScreen(
                    onBack = { navController.popBackStack() },
                    onReplayTutorial = { navController.navigate(Screen.AppTutorial.route) },
                    onOpenFamily = { navController.navigate(Screen.FamilyOnboarding.route) }
                )
            }

            composable(Screen.AppTutorial.route) {
                val modelDeviceClass = remember {
                    when (ModelManager.getDeviceClass(context)) {
                        com.charles.scamradar.app.download.DeviceClass.FULL ->
                            com.charles.scamradar.app.ui.screens.onboarding.DeviceClass.FULL
                        com.charles.scamradar.app.download.DeviceClass.LITE_RECOMMENDED ->
                            com.charles.scamradar.app.ui.screens.onboarding.DeviceClass.LITE_RECOMMENDED
                        com.charles.scamradar.app.download.DeviceClass.LITE_ONLY ->
                            com.charles.scamradar.app.ui.screens.onboarding.DeviceClass.LITE_ONLY
                    }
                }
                val onboardingDownloadState = when (downloadProgress.state) {
                    com.charles.scamradar.app.download.DownloadState.IDLE ->
                        com.charles.scamradar.app.ui.screens.onboarding.DownloadState.IDLE
                    com.charles.scamradar.app.download.DownloadState.DOWNLOADING ->
                        com.charles.scamradar.app.ui.screens.onboarding.DownloadState.DOWNLOADING
                    com.charles.scamradar.app.download.DownloadState.PAUSED ->
                        com.charles.scamradar.app.ui.screens.onboarding.DownloadState.PAUSED
                    com.charles.scamradar.app.download.DownloadState.COMPLETED ->
                        com.charles.scamradar.app.ui.screens.onboarding.DownloadState.COMPLETED
                    com.charles.scamradar.app.download.DownloadState.FAILED ->
                        com.charles.scamradar.app.ui.screens.onboarding.DownloadState.FAILED
                }
                val progressFraction = if (downloadProgress.totalBytes > 0L) {
                    (downloadProgress.bytesDownloaded.toFloat() / downloadProgress.totalBytes.toFloat())
                        .coerceIn(0f, 1f)
                } else {
                    null
                }

                OnboardingScreen(
                    onComplete = { navController.popBackStack() },
                    onStartDownload = {
                        ModelDownloadService.startDownload(context)
                    },
                    onPauseDownload = {
                        if (downloadProgress.state == com.charles.scamradar.app.download.DownloadState.PAUSED) {
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

            composable(Screen.FamilyOnboarding.route) {
                val familyCodeState by userPrefs.familyCode.collectAsState(initial = "")
                LaunchedEffect(familyCodeState) {
                    if (familyCodeState.isNotBlank()) {
                        navController.navigate(Screen.FamilyActivity.route) {
                            popUpTo(Screen.FamilyOnboarding.route) { inclusive = true }
                        }
                    }
                }
                FamilyOnboardingScreen(
                    onCreate = { navController.navigate(Screen.FamilyCreate.route) },
                    onJoin = { navController.navigate(Screen.FamilyJoin.createRoute("")) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.FamilyCreate.route) {
                FamilyCreateScreen(
                    userPrefs = userPrefs,
                    onBack = { navController.popBackStack() },
                    onJoined = {
                        navController.navigate(Screen.FamilyActivity.route) {
                            popUpTo(Screen.FamilyOnboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.FamilyJoin.route,
                arguments = listOf(
                    navArgument("code") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val initial = backStackEntry.arguments?.getString("code").orEmpty()
                FamilyJoinScreen(
                    userPrefs = userPrefs,
                    initialCode = initial,
                    onBack = { navController.popBackStack() },
                    onJoined = {
                        navController.navigate(Screen.FamilyActivity.route) {
                            popUpTo(Screen.FamilyOnboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.FamilyActivity.route) {
                FamilyActivityScreen(
                    userPrefs = userPrefs,
                    onBack = { navController.popBackStack() },
                    onLeave = {
                        navController.navigate(Screen.Scan.route) {
                            popUpTo(Screen.Scan.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Today.route) {
                TodayScreen(
                    userPrefs = userPrefs,
                    onOpenTrustScore = {
                        navController.navigate(Screen.TrustScore.route)
                    }
                )
            }

            composable(Screen.TrustScore.route) {
                TrustScoreScreen(
                    userPrefs = userPrefs,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.UrlScan.route) {
                UrlScanScreen(
                    onBack = { navController.popBackStack() },
                    onScan = { url ->
                        val encoded = NavArgCodec.encode(url)
                        navController.navigate(Screen.UrlScanning.createRoute(encoded))
                    }
                )
            }

            composable(
                route = Screen.UrlScanning.route,
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { backStackEntry ->
                val url = NavArgCodec.decode(backStackEntry.arguments?.getString("url") ?: "")
                UrlScanningScreen(
                    url = url,
                    classifierRouter = classifierRouter,
                    onResult = { scanResult, capture ->
                        val features = com.charles.scamradar.app.webcapture.UrlFeatureExtractor.extract(
                            originalUrl = capture.originalUrl,
                            finalUrl = capture.finalUrl,
                            redirectCount = capture.redirectCount
                        )
                        val enriched = scanResult.copy(
                            urlMetadata = UrlScanMetadata(
                                originalUrl = capture.originalUrl,
                                finalUrl = capture.finalUrl,
                                screenshotPath = capture.screenshotPath,
                                redirectCount = capture.redirectCount,
                                findings = features.findings
                            ),
                            originalMessage = capture.finalUrl
                        )
                        coroutineScope.launch {
                            database.scanHistoryDao().insert(ScanHistoryEntity(enriched, ScanMode.URL))
                            ScamRadarWidget.refreshAll(context)
                        }
                        val json = NavArgCodec.encode(gson.toJson(enriched))
                        navController.navigate("result/$json") {
                            popUpTo(Screen.Scan.route)
                        }
                    },
                    onError = {
                        navController.popBackStack()
                    }
                )
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
        classifierTier = runCatching { ClassifierTier.valueOf(classifierTier) }.getOrDefault(ClassifierTier.LITE),
        urlMetadata = urlMetadataJson?.let {
            runCatching {
                gson.fromJson(it, com.charles.scamradar.app.data.model.UrlScanMetadata::class.java)
            }.getOrNull()
        }
    )
}
