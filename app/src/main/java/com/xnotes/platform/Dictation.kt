package com.xnotes.platform

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * xnotes BR: continuous dictation. Listens in Brazilian Portuguese (on the device when it can),
 * hands each finished sentence to [onText] and starts listening again until [stop].
 * Must be used from the main thread.
 */
class Dictation(
    private val context: Context,
    private val onText: (String) -> Unit,
    private val onState: (Boolean) -> Unit,
    private val onError: (String) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null
    var listening = false
        private set

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        if (listening) return
        if (!available()) { onError("unavailable"); return }
        listening = true
        onState(true)
        listen()
    }

    fun stop() {
        listening = false
        recognizer?.let { runCatching { it.stopListening(); it.destroy() } }
        recognizer = null
        onState(false)
    }

    private fun listen() {
        if (!listening) return
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            recognizer = it
            it.setRecognitionListener(listener)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }
        runCatching { r.startListening(intent) }.onFailure { onError("start"); stop() }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
            if (text.isNotEmpty()) onText(text)
            listen()
        }

        override fun onError(error: Int) {
            when (error) {
                // Silence or nothing understood: just keep listening.
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> listen()
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    recognizer?.let { runCatching { it.destroy() } }
                    recognizer = null
                    listen()
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> { onError("permission"); stop() }
                else -> { onError("error$error"); stop() }
            }
        }
    }
}
