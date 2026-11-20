package com.xnotes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xnotes.R
import com.xnotes.settings.Preferences
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlin.math.roundToInt

private val BUTTON1 = Color(0xFF1D4ED8)
private val BUTTON2 = Color(0xFFEA7A1A)

/**
 * xnotes BR: the Vision Pen screen. A drawing of the pen with its two buttons, one card per button
 * (click, double click, hold), a live test strip that shows which button the app heard, and the
 * width step and notice settings. While it is open, button clicks are only shown, never run.
 */
@Composable
fun VisionPenScreen(editor: Editor, prefs: Preferences, update: (Preferences) -> Unit, onClose: () -> Unit) {
    val palette = LocalPalette.current
    val text = palette.text.toComposeColor()
    val dim = palette.textDim.toComposeColor()
    val line = palette.border.toComposeColor()
    val card = palette.panel.toComposeColor()
    val accent = palette.accent.toComposeColor()
    val tapOptions = tapGestureOptions.map { (id, res) -> id to stringResource(res) }

    DisposableEffect(Unit) {
        editor.penTestMode = true
        onDispose { editor.penTestMode = false; PenSignals.learning = 0 }
    }
    val latest = androidx.compose.runtime.rememberUpdatedState(prefs)
    DisposableEffect(Unit) {
        PenSignals.onLearned = { button, key, bits ->
            val p = latest.value
            update(
                if (button == 1) p.copy(penButton1Key = key, penButton1Bits = bits)
                else p.copy(penButton2Key = key, penButton2Bits = bits),
            )
        }
        onDispose { PenSignals.onLearned = null }
    }
    var touchScreen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (touchScreen) PenTouchScreen(prefs, update) { touchScreen = false }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CaptureDialogPenEvents()
        Box(Modifier.fillMaxSize().background(palette.bg.toComposeColor()), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 980.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.pen_buttons_title), color = text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.pen_screen_subtitle), color = dim, fontSize = 14.sp)
                    }
                    Text(
                        stringResource(R.string.done), color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onClose).padding(12.dp),
                    )
                }

                // The pen.
                Box(
                    Modifier.fillMaxWidth().height(130.dp).background(card, RoundedCornerShape(18.dp)).border(1.dp, line, RoundedCornerShape(18.dp)),
                ) {
                    PenDrawing(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp))
                    Text(
                        stringResource(R.string.pen_button_1_lower), color = BUTTON1, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 140.dp, top = 12.dp),
                    )
                    Text(
                        stringResource(R.string.pen_button_2_upper), color = BUTTON2, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 230.dp, bottom = 12.dp),
                    )
                }

                // Teach the app each button, and see the raw signals the pen sends.
                Column(
                    Modifier.fillMaxWidth().background(card, RoundedCornerShape(18.dp)).border(1.dp, line, RoundedCornerShape(18.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(stringResource(R.string.pen_learn_title), color = text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.pen_learn_help), color = dim, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (button in 1..2) {
                            val colour = if (button == 1) BUTTON1 else BUTTON2
                            val learning = PenSignals.learning == button
                            val key = if (button == 1) prefs.penButton1Key else prefs.penButton2Key
                            val bits = if (button == 1) prefs.penButton1Bits else prefs.penButton2Bits
                            Column(
                                Modifier.weight(1f)
                                    .background(if (learning) colour.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(14.dp))
                                    .border(BorderStroke(if (learning) 2.dp else 1.dp, if (learning) colour else line), RoundedCornerShape(14.dp))
                                    .clickable { PenSignals.learning = if (learning) 0 else button }
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    stringResource(if (learning) R.string.pen_learn_now else R.string.pen_learn_button, button),
                                    color = if (learning) colour else text, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    when {
                                        key >= 0 -> stringResource(R.string.pen_learned_key, android.view.KeyEvent.keyCodeToString(key).removePrefix("KEYCODE_"), key)
                                        bits != 0 -> stringResource(R.string.pen_learned_bits, "0x" + Integer.toHexString(bits))
                                        else -> stringResource(R.string.pen_learned_none)
                                    },
                                    color = dim, fontSize = 12.sp,
                                )
                                if (key >= 0 || bits != 0) {
                                    Text(
                                        stringResource(R.string.pen_learn_forget), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            update(if (button == 1) prefs.copy(penButton1Key = -1, penButton1Bits = 0) else prefs.copy(penButton2Key = -1, penButton2Bits = 0))
                                        }.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                    Text(stringResource(R.string.pen_signals_title), color = text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Column(
                        Modifier.fillMaxWidth().background(Color(0xFF1F2328), RoundedCornerShape(10.dp)).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (PenSignals.log.isEmpty()) {
                            Text(stringResource(R.string.pen_signals_empty), color = Color(0xFFC9CDD3), fontSize = 12.sp)
                        }
                        PenSignals.log.forEach { Text(it, color = Color(0xFFE6E8EB), fontSize = 12.sp) }
                    }
                }

                // Touch fine tuning.
                Row(
                    Modifier.fillMaxWidth().background(card, RoundedCornerShape(18.dp)).border(1.dp, line, RoundedCornerShape(18.dp))
                        .clickable { touchScreen = true }.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.pen_touch_title), color = text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.pen_touch_card_help), color = dim, fontSize = 13.sp)
                    }
                    Text(stringResource(R.string.pen_touch_open), color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                // One card per button.
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (button in 1..2) {
                        Column(
                            Modifier.weight(1f).background(card, RoundedCornerShape(18.dp)).border(1.dp, line, RoundedCornerShape(18.dp)).padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(14.dp).background(if (button == 1) BUTTON1 else BUTTON2, RoundedCornerShape(4.dp)))
                                Spacer(Modifier.size(10.dp))
                                Text(
                                    stringResource(if (button == 1) R.string.pen_button_1 else R.string.pen_button_2),
                                    color = text, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                )
                            }
                            GestureRow(stringResource(R.string.pen_button_click), dim) {
                                OptionDropdown(tapOptions, if (button == 1) prefs.penButton1Click else prefs.penButton2Click) {
                                    update(if (button == 1) prefs.copy(penButton1Click = it) else prefs.copy(penButton2Click = it))
                                }
                            }
                            GestureRow(stringResource(R.string.pen_button_double), dim) {
                                OptionDropdown(tapOptions, if (button == 1) prefs.penButton1Double else prefs.penButton2Double) {
                                    update(if (button == 1) prefs.copy(penButton1Double = it) else prefs.copy(penButton2Double = it))
                                }
                            }
                            val holdOptions = penButtonOptions.map { (id, res) -> id to stringResource(res) }
                            GestureRow(stringResource(R.string.pref_pen_button_hold), dim) {
                                OptionDropdown(holdOptions, if (button == 1) prefs.penButtonTool else prefs.penButton2Tool) {
                                    update(if (button == 1) prefs.copy(penButtonTool = it) else prefs.copy(penButton2Tool = it))
                                }
                            }
                        }
                    }
                }

                // Test strip and settings.
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        Modifier.weight(1f).background(Color(0xFF1F2328), RoundedCornerShape(18.dp)).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(stringResource(R.string.pen_test_title), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.pen_test_help), color = Color(0xFFC9CDD3), fontSize = 13.sp)
                        val heard = editor.penTestHeard
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (button in 1..2) {
                                val got = heard?.first == button
                                val colour = if (button == 1) BUTTON1 else BUTTON2
                                Box(
                                    Modifier
                                        .background(if (got) colour else Color.Transparent, RoundedCornerShape(17.dp))
                                        .border(BorderStroke(1.dp, if (got) colour else Color(0xFF8A8F96)), RoundedCornerShape(17.dp))
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                ) {
                                    Text(
                                        if (got) stringResource(
                                            R.string.pen_test_got, button,
                                            stringResource(if (heard!!.second) R.string.pen_button_double else R.string.pen_button_click),
                                        ) else stringResource(R.string.pen_test_waiting, button),
                                        color = if (got) Color.White else Color(0xFFC9CDD3), fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }
                    Column(
                        Modifier.weight(1f).background(card, RoundedCornerShape(18.dp)).border(1.dp, line, RoundedCornerShape(18.dp)).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val step = prefs.penWidthStep
                        Row {
                            Text(stringResource(R.string.pen_width_step), color = text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(String.format(java.util.Locale.getDefault(), "%.1f", step), color = text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = step.toFloat(),
                            onValueChange = { update(prefs.copy(penWidthStep = ((it * 10).roundToInt() / 10.0).coerceIn(0.1, 3.0))) },
                            valueRange = 0.1f..3f,
                        )
                        CheckLine(stringResource(R.string.pen_button_toast), prefs.penButtonToast, text) { update(prefs.copy(penButtonToast = it)) }
                        CheckLine(stringResource(R.string.pref_pen_button_hover), prefs.penButtonHover, text) { update(prefs.copy(penButtonHover = it)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureRow(label: String, dim: Color, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = dim, fontSize = 14.sp, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
private fun CheckLine(label: String, checked: Boolean, text: Color, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onChange(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, color = text, fontSize = 14.sp)
    }
}

/** The pen, drawn: a cone tip, a long barrel and its two side buttons in their colours. */
@Composable
private fun PenDrawing(modifier: Modifier) {
    Canvas(modifier) {
        val h = size.height
        val cy = h / 2f
        val barrelH = 34.dp.toPx()
        val tipLen = 70.dp.toPx()
        val left = 30.dp.toPx()
        val right = size.width - 30.dp.toPx()
        val tip = Path().apply {
            moveTo(left, cy)
            lineTo(left + tipLen, cy - barrelH * 0.38f)
            lineTo(left + tipLen, cy + barrelH * 0.38f)
            close()
        }
        drawPath(tip, Color(0xFF3D4148))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF5A5F67), Color(0xFF2F3338)), startY = cy - barrelH / 2, endY = cy + barrelH / 2),
            topLeft = Offset(left + tipLen, cy - barrelH / 2),
            size = Size(right - left - tipLen, barrelH),
            cornerRadius = CornerRadius(barrelH / 2),
        )
        val bw = 46.dp.toPx()
        val bh = 10.dp.toPx()
        drawRoundRect(BUTTON1, Offset(left + tipLen + 40.dp.toPx(), cy - barrelH / 2 - bh * 0.6f), Size(bw, bh), CornerRadius(bh / 2))
        drawRoundRect(BUTTON2, Offset(left + tipLen + 100.dp.toPx(), cy - barrelH / 2 - bh * 0.6f), Size(bw, bh), CornerRadius(bh / 2))
    }
}


/**
 * xnotes BR: this screen is a dialog, its own window, so pen keys and button events land there
 * instead of in the activity. Wrap the dialog window's callback to hear them too.
 */
@Composable
private fun CaptureDialogPenEvents() {
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        val original = window?.callback
        if (window != null && original != null) {
            window.callback = object : android.view.Window.Callback by original {
                override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
                    PenSignals.onKey(event)
                    PenSignals.forwardKey?.invoke(event)
                    return original.dispatchKeyEvent(event)
                }

                override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
                    PenSignals.onMotion(event)
                    PenSignals.forwardMotion?.invoke(event)
                    return original.dispatchGenericMotionEvent(event)
                }

                override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
                    PenSignals.onMotion(event)
                    PenSignals.forwardMotion?.invoke(event)
                    return original.dispatchTouchEvent(event)
                }
            }
        }
        onDispose { if (window != null && original != null) window.callback = original }
    }
}
