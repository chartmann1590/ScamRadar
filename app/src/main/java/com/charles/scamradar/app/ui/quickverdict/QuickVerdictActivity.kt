package com.charles.scamradar.app.ui.quickverdict

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.charles.scamradar.app.MainActivity
import com.charles.scamradar.app.analytics.Analytics
import com.charles.scamradar.app.classifier.ClassifierRouter
import com.charles.scamradar.app.data.db.AppDatabase
import com.charles.scamradar.app.data.db.ScanHistoryEntity
import com.charles.scamradar.app.data.model.ScanMode
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.ocr.OcrProcessor
import com.charles.scamradar.app.share.buildShareText
import com.charles.scamradar.app.share.shareText
import com.charles.scamradar.app.ui.theme.ScamRadarTheme
import com.charles.scamradar.app.widget.ScamRadarWidget
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Transparent activity that handles ACTION_SEND intents from the system share sheet.
 * Renders a ModalBottomSheet overlay so the user gets a verdict in 2–3 seconds without
 * leaving their current app context.
 */
class QuickVerdictActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val incoming = parseIntent(intent)
        if (incoming == null) {
            finish()
            return
        }

        val classifierRouter = ClassifierRouter(applicationContext)
        val database = AppDatabase.getInstance(applicationContext)
        val ocrProcessor = OcrProcessor()

        setContent {
            ScamRadarTheme {
                var state by remember { mutableStateOf<QuickVerdictState>(QuickVerdictState.Loading) }

                LaunchedEffect(incoming) {
                    state = runScan(
                        incoming = incoming,
                        scope = this,
                        classifierRouter = classifierRouter,
                        ocrProcessor = ocrProcessor,
                        onPersist = { result, mode ->
                            withContext(Dispatchers.IO) {
                                database.scanHistoryDao().insert(ScanHistoryEntity(result, mode))
                                ScamRadarWidget.refreshAll(applicationContext)
                            }
                        }
                    )
                }

                QuickVerdictBottomSheet(
                    state = state,
                    onDismiss = { finish() },
                    onShareVerdict = { result ->
                        Analytics.shareCardExported(result.verdict, "text")
                        shareText(this, buildShareText(result))
                    },
                    onSeeFullReport = { result ->
                        openFullReport(result)
                    }
                )
            }
        }
    }

    private fun openFullReport(result: ScanResult) {
        val json = Gson().toJson(result)
        val deepLink = Uri.parse("scamradar://result?payload=${Uri.encode(json)}")
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = deepLink
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(mainIntent)
        finish()
    }

    private fun parseIntent(intent: Intent?): QuickIncoming? {
        intent ?: return null
        if (intent.action != Intent.ACTION_SEND) return null
        val type = intent.type ?: return null

        return when {
            type.startsWith("text/") -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
                if (text.isEmpty()) null else QuickIncoming.Text(text)
            }
            type.startsWith("image/") -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return null
                QuickIncoming.Image(uri)
            }
            else -> null
        }
    }

    private suspend fun runScan(
        incoming: QuickIncoming,
        scope: CoroutineScope,
        classifierRouter: ClassifierRouter,
        ocrProcessor: OcrProcessor,
        onPersist: suspend (ScanResult, ScanMode) -> Unit
    ): QuickVerdictState {
        return try {
            val (text, mode) = when (incoming) {
                is QuickIncoming.Text -> incoming.text to ScanMode.TEXT
                is QuickIncoming.Image -> {
                    val extracted = ocrProcessor.recognizeText(incoming.uri, applicationContext)
                    if (extracted.isBlank()) {
                        return QuickVerdictState.Failure(
                            "We couldn't find any text in that image. Try a clearer screenshot."
                        )
                    }
                    extracted to ScanMode.OCR
                }
            }

            val tier = classifierRouter.currentTier()
            Analytics.scanStarted(mode, tier)
            val started = System.currentTimeMillis()
            val result = classifierRouter.selectClassifier().classify(text)
            Analytics.scanCompleted(
                verdict = result.verdict,
                scamType = result.scamType,
                tier = result.classifierTier,
                durationMs = System.currentTimeMillis() - started
            )

            onPersist(result, mode)
            QuickVerdictState.Result(result)
        } catch (e: Exception) {
            QuickVerdictState.Failure(
                e.message?.takeIf { it.isNotBlank() } ?: "Analysis failed unexpectedly."
            )
        }
    }

    private sealed interface QuickIncoming {
        data class Text(val text: String) : QuickIncoming
        data class Image(val uri: Uri) : QuickIncoming
    }
}
