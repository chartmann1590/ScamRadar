package com.charles.scamradar.app.classifier

import android.content.Context
import com.charles.scamradar.app.data.model.ClassifierTier
import java.io.File

class ClassifierRouter(
    private val context: Context
) {
    private val modelFile = File(File(context.filesDir, "models"), "gemma-4-E2B-it.litertlm")

    private val liteClassifier: LiteClassifier by lazy { LiteClassifier() }
    private val gemmaClassifier: GemmaClassifier by lazy { GemmaClassifier.create(context) }

    fun selectClassifier(): ScamClassifier {
        return if (modelFile.exists() && modelFile.length() > 0L) {
            gemmaClassifier
        } else {
            liteClassifier
        }
    }

    fun currentTier(): ClassifierTier {
        return if (modelFile.exists() && modelFile.length() > 0L) {
            ClassifierTier.GEMMA
        } else {
            ClassifierTier.LITE
        }
    }
}
