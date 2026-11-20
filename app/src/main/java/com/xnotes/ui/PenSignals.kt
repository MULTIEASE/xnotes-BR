package com.xnotes.ui

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * xnotes BR: what the pen is telling the app, for the Vision Pen screen. It keeps a short log of
 * the raw signals (key codes and `buttonState` bits) so the user can see whether a side button
 * reaches the app at all, and it can learn a button: while [learning] is 1 or 2, the next key or
 * button bit that arrives is handed to [onLearned] as that button's signal.
 */
object PenSignals {
    val log = mutableStateListOf<String>()
    var learning by mutableIntStateOf(0)
    var lastHeard by mutableStateOf<String?>(null)

    /** Receives (button, keyCode or -1, buttonState bits or 0) when a button is learnt. */
    var onLearned: ((Int, Int, Int) -> Unit)? = null

    /** Set by the activity: hands events that reach another window (the settings dialog) to the pen gestures. */
    var forwardKey: ((KeyEvent) -> Unit)? = null
    var forwardMotion: ((MotionEvent) -> Unit)? = null

    private var lastMask = 0

    private val ignoredKeys = setOf(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.KEYCODE_POWER)

    fun onKey(e: KeyEvent) {
        if (e.repeatCount != 0 || e.keyCode in ignoredKeys) return
        val down = e.action == KeyEvent.ACTION_DOWN
        add("Tecla ${KeyEvent.keyCodeToString(e.keyCode).removePrefix("KEYCODE_")} (${e.keyCode}) ${if (down) "apertada" else "solta"}")
        if (down && learning != 0) {
            val b = learning
            learning = 0
            onLearned?.invoke(b, e.keyCode, 0)
        }
    }

    fun onMotion(e: MotionEvent) {
        val mask = e.buttonState and BUTTON_BITS
        if (mask == lastMask && e.actionMasked != MotionEvent.ACTION_BUTTON_PRESS) return
        val was = lastMask
        lastMask = mask
        val tool = when (e.getToolType(0)) {
            MotionEvent.TOOL_TYPE_STYLUS -> "caneta"
            MotionEvent.TOOL_TYPE_ERASER -> "borracha da caneta"
            MotionEvent.TOOL_TYPE_FINGER -> "dedo"
            MotionEvent.TOOL_TYPE_MOUSE -> "mouse"
            else -> "desconhecido"
        }
        if (mask != 0) {
            add("Botão: bits 0x${Integer.toHexString(mask)} (${names(mask)}) · $tool · ${MotionEvent.actionToString(e.actionMasked).removePrefix("ACTION_")}")
            val teachable = mask and MotionEvent.BUTTON_PRIMARY.inv()
            if (learning != 0 && teachable != 0 && (was and teachable) == 0) {
                val b = learning
                learning = 0
                onLearned?.invoke(b, -1, teachable)
            }
        } else if (was != 0) {
            add("Botão solto · $tool")
        }
    }

    private fun add(s: String) {
        lastHeard = s
        log.add(0, s)
        while (log.size > 8) log.removeAt(log.size - 1)
    }

    private const val BUTTON_BITS = MotionEvent.BUTTON_PRIMARY or MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_TERTIARY or
        MotionEvent.BUTTON_BACK or MotionEvent.BUTTON_FORWARD or MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_STYLUS_SECONDARY

    private fun names(mask: Int): String = buildList {
        if (mask and MotionEvent.BUTTON_STYLUS_PRIMARY != 0) add("caneta 1")
        if (mask and MotionEvent.BUTTON_STYLUS_SECONDARY != 0) add("caneta 2")
        if (mask and MotionEvent.BUTTON_SECONDARY != 0) add("secundário")
        if (mask and MotionEvent.BUTTON_TERTIARY != 0) add("terciário")
        if (mask and MotionEvent.BUTTON_PRIMARY != 0) add("primário")
        if (mask and MotionEvent.BUTTON_BACK != 0) add("voltar")
        if (mask and MotionEvent.BUTTON_FORWARD != 0) add("avançar")
    }.joinToString(", ")
}
