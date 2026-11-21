package com.xnotes.ui

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xnotes.R
import com.xnotes.canvas.PenContactFilter
import com.xnotes.settings.Preferences
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** One test stroke: its ink points. */
private class TestStroke { val ink = mutableStateListOf<Offset>() }

/**
 * xnotes BR: "Ajuste fino da escrita – Vision Pen". Pressure to start, delay on first contact, an
 * automatic calibration (hover, then write) and a test sheet that runs the very filter the notes
 * use: blue is the ink the notes would draw, grey dots are what the filter held back.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PenTouchScreen(prefs: Preferences, update: (Preferences) -> Unit, onClose: () -> Unit) {
    val palette = LocalPalette.current
    val text = palette.text.toComposeColor()
    val dim = palette.textDim.toComposeColor()
    val line = palette.border.toComposeColor()
    val card = palette.panel.toComposeColor()
    val accent = palette.accent.toComposeColor()
    val latest by rememberUpdatedState(prefs)

    val strokes = remember { mutableStateListOf<TestStroke>() }
    val held = remember { mutableStateListOf<Offset>() }
    var pressure by remember { mutableFloatStateOf(0f) }
    var tool by remember { mutableStateOf("") }
    // Calibration: 0 off, 1 hovering, 2 writing.
    var calib by remember { mutableIntStateOf(0) }
    val hoverP = remember { mutableListOf<Float>() }
    val writeP = remember { mutableListOf<Float>() }
    var sawFinger by remember { mutableStateOf(false) }
    var verdict by remember { mutableStateOf<String?>(null) }
    val vNoHover = stringResource(R.string.pen_calib_no_hover)
    val vFlat = stringResource(R.string.pen_calib_flat)
    val vMixed = stringResource(R.string.pen_calib_mixed)
    val vOk = stringResource(R.string.pen_calib_ok)

    val filter = remember {
        PenContactFilter { ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> strokes.add(TestStroke().also { it.ink.add(Offset(ev.x, ev.y)) })
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> strokes.lastOrNull()?.ink?.add(Offset(ev.x, ev.y))
            }
        }.also { f -> f.onHeld = { x, y -> held.add(Offset(x, y)) } }
    }
    filter.minPressure = prefs.penMinPressure.toFloat()
    filter.startDelayMs = prefs.penStartDelayMs.toLong()
    filter.alsoFingers = prefs.penGateFingers

    LaunchedEffect(calib) {
        if (calib == 1) {
            delay(3500)
            if (calib == 1) calib = 2
        }
    }

    fun finishCalibration() {
        calib = 0
        val w = writeP.filter { it > 0f }.sorted()
        val h = hoverP.sorted()
        if (w.size < 10) { verdict = null; return }
        val wLow = w[(w.size * 0.10).toInt()]
        val flat = w.last() - w.first() < 0.02f
        val hHigh = if (h.isEmpty()) 0f else h[((h.size - 1) * 0.95).toInt()]
        var t = maxOf(hHigh * 1.25f + 0.01f, 0.03f)
        verdict = when {
            flat -> { t = 0f; vFlat }
            h.isEmpty() -> { t = (wLow * 0.5f).coerceIn(0.03f, 0.3f); vNoHover }
            t >= wLow -> { t = (hHigh + wLow) / 2f; vMixed }
            else -> vOk
        }
        update(
            latest.copy(
                penMinPressure = ((t * 100).roundToInt() / 100.0).coerceIn(0.0, 0.9),
                penStartDelayMs = if (flat) maxOf(latest.penStartDelayMs, 40) else latest.penStartDelayMs,
                penGateFingers = sawFinger,
            ),
        )
        strokes.clear(); held.clear()
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(palette.bg.toComposeColor()), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 1000.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.pen_touch_title), color = text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.pen_touch_subtitle), color = dim, fontSize = 14.sp)
                    }
                    Text(
                        stringResource(R.string.done), color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onClose).padding(12.dp),
                    )
                }

                // Automatic calibration.
                Column(
                    Modifier.fillMaxWidth().background(accent.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .border(1.5.dp, accent, RoundedCornerShape(18.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.pen_calib_title), color = text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(
                            when (calib) {
                                1 -> R.string.pen_calib_step_hover
                                2 -> R.string.pen_calib_step_write
                                else -> R.string.pen_calib_help
                            },
                        ),
                        color = if (calib != 0) accent else dim, fontSize = 14.sp, fontWeight = if (calib != 0) FontWeight.Bold else FontWeight.Normal,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(if (calib == 2) R.string.pen_calib_finish else if (calib == 1) R.string.cancel else R.string.pen_calib_start),
                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(accent, RoundedCornerShape(12.dp)).clickable {
                                when (calib) {
                                    0 -> { hoverP.clear(); writeP.clear(); sawFinger = false; verdict = null; strokes.clear(); held.clear(); calib = 1 }
                                    1 -> calib = 0
                                    else -> finishCalibration()
                                }
                            }.padding(horizontal = 18.dp, vertical = 10.dp),
                        )
                    }
                    verdict?.let { Text(it, color = text, fontSize = 13.sp) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(
                        Modifier.weight(1f).background(card, RoundedCornerShape(18.dp)).border(1.dp, line, RoundedCornerShape(18.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row {
                            Text(stringResource(R.string.pen_min_pressure), color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${(prefs.penMinPressure * 100).roundToInt()}%", color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(stringResource(R.string.pen_min_pressure_help), color = dim, fontSize = 12.sp)
                        Slider(
                            value = prefs.penMinPressure.toFloat(),
                            onValueChange = { update(prefs.copy(penMinPressure = ((it * 100).roundToInt() / 100.0).coerceIn(0.0, 0.9))) },
                            valueRange = 0f..0.9f,
                        )
                    }
                    Column(
                        Modifier.weight(1f).background(card, RoundedCornerShape(18.dp)).border(1.dp, line, RoundedCornerShape(18.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row {
                            Text(stringResource(R.string.pen_start_delay), color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${prefs.penStartDelayMs} ms", color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(stringResource(R.string.pen_start_delay_help), color = dim, fontSize = 12.sp)
                        Slider(
                            value = prefs.penStartDelayMs.toFloat(),
                            onValueChange = { update(prefs.copy(penStartDelayMs = ((it / 5f).roundToInt() * 5).coerceIn(0, 120))) },
                            valueRange = 0f..120f,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = prefs.penGateFingers, onCheckedChange = { update(prefs.copy(penGateFingers = it)) })
                    Text(stringResource(R.string.pen_gate_fingers), color = text, fontSize = 13.sp)
                }

                // Live pressure meter.
                Row(
                    Modifier.fillMaxWidth().background(card, RoundedCornerShape(14.dp)).border(1.dp, line, RoundedCornerShape(14.dp)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.pen_pressure_now), color = text, fontSize = 14.sp)
                    Box(Modifier.weight(1f).height(14.dp).background(line, RoundedCornerShape(7.dp))) {
                        Box(Modifier.fillMaxWidth(pressure.coerceIn(0f, 1f)).height(14.dp).background(accent, RoundedCornerShape(7.dp)))
                        Box(Modifier.fillMaxWidth(prefs.penMinPressure.toFloat().coerceIn(0.001f, 1f)).height(14.dp)) {
                            Box(Modifier.width(3.dp).height(14.dp).background(Color(0xFFD93025)).align(Alignment.CenterEnd))
                        }
                    }
                    Text("${(pressure * 100).roundToInt()}% · $tool", color = text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // The test sheet, through the same filter the notes use.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pen_test_sheet), color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Legend(Color(0xFF1D4ED8), stringResource(R.string.pen_test_ink), text)
                    Box(Modifier.width(14.dp))
                    Legend(Color(0xFF9AA0A6), stringResource(R.string.pen_test_held), text)
                    Text(
                        stringResource(R.string.clear), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { strokes.clear(); held.clear() }.padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
                    )
                }
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(Color(0xFFFFFDF8), RoundedCornerShape(14.dp))
                        .border(1.dp, line, RoundedCornerShape(14.dp))
                        .pointerInteropFilter { e ->
                            PenSignals.onMotion(e)
                            pressure = if (e.actionMasked == MotionEvent.ACTION_UP) 0f else e.getPressure(0)
                            val type = e.getToolType(0)
                            tool = when (type) {
                                MotionEvent.TOOL_TYPE_STYLUS -> "caneta"
                                MotionEvent.TOOL_TYPE_FINGER -> "dedo"
                                MotionEvent.TOOL_TYPE_ERASER -> "borracha"
                                else -> "outro"
                            }
                            if (calib != 0 && type == MotionEvent.TOOL_TYPE_FINGER) sawFinger = true
                            val bucket = if (calib == 1) hoverP else if (calib == 2) writeP else null
                            if (bucket != null) {
                                for (h in 0 until e.historySize) bucket.add(e.getHistoricalPressure(0, h))
                                bucket.add(e.getPressure(0))
                            }
                            if (calib == 1) return@pointerInteropFilter true // hovering: nothing to draw
                            if (!filter.feed(e) { false }) {
                                // Filter off (or not a filtered pointer): draw every sample as it comes.
                                when (e.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> strokes.add(TestStroke().also { it.ink.add(Offset(e.x, e.y)) })
                                    MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                                        val s = strokes.lastOrNull()
                                        for (h in 0 until e.historySize) s?.ink?.add(Offset(e.getHistoricalX(0, h), e.getHistoricalY(0, h)))
                                        s?.ink?.add(Offset(e.x, e.y))
                                    }
                                }
                            }
                            true
                        },
                ) {
                    var y = 40f
                    while (y < size.height) {
                        drawLine(Color(0xFFD5E2EF), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
                        y += 56f
                    }
                    for (p in held) drawCircle(Color(0xFF9AA0A6), 2.5f, p)
                    for (s in strokes) {
                        if (s.ink.size == 1) drawCircle(Color(0xFF1D4ED8), 3f, s.ink[0])
                        for (i in 1 until s.ink.size) drawLine(Color(0xFF1D4ED8), s.ink[i - 1], s.ink[i], strokeWidth = 4f, cap = StrokeCap.Round)
                    }
                }
                Text(stringResource(R.string.pen_test_sheet_help), color = dim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Legend(colour: Color, label: String, text: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(colour, CircleShape))
        Text(label, color = text, fontSize = 12.sp)
    }
}
