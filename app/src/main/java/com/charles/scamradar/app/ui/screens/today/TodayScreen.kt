package com.charles.scamradar.app.ui.screens.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.community.CommunityReportsRepository
import com.charles.scamradar.app.community.TrendingItem
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.engagement.BriefItem
import com.charles.scamradar.app.engagement.DailyBrief
import com.charles.scamradar.app.engagement.QuizQuestion
import com.charles.scamradar.app.engagement.TodayRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    userPrefs: UserPrefs,
    onOpenTrustScore: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { TodayRepository(context) }
    val communityRepo = remember { CommunityReportsRepository() }
    val brief = remember { repo.loadDailyBrief() }
    val question = remember { repo.questionForToday() }
    var trending by remember { mutableStateOf<List<TrendingItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        trending = communityRepo.loadTrending()
    }

    val coroutineScope = rememberCoroutineScope()
    val currentStreak by userPrefs.currentStreak.collectAsState(initial = 0)
    val lastQuizDate by userPrefs.lastQuizDate.collectAsState(initial = "")
    val today = remember { LocalDate.now().toString() }
    val alreadyAnsweredToday = lastQuizDate == today

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
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
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StreakBanner(currentStreak, onOpenTrustScore)
            if (question != null) {
                QuizCard(
                    question = question,
                    locked = alreadyAnsweredToday,
                    onAnswer = { wasCorrect ->
                        coroutineScope.launch {
                            updateQuizState(userPrefs, today, wasCorrect)
                        }
                    }
                )
            }
            DailyBriefSection(brief)
            TrendingCommunitySection(trending)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StreakBanner(streak: Int, onOpenStats: () -> Unit) {
    Surface(
        onClick = onOpenStats,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (streak > 0) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (streak > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (streak > 0) "$streak-day streak" else "Start your streak today",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Answer one scam-spotting quiz every day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Stats →",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun QuizCard(
    question: QuizQuestion,
    locked: Boolean,
    onAnswer: (Boolean) -> Unit
) {
    var answered by remember(question.id, locked) { mutableStateOf(locked) }
    var pickedScam by remember(question.id) { mutableStateOf<Boolean?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Spot the scam",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "One is real. One is a scam. Which is which?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val scamFirst = remember(question.id) { question.id.hashCode() % 2 == 0 }
            val first = if (scamFirst) question.scamMessage else question.safeMessage
            val firstIsScam = scamFirst
            val second = if (scamFirst) question.safeMessage else question.scamMessage
            val secondIsScam = !scamFirst

            QuizOption(
                label = "A",
                message = first,
                state = optionState(answered, picked = pickedScam, optionIsScam = firstIsScam),
                onClick = {
                    if (!answered) {
                        answered = true
                        pickedScam = firstIsScam
                        onAnswer(firstIsScam)
                    }
                }
            )
            QuizOption(
                label = "B",
                message = second,
                state = optionState(answered, picked = pickedScam, optionIsScam = secondIsScam),
                onClick = {
                    if (!answered) {
                        answered = true
                        pickedScam = secondIsScam
                        onAnswer(secondIsScam)
                    }
                }
            )

            if (answered) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Why",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (locked && !answered) {
                Text(
                    text = "Come back tomorrow for the next quiz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private enum class OptionState { NEUTRAL, CORRECT, WRONG, MUTED }

private fun optionState(
    answered: Boolean,
    picked: Boolean?,
    optionIsScam: Boolean
): OptionState {
    if (!answered) return OptionState.NEUTRAL
    return when {
        optionIsScam && picked == true -> OptionState.CORRECT
        optionIsScam && picked == false -> OptionState.CORRECT
        !optionIsScam && picked == false -> OptionState.WRONG
        !optionIsScam && picked == true -> OptionState.MUTED
        else -> OptionState.MUTED
    }
}

@Composable
private fun QuizOption(
    label: String,
    message: String,
    state: OptionState,
    onClick: () -> Unit
) {
    val targetContainer = when (state) {
        OptionState.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh
        OptionState.CORRECT -> Color(0xFF10B981).copy(alpha = 0.18f)
        OptionState.WRONG -> Color(0xFFEF4444).copy(alpha = 0.18f)
        OptionState.MUTED -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val container by animateColorAsState(targetContainer, label = "container")

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (state == OptionState.CORRECT) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981)
                )
            } else if (state == OptionState.WRONG) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
private fun DailyBriefSection(brief: DailyBrief) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.TrendingUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Scams trending this week",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Spacer(modifier = Modifier.height(2.dp))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(brief.items) { item ->
            BriefCard(item)
        }
    }
}

@Composable
private fun BriefCard(item: BriefItem) {
    val accent = runCatching { Color(android.graphics.Color.parseColor(item.color)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    Card(
        modifier = Modifier
            .width(280.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.tagline,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tells",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            item.tells.forEach { tell ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "•",
                        color = accent,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = tell,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingCommunitySection(items: List<TrendingItem>) {
    if (items.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.TrendingUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Community reports · last 7 days",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    items.take(5).forEachIndexed { index, item ->
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName(item.scamType),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.count7d} reports this week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun displayName(scamType: String): String {
    return scamType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

private suspend fun updateQuizState(
    prefs: UserPrefs,
    today: String,
    wasCorrect: Boolean
) {
    val last = prefs.lastQuizDate.first()
    val currentStreak = prefs.currentStreak.first()
    val longest = prefs.longestStreak.first()
    val answered = prefs.quizAnsweredTotal.first()
    val correct = prefs.quizCorrectTotal.first()

    val newStreak = when {
        last == today -> currentStreak
        isConsecutive(last, today) -> currentStreak + 1
        else -> 1
    }
    val newLongest = maxOf(longest, newStreak)

    prefs.setLastQuizDate(today)
    prefs.setCurrentStreak(newStreak)
    prefs.setLongestStreak(newLongest)
    prefs.setQuizAnsweredTotal(answered + 1)
    if (wasCorrect) prefs.setQuizCorrectTotal(correct + 1)
}

private fun isConsecutive(previous: String, today: String): Boolean {
    if (previous.isBlank()) return false
    return runCatching {
        val p = LocalDate.parse(previous)
        val t = LocalDate.parse(today)
        java.time.temporal.ChronoUnit.DAYS.between(p, t) == 1L
    }.getOrDefault(false)
}
