package com.charles.scamradar.app.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val unlocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustScoreScreen(
    userPrefs: UserPrefs,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val streak by userPrefs.currentStreak.collectAsState(initial = 0)
    val longest by userPrefs.longestStreak.collectAsState(initial = 0)
    val answered by userPrefs.quizAnsweredTotal.collectAsState(initial = 0)
    val correct by userPrefs.quizCorrectTotal.collectAsState(initial = 0)

    var scansTotal by remember { mutableIntStateOf(0) }
    var scansFlagged by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val items = database.scanHistoryDao().getAll().first()
        scansTotal = items.size
        scansFlagged = items.count { it.verdict != "SAFE" }
    }

    val accuracy = if (answered > 0) (correct.toFloat() / answered.toFloat()) else 0f
    val trustScore = computeTrustScore(
        scansTotal = scansTotal,
        accuracy = accuracy,
        streak = streak
    )
    val achievements = computeAchievements(
        scansTotal = scansTotal,
        scansFlagged = scansFlagged,
        accuracy = accuracy,
        currentStreak = streak,
        longestStreak = longest,
        quizAnswered = answered
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TrustScoreHero(trustScore)
            StatGrid(
                scansTotal = scansTotal,
                scansFlagged = scansFlagged,
                streak = streak,
                longest = longest,
                accuracy = accuracy
            )
            AchievementsSection(achievements)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TrustScoreHero(score: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Trust Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = trustLabel(score),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StatGrid(
    scansTotal: Int,
    scansFlagged: Int,
    streak: Int,
    longest: Int,
    accuracy: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatTile("Scans", scansTotal.toString(), modifier = Modifier.weight(1f))
        StatTile("Flagged", scansFlagged.toString(), modifier = Modifier.weight(1f))
        StatTile("Streak", "$streak d", modifier = Modifier.weight(1f))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatTile("Longest streak", "$longest d", modifier = Modifier.weight(1f))
        StatTile("Quiz accuracy", "${(accuracy * 100).toInt()}%", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AchievementsSection(achievements: List<Achievement>) {
    Text(
        text = "Achievements",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    achievements.forEach { item ->
        AchievementRow(item)
    }
}

@Composable
private fun AchievementRow(achievement: Achievement) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.unlocked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (achievement.unlocked) achievement.icon else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (achievement.unlocked) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun computeTrustScore(scansTotal: Int, accuracy: Float, streak: Int): Int {
    val scanComponent = (scansTotal * 2).coerceAtMost(40)
    val accuracyComponent = (accuracy * 40).toInt()
    val streakComponent = (streak * 4).coerceAtMost(20)
    return (scanComponent + accuracyComponent + streakComponent).coerceIn(0, 100)
}

private fun trustLabel(score: Int): String = when {
    score >= 80 -> "Scam-aware pro"
    score >= 60 -> "Sharp eye"
    score >= 30 -> "Learning fast"
    score > 0 -> "Just getting started"
    else -> "Welcome — answer one quiz to begin"
}

private fun computeAchievements(
    scansTotal: Int,
    scansFlagged: Int,
    accuracy: Float,
    currentStreak: Int,
    longestStreak: Int,
    quizAnswered: Int
): List<Achievement> {
    return listOf(
        Achievement(
            id = "first-scan",
            title = "First scan",
            description = "Scan any message",
            icon = Icons.Default.Shield,
            unlocked = scansTotal >= 1
        ),
        Achievement(
            id = "ten-scans",
            title = "Vigilant",
            description = "Scan 10 messages",
            icon = Icons.Default.EmojiEvents,
            unlocked = scansTotal >= 10
        ),
        Achievement(
            id = "first-block",
            title = "First scam caught",
            description = "Catch a likely scam",
            icon = Icons.Default.Shield,
            unlocked = scansFlagged >= 1
        ),
        Achievement(
            id = "week-streak",
            title = "7-day streak",
            description = "Answer the quiz 7 days in a row",
            icon = Icons.Default.LocalFireDepartment,
            unlocked = longestStreak >= 7
        ),
        Achievement(
            id = "quiz-accuracy",
            title = "Sharpshooter",
            description = "80% quiz accuracy across 10+ questions",
            icon = Icons.Default.WorkspacePremium,
            unlocked = quizAnswered >= 10 && accuracy >= 0.8f
        )
    )
}
