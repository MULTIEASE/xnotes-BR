package com.xnotes.canvas

import android.view.MotionEvent

/**
 * xnotes BR: the pen contact filter. Some tablets (the Positivo Vision 11 among them) report the
 * pen as touching while it is still a little above the glass, so ink starts in the air and keeps
 * going when the pen lifts slightly between words. This filter rewrites the pen's touch stream by
 * pressure, sample by sample:
 *
 *  - ink starts only once pressure reaches [minPressure] (and, on first contact, after [startDelayMs]);
 *  - ink stops as soon as pressure falls below [liftPressure], even if the tablet still says
 *    "touching", and starts again when pressure comes back: each word becomes its own stroke.
 *
 * It hands the rewritten events (DOWN / MOVE / UP / CANCEL, one sample each) to [emit]. Events it
 * does not filter (fingers, unless [alsoFingers]; several pointers) go to [emit] untouched.
 */
class PenContactFilter(private val emit: (MotionEvent) -> Unit) {
    var minPressure = 0f
    var startDelayMs = 0L
    var alsoFingers = false

    /** Pressure under which the pen counts as lifted: a little under the start, so ink does not flicker. */
    val liftPressure get() = minPressure * 0.82f

    val enabled get() = minPressure > 0f || startDelayMs > 0L

    /** Samples the filter held back since the last call, for the test sheet (x, y). */
    var onHeld: ((Float, Float) -> Unit)? = null

    private var physical = false
    private var inking = false
    private var contactAt = 0L
    private var inkDownTime = 0L
    private var firstX = 0f
    private var firstY = 0f
    private var everInked = false
    private val props = arrayOf(MotionEvent.PointerProperties())
    private val coords = arrayOf(MotionEvent.PointerCoords())

    /** Feed one real event; returns true when the filter took it (the caller must not use it). */
    fun feed(e: MotionEvent, wantsTap: () -> Boolean): Boolean {
        if (!enabled) return false
        val type = e.getToolType(0)
        val filtered = type == MotionEvent.TOOL_TYPE_STYLUS || (alsoFingers && type == MotionEvent.TOOL_TYPE_FINGER)
        if (!physical && (!filtered || e.actionMasked != MotionEvent.ACTION_DOWN)) return false
        if (physical && e.pointerCount > 1) {
            // A second pointer joined: stop what we drew and let the rest of the gesture be ignored.
            if (inking) { synth(e, -1, MotionEvent.ACTION_CANCEL); inking = false }
            if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) physical = false
            return true
        }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                physical = true
                inking = false
                everInked = false
                contactAt = e.eventTime
                firstX = e.x
                firstY = e.y
                sample(e, -1)
            }
            MotionEvent.ACTION_MOVE -> {
                for (h in 0 until e.historySize) sample(e, h)
                sample(e, -1)
            }
            MotionEvent.ACTION_UP -> {
                sample(e, -1)
                if (inking) {
                    synth(e, -1, MotionEvent.ACTION_UP)
                } else if (!everInked && wantsTap()) {
                    // A light tap that never inked still reaches tools that act on taps.
                    synthAt(e, firstX, firstY, MotionEvent.ACTION_DOWN, e.eventTime)
                    synthAt(e, firstX, firstY, MotionEvent.ACTION_UP, e.eventTime)
                }
                inking = false
                physical = false
            }
            MotionEvent.ACTION_CANCEL -> {
                if (inking) synth(e, -1, MotionEvent.ACTION_CANCEL)
                inking = false
                physical = false
            }
            else -> Unit
        }
        return true
    }

    private fun pressureAt(e: MotionEvent, h: Int) = if (h < 0) e.getPressure(0) else e.getHistoricalPressure(0, h)
    private fun timeAt(e: MotionEvent, h: Int) = if (h < 0) e.eventTime else e.getHistoricalEventTime(h)

    private fun sample(e: MotionEvent, h: Int) {
        val p = pressureAt(e, h)
        val t = timeAt(e, h)
        if (!inking) {
            val delayOk = everInked || t - contactAt >= startDelayMs
            if (p >= minPressure && delayOk) {
                inking = true
                everInked = true
                inkDownTime = t
                synth(e, h, MotionEvent.ACTION_DOWN)
            } else {
                onHeld?.invoke(if (h < 0) e.x else e.getHistoricalX(0, h), if (h < 0) e.y else e.getHistoricalY(0, h))
            }
        } else if (p < liftPressure && minPressure > 0f) {
            // The tip left the glass, whatever the tablet says: end this stroke here.
            synth(e, h, MotionEvent.ACTION_UP)
            inking = false
            onHeld?.invoke(if (h < 0) e.x else e.getHistoricalX(0, h), if (h < 0) e.y else e.getHistoricalY(0, h))
        } else if (e.actionMasked == MotionEvent.ACTION_MOVE) {
            synth(e, h, MotionEvent.ACTION_MOVE)
        }
    }

    private fun synth(e: MotionEvent, h: Int, action: Int) {
        e.getPointerProperties(0, props[0])
        if (h < 0) e.getPointerCoords(0, coords[0]) else e.getHistoricalPointerCoords(0, h, coords[0])
        val t = timeAt(e, h)
        val ev = MotionEvent.obtain(
            if (action == MotionEvent.ACTION_DOWN) t else inkDownTime, t, action, 1, props, coords,
            e.metaState, e.buttonState, e.xPrecision, e.yPrecision, e.deviceId, e.edgeFlags, e.source, e.flags,
        )
        try { emit(ev) } finally { ev.recycle() }
    }

    private fun synthAt(e: MotionEvent, x: Float, y: Float, action: Int, t: Long) {
        e.getPointerProperties(0, props[0])
        e.getPointerCoords(0, coords[0])
        coords[0].x = x
        coords[0].y = y
        coords[0].pressure = maxOf(coords[0].pressure, minPressure)
        val ev = MotionEvent.obtain(t, t, action, 1, props, coords, e.metaState, e.buttonState, e.xPrecision, e.yPrecision, e.deviceId, e.edgeFlags, e.source, e.flags)
        try { emit(ev) } finally { ev.recycle() }
    }
}
