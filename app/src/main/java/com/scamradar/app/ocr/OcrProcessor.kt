package com.scamradar.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class OcrProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(imageUri: Uri, context: Context): String {
        return try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            suspendCancellableCoroutine { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(visionText.text)
                    }
                    .addOnFailureListener { _ ->
                        continuation.resume("")
                    }
                continuation.invokeOnCancellation {
                    recognizer.close()
                }
            }
        } catch (_: Exception) {
            ""
        } finally {
            runCatching { recognizer.close() }
        }
    }
}
