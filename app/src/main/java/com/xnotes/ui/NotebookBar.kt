package com.xnotes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.R
import com.xnotes.core.tools.Tool
import com.xnotes.ui.theme.toComposeColor

/**
 * xnotes BR: the "Modo Caderno" bar. A row of square buttons drawn like the printed boxes at the
 * foot of a smart paper notebook, for the tools used most: three pen widths, pencil, highlighter,
 * eraser, lasso, four colours, record, undo and a new page.
 */
@Composable
fun NotebookBar(editor: Editor, onRecord: () -> Unit, modifier: Modifier = Modifier) {
    val paper = Color(0xFFF4F1E8)
    val ink = Color(0xFF262626)
    val line = Color(0xFF8F887A)
    val armed = editor.tool
    val width = if (armed.isStroke) editor.toolConfig(armed).baseWidth else -1.0
    Column(
        modifier
            .fillMaxWidth()
            .background(paper)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(line))
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PenBox(stringResource(R.string.nb_fine), 1.5f, armed == Tool.PEN && width in 0.0..1.75, ink, line) { editor.notebookPen(1.0) }
            PenBox(stringResource(R.string.nb_medium), 3f, armed == Tool.PEN && width in 1.76..3.75, ink, line) { editor.notebookPen(2.5) }
            PenBox(stringResource(R.string.nb_thick), 6f, armed == Tool.PEN && width > 3.75, ink, line) { editor.notebookPen(5.0) }
            LabelBox(stringResource(R.string.nb_pencil), armed == Tool.TAPER, ink, line) { editor.notebookTool(Tool.TAPER) }
            LabelBox(stringResource(R.string.tool_highlighter), armed == Tool.HIGHLIGHTER, ink, line) { editor.notebookTool(Tool.HIGHLIGHTER) }
            LabelBox(stringResource(R.string.tool_eraser), armed == Tool.ERASER, ink, line) { editor.notebookTool(Tool.ERASER) }
            LabelBox(stringResource(R.string.tool_lasso), armed == Tool.LASSO, ink, line) { editor.notebookTool(Tool.LASSO) }
            Box(Modifier.width(1.dp).height(40.dp).background(line))
            val colours = editor.toolbarColors.take(5)
            colours.forEachIndexed { i, c ->
                ColourBox(c.toComposeColor(), editor.activeColorIndex == i, ink, line) { editor.pickColor(i) }
            }
            Box(Modifier.width(1.dp).height(40.dp).background(line))
            LabelBox(stringResource(R.string.nb_record), false, ink, line, dot = Color(0xFFB42318)) { onRecord() }
            LabelBox(
                stringResource(if (editor.dictating) R.string.nb_dictate_stop else R.string.nb_dictate),
                editor.dictating, ink, line, dot = if (editor.dictating) Color(0xFFB42318) else null,
            ) { editor.toggleDictation() }
            LabelBox(stringResource(R.string.undo), false, ink, line) { editor.undo() }
            LabelBox(stringResource(R.string.nb_new_page), false, ink, line) { editor.addPage() }
        }
    }
}

@Composable
private fun boxModifier(selected: Boolean, ink: Color, line: Color, onClick: () -> Unit): Modifier =
    Modifier
        .size(width = 66.dp, height = 44.dp)
        .background(if (selected) Color(0xFFE2DCCD) else Color.Transparent, RoundedCornerShape(2.dp))
        .border(BorderStroke(if (selected) 1.8.dp else 1.dp, if (selected) ink else line), RoundedCornerShape(2.dp))
        .clickable(onClick = onClick)

@Composable
private fun PenBox(label: String, thickness: Float, selected: Boolean, ink: Color, line: Color, onClick: () -> Unit) {
    Column(
        boxModifier(selected, ink, line, onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.width(30.dp).height(thickness.dp).background(ink, RoundedCornerShape(2.dp)))
        Spacer(Modifier.height(4.dp))
        Text(label, color = ink, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun LabelBox(label: String, selected: Boolean, ink: Color, line: Color, dot: Color? = null, onClick: () -> Unit) {
    Row(
        boxModifier(selected, ink, line, onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(9.dp).background(dot, CircleShape))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            label, color = ink, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ColourBox(colour: Color, selected: Boolean, ink: Color, line: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .border(BorderStroke(if (selected) 1.8.dp else 1.dp, if (selected) ink else line), RoundedCornerShape(2.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(if (selected) 26.dp else 22.dp).background(colour, RoundedCornerShape(3.dp)))
    }
}
