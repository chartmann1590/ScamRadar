package com.charles.scamradar.app.classifier

import android.content.Context
import com.charles.scamradar.app.data.model.ClassifierTier
import com.charles.scamradar.app.download.ModelManager

class ClassifierRouter(
    private val context: Context
) {
    private val liteClassifier: LiteClassifier by lazy { LiteClassifier() }
    private val gemmaClassifier: GemmaClassifier by lazy { GemmaClassifier.create(context) }

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
}
