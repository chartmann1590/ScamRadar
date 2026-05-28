package com.scamradar.app.speech

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SpeechProcessor {

    fun isOnDeviceSpeechAvailable(context: Context): Boolean {
        return SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    }

    suspend fun transcribeAudio(audioUri: Uri, context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return transcribeOnDevice(audioUri, context)
        }
        return transcribeLegacy(audioUri, context)
    }

    private suspend fun transcribeOnDevice(audioUri: Uri, context: Context): String {
        val speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
        return suspendCancellableCoroutine { continuation ->
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    continuation.resume("")
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    continuation.resume(matches?.firstOrNull() ?: "")
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speechRecognizer.startListening(intent)
            continuation.invokeOnCancellation {
                speechRecognizer.stopListening()
                speechRecognizer.destroy()
            }
        }
    }

    private suspend fun transcribeLegacy(audioUri: Uri, context: Context): String {
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        return suspendCancellableCoroutine { continuation ->
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    continuation.resume("")
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    continuation.resume(matches?.firstOrNull() ?: "")
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speechRecognizer.startListening(intent)
            continuation.invokeOnCancellation {
                speechRecognizer.stopListening()
                speechRecognizer.destroy()
            }
        }
    }

    fun transcribeFromAudioFile(filePath: String): String {
        TODO("SpeechRecognizer does not directly support audio file input. For the MVP, audio will need to be played through the speaker and captured via MediaRecorder + SpeechRecognizer pipeline. Implement MediaRecorder playback bridge for file-based transcription.")
    }
}
