package com.xnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.R
import com.xnotes.core.tools.Tool
import com.xnotes.ui.icons.XnotesIcons
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * xnotes BR: the radial menu. Opened by a pen button (or a gesture), it rings the most used tools
 * round the middle of the page so the pen never has to travel to the toolbar.
 */
@Composable
fun RadialMenu(editor: Editor) {
    if (!editor.radialOpen) return
    val palette = LocalPalette.current
    val panel = palette.panel.toComposeColor()
    val text = palette.text.toComposeColor()
    val accent = palette.accent.toComposeColor()
    val line = palette.border.toComposeColor()
    class Entry(val icon: ImageVector?, val label: String, val selected: Boolean, val colour: Boolean = false, val run: () -> Unit)
    val entries = listOf(
        Entry(null, stringResource(R.string.tool_pen), editor.tool == Tool.PEN) { editor.notebookTool(Tool.PEN) },
        Entry(null, stringResource(R.string.tool_highlighter), editor.tool == Tool.HIGHLIGHTER) { editor.notebookTool(Tool.HIGHLIGHTER) },
        Entry(XnotesIcons.eraser, stringResource(R.string.tool_eraser), editor.tool == Tool.ERASER) { editor.notebookTool(Tool.ERASER) },
        Entry(XnotesIcons.lasso, stringResource(R.string.tool_lasso), editor.tool == Tool.LASSO) { editor.notebookTool(Tool.LASSO) },
        Entry(XnotesIcons.textBox, stringResource(R.string.tool_text_box), editor.tool == Tool.TEXT_BOX) { editor.notebookTool(Tool.TEXT_BOX) },
        Entry(XnotesIcons.shape, stringResource(R.string.tool_shape), editor.tool == Tool.SHAPE) { editor.notebookTool(Tool.SHAPE) },
        Entry(null, stringResource(R.string.action_next_colour), false, colour = true) { editor.dispatchTapGesture("next_colour") },
        Entry(XnotesIcons.undo, stringResource(R.string.undo), false) { editor.undo() },
    )
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.12f))
            .clickable(remember { MutableInteractionSource() }, indication = null) { editor.radialOpen = false },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(270.dp).background(panel.copy(alpha = 0.96f), CircleShape).border(1.dp, line, CircleShape))
        entries.forEachIndexed { i, e ->
            val angle = Math.toRadians(-90.0 + i * 360.0 / entries.size)
            Box(
                Modifier
                    .offset(x = (100 * cos(angle)).dp, y = (100 * sin(angle)).dp)
                    .size(62.dp)
                    .background(if (e.selected) accent else palette.bg.toComposeColor(), CircleShape)
                    .border(1.dp, if (e.selected) accent else line, CircleShape)
                    .clickable { e.run(); editor.radialOpen = false },
                contentAlignment = Alignment.Center,
            ) {
                if (e.icon != null) {
                    Icon(e.icon, contentDescription = e.label, tint = if (e.selected) Color.White else text, modifier = Modifier.size(26.dp))
                } else if (!e.colour) {
                    Text(e.label, color = if (e.selected) Color.White else text, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 11.sp, maxLines = 2)
                } else {
                    Box(Modifier.size(26.dp).background(
                        (editor.toolbarColors.getOrNull(editor.activeColorIndex) ?: com.xnotes.core.model.Rgba(0, 0, 0)).toComposeColor(), CircleShape,
                    ).border(1.dp, line, CircleShape))
                }
            }
        }
        Text(
            stringResource(R.string.radial_close), color = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.size(80.dp).offset(y = 30.dp),
        )
    }
}
