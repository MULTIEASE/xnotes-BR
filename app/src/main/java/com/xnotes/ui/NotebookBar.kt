package com.xnotes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.R
import com.xnotes.core.model.Rgba
import com.xnotes.core.tools.Tool
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

/** xnotes BR: fluorescent highlighter colours offered by a double tap on the highlighter box. */
val NOTEBOOK_FLUO = listOf(
    Rgba(255, 255, 0), Rgba(204, 255, 0), Rgba(57, 255, 20), Rgba(0, 255, 171),
    Rgba(0, 240, 255), Rgba(77, 166, 255), Rgba(191, 95, 255), Rgba(255, 0, 255),
    Rgba(255, 110, 199), Rgba(255, 159, 0), Rgba(255, 49, 49), Rgba(255, 184, 108),
)

/** xnotes BR: pen widths offered by a double tap on the pen box. */
val NOTEBOOK_WIDTHS = listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.5, 5.0, 8.0)

/**
 * xnotes BR: the "Modo Caderno" bar, in the app's own colours. One box per tool: a tap arms it,
 * a double tap opens its choices above (widths for the pen, fluorescent colours for the highlighter,
 * the colour palette for the colour box).
 */
@Composable
fun NotebookBar(editor: Editor, onRecord: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val bg = palette.panel.toComposeColor()
    val ink = palette.text.toComposeColor()
    val line = palette.border.toComposeColor()
    val accent = palette.accent.toComposeColor()
    val armed = editor.tool
    val k = when (editor.notebookBarSize) { "small" -> 0.82f; "large" -> 1.3f; else -> 1f }
    var widthsOpen by remember { mutableStateOf(false) }
    var fluoOpen by remember { mutableStateOf(false) }
    var coloursOpen by remember { mutableStateOf(false) }
    var inkOptsOpen by remember { mutableStateOf(false) }
    @Suppress("UNUSED_VARIABLE") val configVersion = editor.toolConfigVersion // recompose when a pen width changes
    val penWidth = editor.toolConfig(Tool.PEN).baseWidth
    val hlColour = editor.toolConfig(Tool.HIGHLIGHTER).colorOverride ?: NOTEBOOK_FLUO.first()
    val inkColour = editor.toolbarColors.getOrNull(editor.activeColorIndex) ?: Rgba(0, 0, 0)

    androidx.compose.runtime.CompositionLocalProvider(LocalBarScale provides k) {
    Column(modifier.fillMaxWidth().background(bg).padding(horizontal = 10.dp, vertical = (6 * k).dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(line))
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Pen: tap = pen, double tap = widths.
            Box {
                ToolBox(
                    selected = armed == Tool.PEN, line = line, accent = accent,
                    onClick = { editor.notebookTool(Tool.PEN) },
                    onDouble = { editor.notebookTool(Tool.PEN); widthsOpen = true },
                ) {
                    Box(Modifier.width(30.dp).height(widthDp(penWidth)).background(inkColour.toComposeColor(), RoundedCornerShape(2.dp)))
                    Spacer(Modifier.height(4.dp))
                    BoxLabel(stringResource(R.string.tool_pen) + " " + formatWidth(penWidth), ink, armed == Tool.PEN)
                }
                DropdownMenu(expanded = widthsOpen, onDismissRequest = { widthsOpen = false }) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NOTEBOOK_WIDTHS.forEach { w ->
                            Column(
                                Modifier.size(width = 52.dp, height = 52.dp)
                                    .border(BorderStroke(if (w == penWidth) 2.dp else 1.dp, if (w == penWidth) accent else line), RoundedCornerShape(8.dp))
                                    .clickable { editor.notebookPen(w); widthsOpen = false },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Box(Modifier.width(30.dp).height(widthDp(w)).background(ink, RoundedCornerShape(2.dp)))
                                Spacer(Modifier.height(4.dp))
                                Text(formatWidth(w), color = ink, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            // Highlighter: tap = highlighter, double tap = fluorescent colours.
            Box {
                ToolBox(
                    selected = armed == Tool.HIGHLIGHTER, line = line, accent = accent,
                    onClick = { editor.notebookTool(Tool.HIGHLIGHTER) },
                    onDouble = { editor.notebookTool(Tool.HIGHLIGHTER); fluoOpen = true },
                ) {
                    Box(Modifier.width(34.dp).height(10.dp).background(hlColour.toComposeColor(), RoundedCornerShape(3.dp)))
                    Spacer(Modifier.height(4.dp))
                    BoxLabel(stringResource(R.string.tool_highlighter), ink, armed == Tool.HIGHLIGHTER)
                }
                DropdownMenu(expanded = fluoOpen, onDismissRequest = { fluoOpen = false }) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NOTEBOOK_FLUO.forEach { c ->
                            Swatch(c.toComposeColor(), c == hlColour, accent, line) { editor.notebookHighlighter(c); fluoOpen = false }
                        }
                    }
                }
            }
            SimpleBox(stringResource(R.string.nb_pencil), armed == Tool.TAPER, ink, line, accent) { editor.notebookTool(Tool.TAPER) }
            SimpleBox(stringResource(R.string.tool_eraser), armed == Tool.ERASER, ink, line, accent) { editor.notebookTool(Tool.ERASER) }
            SimpleBox(stringResource(R.string.tool_lasso), armed == Tool.LASSO, ink, line, accent) { editor.notebookTool(Tool.LASSO) }
            SimpleBox(stringResource(R.string.tool_shape), armed == Tool.SHAPE, ink, line, accent) { editor.notebookTool(Tool.SHAPE) }
            // Colour: tap or double tap opens the palette.
            Box {
                ToolBox(
                    selected = false, line = line, accent = accent,
                    onClick = { coloursOpen = true },
                    onDouble = { coloursOpen = true },
                ) {
                    Box(Modifier.size(22.dp).background(inkColour.toComposeColor(), CircleShape).border(1.dp, line, CircleShape))
                    Spacer(Modifier.height(4.dp))
                    BoxLabel(stringResource(R.string.caption_colour), ink, false)
                }
                DropdownMenu(expanded = coloursOpen, onDismissRequest = { coloursOpen = false }) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        editor.toolbarColors.forEachIndexed { i, c ->
                            Swatch(c.toComposeColor(), i == editor.activeColorIndex, accent, line) { editor.pickColor(i); coloursOpen = false }
                        }
                    }
                }
            }
            Box(Modifier.width(1.dp).height(40.dp).background(line))
            // Record: one tap starts, the next tap stops and places the recording.
            val recordingNow = AudioRecording.session != null
            SimpleBox(
                stringResource(if (recordingNow) R.string.nb_record_stop else R.string.nb_record), recordingNow, ink, line, accent,
                dot = Color(0xFFD93025),
            ) { if (recordingNow) AudioRecording.stop() else onRecord() }
            // Handwriting to typed text: tap = on/off (or convert the selection), double tap = options.
            Box {
                @Suppress("UNUSED_VARIABLE") val optsVersion = editor.inkOptsVersion
                val selectMode = editor.inkTextMode == "select"
                ToolBox(
                    selected = editor.inkToText, line = line, accent = accent,
                    onClick = { editor.toggleInkToText() },
                    onDouble = { inkOptsOpen = true },
                ) {
                    BoxLabel(stringResource(R.string.nb_ink_to_text), ink, editor.inkToText)
                    BoxLabel(
                        if (selectMode) stringResource(R.string.ink_mode_select_short) else stringResource(R.string.ink_mode_auto_short),
                        accent, false,
                    )
                }
                DropdownMenu(expanded = inkOptsOpen, onDismissRequest = { inkOptsOpen = false }) {
                    InkTextOptions(editor, ink, line, accent)
                }
            }
            // Radial menu of tools.
            SimpleBox(stringResource(R.string.nb_radial), editor.radialOpen, ink, line, accent) { editor.radialOpen = !editor.radialOpen }
            SimpleBox(
                stringResource(if (editor.dictating || editor.dictationArmed) R.string.nb_dictate_stop else R.string.nb_dictate),
                editor.dictating || editor.dictationArmed, ink, line, accent,
                dot = if (editor.dictating) Color(0xFFD93025) else null,
            ) { editor.toggleDictation() }
            SimpleBox(stringResource(R.string.nb_paste_image), false, ink, line, accent) { editor.pasteImage() }
            SimpleBox(stringResource(R.string.undo), false, ink, line, accent) { editor.undo() }
            SimpleBox(stringResource(R.string.nb_new_page), false, ink, line, accent) { editor.addPageAndTurn() }
            // Bar size: small, medium, large, round and round.
            SimpleBox(
                stringResource(when (editor.notebookBarSize) { "small" -> R.string.nb_size_small; "large" -> R.string.nb_size_large; else -> R.string.nb_size_medium }),
                false, ink, line, accent,
            ) { editor.chooseNotebookBarSize(when (editor.notebookBarSize) { "small" -> "medium"; "medium" -> "large"; else -> "small" }) }
        }
    }
    }
}

/** xnotes BR: how big the notebook bar's boxes are (small 0.82, medium 1, large 1.3). */
private val LocalBarScale = androidx.compose.runtime.compositionLocalOf { 1f }

private fun widthDp(w: Double): Dp = (w * 1.2).coerceIn(1.0, 10.0).dp

private fun formatWidth(w: Double): String =
    if (w % 1.0 == 0.0) w.toInt().toString() else String.format(java.util.Locale.getDefault(), "%.1f", w)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolBox(
    selected: Boolean,
    line: Color,
    accent: Color,
    onClick: () -> Unit,
    onDouble: () -> Unit,
    content: @Composable () -> Unit,
) {
    val k = LocalBarScale.current
    Column(
        Modifier
            .size(width = (76 * k).dp, height = (48 * k).dp)
            .background(if (selected) accent.copy(alpha = 0.14f) else Color.Transparent, RoundedCornerShape(10.dp))
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else line), RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onClick, onDoubleClick = onDouble),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

@Composable
private fun BoxLabel(text: String, ink: Color, selected: Boolean) {
    Text(
        text, color = ink, fontSize = (11 * LocalBarScale.current).sp, textAlign = TextAlign.Center, lineHeight = (12 * LocalBarScale.current).sp, maxLines = 1,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
    )
}

@Composable
private fun SimpleBox(label: String, selected: Boolean, ink: Color, line: Color, accent: Color, dot: Color? = null, onClick: () -> Unit) {
    val k = LocalBarScale.current
    Row(
        Modifier
            .size(width = (72 * k).dp, height = (48 * k).dp)
            .background(if (selected) accent.copy(alpha = 0.14f) else Color.Transparent, RoundedCornerShape(10.dp))
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else line), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(9.dp).background(dot, CircleShape))
            Spacer(Modifier.width(4.dp))
        }
        BoxLabel(label, ink, selected)
    }
}

@Composable
private fun Swatch(colour: Color, selected: Boolean, accent: Color, line: Color, onClick: () -> Unit) {
    Box(
        Modifier.size((40 * LocalBarScale.current).dp)
            .border(BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) accent else line), CircleShape)
            .padding(4.dp)
            .background(colour, CircleShape)
            .clickable(onClick = onClick),
    )
}


/** xnotes BR: the options of "Escrita → texto": when to convert, how long to wait, font and size. */
@Composable
private fun InkTextOptions(editor: Editor, ink: Color, line: Color, accent: Color) {
    @Suppress("UNUSED_VARIABLE") val optsVersion = editor.inkOptsVersion
    val mode = editor.inkTextMode
    val prefs = editor.preferences
    Column(Modifier.padding(14.dp).width(420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.ink_opts_title), color = ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.ink_opts_mode), color = ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(R.string.ink_opts_auto), mode == "auto", ink, line, accent, Modifier.weight(1f)) { editor.setInkTextOptions(mode = "auto") }
            Chip(stringResource(R.string.ink_opts_select), mode == "select", ink, line, accent, Modifier.weight(1f)) { editor.setInkTextOptions(mode = "select") }
        }
        if (mode == "auto") {
            Text(stringResource(R.string.ink_opts_delay), color = ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1000, 2000, 3000, 5000, 8000).forEach { ms ->
                    Chip("${ms / 1000} s", prefs.inkTextDelayMs == ms, ink, line, accent, Modifier.weight(1f)) { editor.setInkTextOptions(delayMs = ms) }
                }
            }
        } else {
            Text(stringResource(R.string.ink_opts_select_help), color = ink.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Text(stringResource(R.string.ink_opts_size), color = ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(stringResource(R.string.ink_opts_size_auto), prefs.inkTextSize == 0, ink, line, accent, Modifier.weight(1.6f)) { editor.setInkTextOptions(size = 0) }
            listOf(11, 12, 14, 16, 20, 24).forEach { pt ->
                Chip("$pt", prefs.inkTextSize == pt, ink, line, accent, Modifier.weight(1f)) { editor.setInkTextOptions(size = pt) }
            }
        }
        Text(stringResource(R.string.ink_opts_font) + ": " + editor.inkTextFaceLabel(), color = ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        val current = editor.inkTextFace()
        Column(
            Modifier.height(220.dp).border(1.dp, line, RoundedCornerShape(10.dp))
                .verticalScroll(rememberScrollState()).padding(4.dp),
        ) {
            editor.inkFontChoices().forEach { c ->
                val on = c.face == current
                Text(
                    "Edson da Silva Flores — ${c.label}",
                    color = if (on) accent else ink,
                    fontSize = 16.sp,
                    fontFamily = rememberFontFamily(c.face),
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth()
                        .background(if (on) accent.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { editor.setInkTextOptions(face = c.face.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        Text(stringResource(R.string.ink_opts_hint), color = ink.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
private fun rememberFontFamily(face: com.xnotes.core.pal.FontFace): androidx.compose.ui.text.font.FontFamily =
    remember(face.id) {
        runCatching {
            androidx.compose.ui.text.font.FontFamily(
                com.xnotes.platform.FontCatalog.resolve(face, bold = false, italic = false).typeface,
            )
        }.getOrDefault(androidx.compose.ui.text.font.FontFamily.Default)
    }

@Composable
private fun Chip(label: String, on: Boolean, ink: Color, line: Color, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .background(if (on) accent.copy(alpha = 0.14f) else Color.Transparent, RoundedCornerShape(10.dp))
            .border(BorderStroke(if (on) 2.dp else 1.dp, if (on) accent else line), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = ink, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, lineHeight = 14.sp)
    }
}
