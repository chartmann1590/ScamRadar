package com.charles.scamradar.app.engagement

import android.content.Context
import com.charles.scamradar.app.data.datastore.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AchievementEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _unlocked = MutableSharedFlow<Achievement>(extraBufferCapacity = 8)
    val unlocked: SharedFlow<Achievement> = _unlocked.asSharedFlow()

    @Volatile private var stats = AchievementStats(0, 0, 0, 0, 0, 0, false, 0, 0)

    fun bootstrap(context: Context) {
        scope.launch {
            val prefs = UserPrefs(context.applicationContext)
            stats = AchievementStats(
                totalScans = prefs.scanCountToday.first(),
                totalScamsCaught = 0,
                currentStreak = prefs.currentStreak.first(),
                longestStreak = prefs.longestStreak.first(),
                quizCorrect = prefs.quizCorrectTotal.first(),
                familyShares = 0,
                shieldEnabled = prefs.shieldEnabled.first(),
                recoveryFlowsCompleted = 0,
                outboundShares = 0,
            )
        }
    }

    fun onScanCompleted(context: Context, isLikelyScam: Boolean) {
        scope.launch {
            val prefs = UserPrefs(context.applicationContext)
            stats = stats.copy(
                totalScans = stats.totalScans + 1,
                totalScamsCaught = stats.totalScamsCaught + if (isLikelyScam) 1 else 0,
            )
            evaluateAndUnlock(prefs)
        }
    }

    fun onFamilyShare(context: Context) {
        scope.launch {
            val prefs = UserPrefs(context.applicationContext)
            stats = stats.copy(familyShares = stats.familyShares + 1)
            evaluateAndUnlock(prefs)
        }
    }

    fun onOutboundShare(context: Context) {
        scope.launch {
            val prefs = UserPrefs(context.applicationContext)
            stats = stats.copy(outboundShares = stats.outboundShares + 1)
            evaluateAndUnlock(prefs)
        }
    }

    fun onRecoveryFlowCompleted(context: Context) {
        scope.launch {
            val prefs = UserPrefs(context.applicationContext)
            stats = stats.copy(recoveryFlowsCompleted = stats.recoveryFlowsCompleted + 1)
            evaluateAndUnlock(prefs)
        }
    }

    fun onShieldToggled(context: Context, enabled: Boolean) {
        scope.launch {
            val prefs = UserPrefs(context.applicationContext)
            stats = stats.copy(shieldEnabled = enabled)
            evaluateAndUnlock(prefs)
        }
    }

    fun onQuizCorrect(context: Context) {
        scope.launch {
            val prefs = UserPrefs(context.applicationContext)
            stats = stats.copy(quizCorrect = stats.quizCorrect + 1)
            evaluateAndUnlock(prefs)
        }
    }

    fun snapshot(): AchievementStats = stats

    private suspend fun evaluateAndUnlock(prefs: UserPrefs) {
        val current = prefs.unlockedAchievements.first().toMutableSet()
        val freshlyUnlocked = mutableListOf<Achievement>()
        AchievementCatalog.all.forEach { ach ->
            if (current.contains(ach.id)) return@forEach
            if (ach.predicate(stats)) {
                current += ach.id
                freshlyUnlocked += ach
            }
        }
        if (freshlyUnlocked.isNotEmpty()) {
            prefs.setUnlockedAchievements(current)
            freshlyUnlocked.forEach { _unlocked.tryEmit(it) }
        }
    }
}
