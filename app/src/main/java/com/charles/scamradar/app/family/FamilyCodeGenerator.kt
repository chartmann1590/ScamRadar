package com.charles.scamradar.app.family

import android.content.Context
import java.util.Locale
import kotlin.random.Random

class FamilyCodeGenerator(private val context: Context) {

    private val adjectives: List<String> by lazy { readLines("family_words/adjectives.txt") }
    private val nouns: List<String> by lazy { readLines("family_words/nouns.txt") }

    fun generate(): String {
        val adjective = adjectives.random()
        val noun = nouns.random()
        return "${adjective.uppercase(Locale.US)}-${noun.uppercase(Locale.US)}"
    }

    fun generateWithCollisionSuffix(base: String): String {
        val suffix = String.format(Locale.US, "%02d", Random.nextInt(1, 100))
        return "$base-$suffix"
    }

    fun isValidFormat(code: String): Boolean {
        val trimmed = code.trim().uppercase(Locale.US)
        if (trimmed.isEmpty()) return false
        val parts = trimmed.split("-")
        return parts.size in 2..3 && parts.all { it.matches(Regex("[A-Z0-9]+")) }
    }

    fun normalize(code: String): String = code.trim().uppercase(Locale.US)

    private fun readLines(path: String): List<String> {
        return runCatching {
            context.assets.open(path).bufferedReader().useLines { seq ->
                seq.map { it.trim() }.filter { it.isNotEmpty() }.toList()
            }
        }.getOrDefault(emptyList())
    }
}
