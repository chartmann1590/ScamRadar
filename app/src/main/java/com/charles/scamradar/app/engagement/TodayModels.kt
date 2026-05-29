package com.charles.scamradar.app.engagement

data class DailyBrief(
    val weekStarting: String,
    val items: List<BriefItem>
)

data class BriefItem(
    val id: String,
    val title: String,
    val tagline: String,
    val summary: String,
    val tells: List<String>,
    val color: String
)

data class QuizQuestion(
    val id: String,
    val scamMessage: String,
    val safeMessage: String,
    val explanation: String
)

data class QuizPool(
    val questions: List<QuizQuestion>
)
