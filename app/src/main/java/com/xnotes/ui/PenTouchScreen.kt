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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.xnotes.settings.Preferences
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlin.math.roundToInt

/** One test stroke: the points that would have been ink, and the ones the gate held back. */
private class TestStroke {
    val ink = mutableStateListOf<Offset>()
    val held = mutableStateListOf<Offset>()
}

/**
 * xnotes BR: "Ajuste fino da escrita". Two sliders, the pressure a stroke needs to start and how
 * long after contact it may start, and a test sheet that applies them live: the part of each
 * stroke the gate held back shows as a dotted grey trail, the ink in blue, and a meter shows the
 * pen's pressure right now. What is set here applies to the notes as soon as it changes.
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

    val strokes = remember { mutableStateListOf<TestStroke>() }
    var pressure by remember { mutableFloatStateOf(0f) }
    var tool by remember { mutableStateOf("") }
    var heldCount by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(palette.bg.toComposeColor())) {
            Column(
                Modifier.widthIn(max = 1000.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.pen_touch_title), color = text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.pen_touch_subtitle), color = dim, fontSize = 14.sp)
                    }
                    Text(
                        stringResource(R.string.done), color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onClose).padding(12.dp),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Pressure to start.
                    Column(
                        Modifier.weight(1f).background(card, RoundedCornerShape(18.dp)).border(1.dp, line, RoundedCornerShape(18.dp)).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row {
                            Text(stringResource(R.string.pen_min_pressure), color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${(prefs.penMinPressure * 100).roundToInt()}%", color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(stringResource(R.string.pen_min_pressure_help), color = dim, fontSize = 12.sp)
                        Slider(
                            value = prefs.penMinPressure.toFloat(),
                            onValueChange = { update(prefs.copy(penMinPressure = ((it * 100).roundToInt() / 100.0).coerceIn(0.0, 0.6))) },
                            valueRange = 0f..0.6f,
                        )
                    }
                    // Delay to start.
                    Column(
                        Modifier.weight(1f).background(card, RoundedCornerShape(18.dp)).border(1.dp, line, RoundedCornerShape(18.dp)).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
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

                // Live pressure meter.
                Row(
                    Modifier.fillMaxWidth().background(card, RoundedCornerShape(14.dp)).border(1.dp, line, RoundedCornerShape(14.dp)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.pen_pressure_now), color = text, fontSize = 14.sp)
                    Box(Modifier.weight(1f).height(14.dp).background(line, RoundedCornerShape(7.dp))) {
                        Box(Modifier.fillMaxWidth(pressure.coerceIn(0f, 1f)).height(14.dp).background(accent, RoundedCornerShape(7.dp)))
                        // Where the threshold sits.
                        Box(
                            Modifier.fillMaxWidth(prefs.penMinPressure.toFloat().coerceIn(0.001f, 1f)).height(14.dp),
                        ) { Box(Modifier.width(3.dp).height(14.dp).background(Color(0xFFD93025)).align(androidx.compose.ui.Alignment.CenterEnd)) }
                    }
                    Text("${(pressure * 100).roundToInt()}% · $tool", color = text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // The test sheet.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text(stringResource(R.string.pen_test_sheet), color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            stringResource(R.string.clear), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { strokes.clear(); heldCount = 0 }.padding(6.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Legend(Color(0xFF1D4ED8), stringResource(R.string.pen_test_ink), text)
                        Legend(Color(0xFF9AA0A6), stringResource(R.string.pen_test_held), text)
                    }
                    var gateAt by remember { mutableStateOf(-1L) }
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .background(Color(0xFFFFFDF8), RoundedCornerShape(14.dp))
                            .border(1.dp, line, RoundedCornerShape(14.dp))
                            .pointerInteropFilter { e ->
                                PenSignals.onMotion(e)
                                pressure = e.getPressure(0)
                                tool = when (e.getToolType(0)) {
                                    MotionEvent.TOOL_TYPE_STYLUS -> "caneta"
                                    MotionEvent.TOOL_TYPE_FINGER -> "dedo"
                                    MotionEvent.TOOL_TYPE_ERASER -> "borracha"
                                    else -> "outro"
                                }
                                val p = Offset(e.x, e.y)
                                when (e.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> {
                                        strokes.add(TestStroke())
                                        gateAt = e.eventTime
                                    }
                                    MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> Unit
                                    else -> return@pointerInteropFilter true
                                }
                                val s = strokes.lastOrNull() ?: return@pointerInteropFilter true
                                // Every historical sample, so the trail is as fine as the pen reports it.
                                for (h in 0 until e.historySize) {
                                    val hp = Offset(e.getHistoricalX(0, h), e.getHistoricalY(0, h))
                                    val ok = s.ink.isNotEmpty() ||
                                        (e.getHistoricalPressure(0, h) >= prefs.penMinPressure && e.getHistoricalEventTime(h) - gateAt >= prefs.penStartDelayMs)
                                    if (ok) s.ink.add(hp) else s.held.add(hp)
                                }
                                val ok = s.ink.isNotEmpty() || (e.getPressure(0) >= prefs.penMinPressure && e.eventTime - gateAt >= prefs.penStartDelayMs)
                                if (ok) s.ink.add(p) else { s.held.add(p); if (e.actionMasked == MotionEvent.ACTION_DOWN) heldCount++ }
                                if (e.actionMasked == MotionEvent.ACTION_UP) pressure = 0f
                                true
                            },
                    ) {
                        // Ruled lines.
                        var y = 40f
                        while (y < size.height) {
                            drawLine(Color(0xFFD5E2EF), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
                            y += 56f
                        }
                        for (s in strokes) {
                            for (i in 1 until s.held.size) {
                                if (i % 2 == 0) drawLine(Color(0xFF9AA0A6), s.held[i - 1], s.held[i], strokeWidth = 3f, cap = StrokeCap.Round)
                            }
                            s.held.firstOrNull()?.let { drawCircle(Color(0xFF9AA0A6), 5f, it) }
                            for (i in 1 until s.ink.size) {
                                drawLine(Color(0xFF1D4ED8), s.ink[i - 1], s.ink[i], strokeWidth = 4f, cap = StrokeCap.Round)
                            }
                        }
                    }
                    Text(stringResource(R.string.pen_test_sheet_help), color = dim, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun Legend(colour: Color, label: String, text: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).background(colour, CircleShape))
        Text(label, color = text, fontSize = 12.sp)
    }
}
