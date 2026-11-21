package com.xnotes.platform

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionContext
import com.google.mlkit.vision.digitalink.WritingArea

/**
 * xnotes BR: handwriting recognition in Brazilian Portuguese, on the tablet (Google ML Kit
 * Digital Ink). The language model, about 20 MB, is downloaded once; after that it works offline.
 */
class InkRecognizer {
    private val model: DigitalInkRecognitionModel? = runCatching {
        val id = DigitalInkRecognitionModelIdentifier.fromLanguageTag("pt-BR")
            ?: DigitalInkRecognitionModelIdentifier.fromLanguageTag("pt")
        id?.let { DigitalInkRecognitionModel.builder(it).build() }
    }.getOrNull()
    private var recognizer: DigitalInkRecognizer? = null

    /** Make sure the model is on the tablet, downloading it when needed. [done] gets true when ready. */
    fun ensureModel(done: (Boolean) -> Unit) {
        val m = model ?: return done(false)
        val manager = RemoteModelManager.getInstance()
        manager.isModelDownloaded(m)
            .addOnSuccessListener { have ->
                if (have) done(true)
                else manager.download(m, DownloadConditions.Builder().build())
                    .addOnSuccessListener { done(true) }
                    .addOnFailureListener { done(false) }
            }
            .addOnFailureListener { done(false) }
    }

    /**
     * Read [strokes], each a list of (x, y, timeMs) points, written in an area [width] × [height].
     * [done] gets the most likely text, or null.
     */
    fun recognize(strokes: List<List<Triple<Float, Float, Long>>>, width: Float, height: Float, done: (String?) -> Unit) {
        val m = model ?: return done(null)
        val r = recognizer ?: DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(m).build()).also { recognizer = it }
        val ink = Ink.builder()
        for (s in strokes) {
            if (s.isEmpty()) continue
            val sb = Ink.Stroke.builder()
            for ((x, y, t) in s) sb.addPoint(Ink.Point.create(x, y, t))
            ink.addStroke(sb.build())
        }
        val context = RecognitionContext.builder()
            .setPreContext("")
            .setWritingArea(WritingArea(width.coerceAtLeast(1f), height.coerceAtLeast(1f)))
            .build()
        r.recognize(ink.build(), context)
            .addOnSuccessListener { result -> done(result.candidates.firstOrNull()?.text?.trim()?.takeIf { it.isNotEmpty() }) }
            .addOnFailureListener { done(null) }
    }

    fun close() {
        recognizer?.close()
        recognizer = null
    }
}
