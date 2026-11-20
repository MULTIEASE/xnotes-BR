package com.xnotes.canvas

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration

/**
 * Whether a stylus side button is currently down.
 *
 * Pens do not agree on how to report this, and getting it right took several releases of
 * device-specific work, so both canvases latch it through this rather than each working it out.
 * Three delivery routes are covered:
 *
 *  - the touch stream's `buttonState`, which most pens use;
 *  - the hovering generic-motion stream, for pens that report the button only while hovering and
 *    never in a touch event;
 *  - a `KeyEvent`, which Bluetooth and USI pens use, including two vendor codes with no standard
 *    mapping ([InteractionController.VENDOR_HELD_BUTTON_KEYCODE] and the Xiaomi Focus Pen Pro's
 *    pinch, [InteractionController.FOCUS_PEN_PRO_PINCH_KEYCODE]).
 *
 * The last two are latched apart and either one holds the button: a pen that sends the key can
 * hover with an empty `buttonState` while it is down (the OnePlus Pad Go 2 Stylo does), and that
 * must not let go of the key.
 *
 * A fourth quirk is handled higher up and needs nothing here: some Samsung builds tag a
 * button-held stroke with proprietary action codes instead of DOWN/MOVE/UP, and `EditorActivity`
 * rewrites those before dispatch, so any view in the tree gets a normal touch stream.
 *
 * The button masks and the vendor keycode are read from [InteractionController] rather than copied,
 * so the two canvases cannot drift apart on which buttons count.
 */
class StylusButtonLatch {

    private var motionHeld = false

    /** True while a side button is held, from whichever stream reported it. */
    val held: Boolean
        get() = keyHeld || motionHeld

    /**
     * Latch the button off the generic-motion (hover) stream. Returns true when a stylus event
     * leaves the button up, so a caller can end a hovering gesture.
     */
    fun onGenericMotion(e: MotionEvent): Boolean =
        e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS && onStylusMotion(e.buttonState)

    internal fun onStylusMotion(buttonState: Int): Boolean {
        motionHeld = (buttonState and buttonMask()) != 0
        return !held
    }

    /**
     * Latch the button off a key event. Returns true when [keyCode] was a stylus side button, so
     * the host consumes it instead of letting it fall through as a normal key.
     */
    fun onKey(keyCode: Int, down: Boolean): Boolean {
        if (!isStylusButtonKey(keyCode)) return false
        keyHeld = down
        if (down) keySecondary = keyCode == KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY || keyCode == KeyEvent.KEYCODE_STYLUS_BUTTON_TERTIARY ||
            learnedKeys[keyCode] == 2
        return true
    }

    /** Forget what the hover stream said; a held key belongs to the pen and its key-up clears it. */
    fun reset() {
        motionHeld = false
    }

    /** Whether [e] is a stylus contact with the side button down, on either route. */
    fun heldFor(e: MotionEvent): Boolean =
        e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS && heldWith(e.buttonState)

    internal fun heldWith(buttonState: Int): Boolean =
        (buttonState and buttonMask()) != 0 || held

    /** xnotes BR: whether the button held for [e] is the pen's second (upper) button. */
    fun secondaryFor(e: MotionEvent): Boolean =
        (e.buttonState and SECONDARY_BITS) != 0 || (keyHeld && keySecondary && (e.buttonState and PRIMARY_BITS) == 0)

    companion object {
        /** One pen, so one key state for every latch: its key-up can reach another pane than its key-down. */
        private var keyHeld = false
        private var keySecondary = false

        /** xnotes BR: key codes the user taught the app for each side button (key code to button 1 or 2). */
        @Volatile var learnedKeys: Map<Int, Int> = emptyMap()

        /** xnotes BR: `buttonState` bits the user taught the app for button 1 and button 2. */
        @Volatile var learnedBits1 = 0
        @Volatile var learnedBits2 = 0

        /** Every bit that counts as a side button, the standard ones and any the user taught. */
        fun buttonMask(): Int = InteractionController.STYLUS_BUTTON_MASK or learnedBits1 or learnedBits2

        /** Bits that mean the first (lower) side button. */
        val PRIMARY_BITS: Int
            get() = if (learnedBits1 != 0) learnedBits1 else (MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_SECONDARY) and learnedBits2.inv()

        /** Bits that mean the second (upper) side button. */
        val SECONDARY_BITS: Int
            get() = if (learnedBits2 != 0) learnedBits2 else (MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_TERTIARY) and learnedBits1.inv()

        /** The same pen's button clicks, kept once for the same reason; see [ButtonClick]. */
        val clicks = ButtonClick { ViewConfiguration.getLongPressTimeout().toLong() }

        /** Let go of a held key whose key-up may never arrive, as when the window loses focus. */
        fun releaseKey() {
            keyHeld = false
            clicks.cancel()
        }

        fun isStylusButtonKey(keyCode: Int): Boolean =
            keyCode == KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY ||
                keyCode == KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY ||
                keyCode == KeyEvent.KEYCODE_STYLUS_BUTTON_TERTIARY ||
                keyCode == KeyEvent.KEYCODE_STYLUS_BUTTON_TAIL ||
                keyCode == InteractionController.VENDOR_HELD_BUTTON_KEYCODE ||
                keyCode == InteractionController.FOCUS_PEN_PRO_PINCH_KEYCODE ||
                keyCode in learnedKeys
    }
}

/**
 * Tells a click of a side button from a hold, for a button whose hardware reports both when it
 * goes down and when it comes up (the OnePlus Pad Go 2 Stylo's F21 and the Xiaomi Focus Pen Pro's
 * pinch do).
 *
 * Such a button can be held and clicked, so the user gets both: held while the pen touches the
 * screen, it is the side button's hold tool through [StylusButtonLatch]; clicked, it runs the
 * "Stylus side button (tap)" action. A pen whose button reports only clicks (Honor, Lenovo,
 * Redmi) or only a hold keeps the one it has, and another pen with both joins by adding its key
 * to the editor's click list.
 *
 * A click is let go within the long-press timeout with the pen tip off the screen throughout, so
 * the hold never acted on it. A hold that works without touching (hovering) waits out that window
 * through [mayStillClick], so a click never erases under a hovering pen.
 */
class ButtonClick(private val timeoutMs: () -> Long) {

    private var pressedAt = NONE
    private var touched = false
    private var touching = false

    /** The button went down at [at] (uptime ms). */
    fun onPress(at: Long) {
        pressedAt = at
        touched = touching
    }

    /** The button came up at [at]; true when the press was a click. */
    fun onRelease(at: Long): Boolean {
        val click = mayStillClick(at)
        pressedAt = NONE
        return click
    }

    /** Drop the press in progress, so its release cannot click. */
    fun cancel() {
        pressedAt = NONE
    }

    /** Whether the press in progress may still turn out a click at [at], so a hold must not act on it yet. */
    fun mayStillClick(at: Long): Boolean = pressedAt != NONE && !touched && at - pressedAt < timeoutMs()

    /** Follow the pen tip on and off the screen, from every touch the window gets. */
    fun onTouch(e: MotionEvent) = onTouch(e.actionMasked, e.getToolType(e.actionIndex))

    internal fun onTouch(action: Int, toolType: Int) {
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> if (toolType == MotionEvent.TOOL_TYPE_STYLUS) {
                touching = true
                touched = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> if (toolType == MotionEvent.TOOL_TYPE_STYLUS) touching = false
            MotionEvent.ACTION_CANCEL -> touching = false
        }
    }

    private companion object {
        const val NONE = -1L
    }
}


/**
 * xnotes BR: tells clicks and double clicks of each of the pen's two side buttons apart, from
 * whichever stream reports them (hover `buttonState`, `ACTION_BUTTON_PRESS/RELEASE`, or stylus
 * keys). A press while the tip touches the screen, or one held past the long-press timeout, is a
 * hold and never a click. [onGesture] receives the button (1 or 2) and whether it was a double click.
 */
class PenButtonGestures(
    private val longPressMs: () -> Long,
    private val wantsDouble: (Int) -> Boolean,
    private val onGesture: (button: Int, double: Boolean) -> Unit,
) {
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val down = BooleanArray(3)
    private val downAt = LongArray(3)
    private val touchedDuring = BooleanArray(3)
    private val lastClickAt = LongArray(3)
    private val pending = arrayOfNulls<Runnable>(3)
    private var touching = false
    private var lastMask = 0

    fun press(button: Int, at: Long) {
        if (down[button]) return
        down[button] = true
        downAt[button] = at
        touchedDuring[button] = touching
    }

    fun release(button: Int, at: Long) {
        if (!down[button]) return
        down[button] = false
        if (touchedDuring[button] || at - downAt[button] > longPressMs()) return
        val last = lastClickAt[button]
        if (last > 0 && at - last <= DOUBLE_MS && wantsDouble(button)) {
            pending[button]?.let { handler.removeCallbacks(it) }
            pending[button] = null
            lastClickAt[button] = 0
            onGesture(button, true)
            return
        }
        lastClickAt[button] = at
        if (wantsDouble(button)) {
            val r = Runnable { pending[button] = null; onGesture(button, false) }
            pending[button] = r
            handler.postDelayed(r, DOUBLE_MS)
        } else {
            onGesture(button, false)
        }
    }

    /** Follow a full button mask (hover or touch `buttonState`). */
    fun onMask(mask: Int, at: Long) {
        val p1 = StylusButtonLatch.PRIMARY_BITS
        val p2 = StylusButtonLatch.SECONDARY_BITS
        val b1 = (mask and p1) != 0
        val b2 = (mask and p2) != 0
        val was1 = (lastMask and p1) != 0
        val was2 = (lastMask and p2) != 0
        if (b1 && !was1) press(1, at)
        if (!b1 && was1) release(1, at)
        if (b2 && !was2) press(2, at)
        if (!b2 && was2) release(2, at)
        lastMask = mask
    }

    fun onMotion(e: MotionEvent) {
        // xnotes BR: some pens (the Positivo Vision Pen among them) may not call themselves a stylus;
        // follow any pointer that carries side-button bits, and stylus contacts for the hold rule.
        if (e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS && (e.buttonState and StylusButtonLatch.buttonMask()) == 0 && lastMask == 0) return
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                touching = true
                for (b in 1..2) if (down[b]) touchedDuring[b] = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> touching = false
        }
        onMask(e.buttonState, e.eventTime)
    }

    fun onKey(keyCode: Int, isDown: Boolean, at: Long) {
        val button = StylusButtonLatch.learnedKeys[keyCode] ?: when (keyCode) {
            KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY -> 1
            KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, KeyEvent.KEYCODE_STYLUS_BUTTON_TERTIARY -> 2
            else -> return
        }
        if (isDown) press(button, at) else release(button, at)
    }

    companion object {
        const val DOUBLE_MS = 350L
    }
}
