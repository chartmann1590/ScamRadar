package com.charles.scamradar.app.engagement

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser

data class AchievementStats(
    val totalScans: Int,
    val totalScamsCaught: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val quizCorrect: Int,
    val familyShares: Int,
    val shieldEnabled: Boolean,
    val recoveryFlowsCompleted: Int,
    val outboundShares: Int,
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val predicate: (AchievementStats) -> Boolean,
)

object AchievementCatalog {

    val all: List<Achievement> = listOf(
        Achievement(
            id = "first_scan",
            title = "First scan",
            description = "Run your first scam check",
            icon = Icons.Default.Star,
            predicate = { it.totalScans >= 1 },
        ),
        Achievement(
            id = "first_scam",
            title = "First catch",
            description = "Identify your first likely scam",
            icon = Icons.Default.Shield,
            predicate = { it.totalScamsCaught >= 1 },
        ),
        Achievement(
            id = "streak_7",
            title = "Week strong",
            description = "Hit a 7-day streak",
            icon = Icons.Default.LocalFireDepartment,
            predicate = { it.longestStreak >= 7 || it.currentStreak >= 7 },
        ),
        Achievement(
            id = "streak_30",
            title = "Month strong",
            description = "Hit a 30-day streak",
            icon = Icons.Default.LocalFireDepartment,
            predicate = { it.longestStreak >= 30 || it.currentStreak >= 30 },
        ),
        Achievement(
            id = "century",
            title = "Century",
            description = "Complete 100 scans",
            icon = Icons.Default.MilitaryTech,
            predicate = { it.totalScans >= 100 },
        ),
        Achievement(
            id = "quiz_25",
            title = "Quiz scholar",
            description = "Answer 25 quizzes correctly",
            icon = Icons.Default.Quiz,
            predicate = { it.quizCorrect >= 25 },
        ),
        Achievement(
            id = "family_saver",
            title = "Family saver",
            description = "Share a likely scam to your family pod",
            icon = Icons.Default.Group,
            predicate = { it.familyShares >= 1 },
        ),
        Achievement(
            id = "shield_on",
            title = "Shield activated",
            description = "Turn on Live Shield",
            icon = Icons.Default.Security,
            predicate = { it.shieldEnabled },
        ),
        Achievement(
            id = "recovery_hero",
            title = "Recovery hero",
            description = "Complete a recovery flow end-to-end",
            icon = Icons.Default.VerifiedUser,
            predicate = { it.recoveryFlowsCompleted >= 1 },
        ),
        Achievement(
            id = "warned_friends",
            title = "Looking out",
            description = "Warn a friend or relative",
            icon = Icons.Default.EmojiEvents,
            predicate = { it.outboundShares >= 1 },
        ),
        Achievement(
            id = "ten_scams",
            title = "Ten caught",
            description = "Catch 10 likely scams",
            icon = Icons.Default.AutoAwesome,
            predicate = { it.totalScamsCaught >= 10 },
        ),
        Achievement(
            id = "fifty_scans",
            title = "Half-century",
            description = "Complete 50 scans",
            icon = Icons.Default.Star,
            predicate = { it.totalScans >= 50 },
        ),
        Achievement(
            id = "quiz_perfect_week",
            title = "Perfect week",
            description = "Answer 7 daily quizzes correctly in a row",
            icon = Icons.Default.Quiz,
            predicate = { it.quizCorrect >= 7 },
        ),
        Achievement(
            id = "share_streak",
            title = "Spread the word",
            description = "Warn family or friends 3 times",
            icon = Icons.Default.EmojiEvents,
            predicate = { it.outboundShares >= 3 },
        ),
        Achievement(
            id = "guardian",
            title = "Family guardian",
            description = "Share 5 scams with your pod",
            icon = Icons.Default.Group,
            predicate = { it.familyShares >= 5 },
        ),
    )
}
