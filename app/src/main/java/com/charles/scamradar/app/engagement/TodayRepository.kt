package com.charles.scamradar.app.engagement

import android.content.Context
import com.google.gson.Gson
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TodayRepository(private val context: Context) {

    private val gson = Gson()

    fun loadDailyBrief(): DailyBrief {
        return runCatching {
            val json = context.assets.open("daily_brief_seed.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, DailyBrief::class.java)
        }.getOrDefault(DailyBrief("", emptyList()))
    }

    fun loadQuizPool(): QuizPool {
        return runCatching {
            val json = context.assets.open("quiz_questions.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, QuizPool::class.java)
        }.getOrDefault(QuizPool(emptyList()))
    }

    /** Picks today's quiz question by hashing the current date into the question pool. */
    fun questionForToday(): QuizQuestion? {
        val pool = loadQuizPool().questions
        if (pool.isEmpty()) return null
        val today = LocalDate.now()
        val seed = today.year * 1000 + today.dayOfYear
        return pool[(seed % pool.size).let { if (it < 0) it + pool.size else it }]
    }

    companion object {
        val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
