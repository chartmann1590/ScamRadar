package com.charles.scamradar.app.classifier

import android.content.Context
import com.charles.scamradar.app.data.datastore.FeedbackStore
import com.charles.scamradar.app.data.model.ClassifierTier
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.download.ModelManager
import kotlinx.coroutines.flow.first

class ClassifierRouter(
    private val context: Context
) {
    private val liteClassifier: LiteClassifier by lazy { LiteClassifier() }
    private val gemmaClassifier: GemmaClassifier by lazy { GemmaClassifier.create(context) }
    private val feedbackStore: FeedbackStore by lazy { FeedbackStore(context) }

    fun selectClassifier(): ScamClassifier {
        return if (ModelManager.isModelDownloaded(context)) {
            gemmaClassifier
        } else {
            liteClassifier
        }
    }

    fun liteOnly(): ScamClassifier = liteClassifier

    fun currentTier(): ClassifierTier {
        return if (ModelManager.isModelDownloaded(context)) {
            ClassifierTier.GEMMA
        } else {
            ClassifierTier.LITE
        }
    }

    /**
     * Softens [result]'s verdict if the user has previously told ScamRadar that a
     * similar message wasn't actually a scam. See [FeedbackLearningEngine].
     */
    suspend fun applyLearnedFeedback(result: ScanResult): ScanResult {
        val signatures = feedbackStore.falsePositiveSignatures.first()
        return FeedbackLearningEngine.adjustForFeedback(result, signatures)
    }
}
