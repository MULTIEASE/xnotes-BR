package com.xnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.R

/**
 * xnotes BR: Modo Leitura (Kindle style). The toolbar is gone; a tap on the left or right edge
 * turns the page, and a small paper-coloured strip at the top shows where you are and how to leave.
 */
@Composable
fun ReadingOverlay(editor: Editor) {
    val total = editor.state.document.pages.size.coerceAtLeast(1)
    val page = (editor.pageIndex + 1).coerceIn(1, total)
    val percent = page * 100 / total
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(0.16f)
                    .clickable(remember { MutableInteractionSource() }, indication = null) { editor.prevPage() },
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(0.19f)
                    .clickable(remember { MutableInteractionSource() }, indication = null) { editor.nextPage() },
            )
        }
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .background(Color(0xEEF3EFE4), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.reading_position, page, total, percent), color = Color(0xFF3A3732), fontSize = 13.sp)
            Text(
                stringResource(R.string.reading_exit),
                color = Color(0xFF1D4ED8), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { editor.toggleReadingMode() }.padding(4.dp),
            )
        }
        // A thin progress line along the bottom edge, like an e-reader.
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x22000000)))
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(percent / 100f).height(3.dp).background(Color(0xFF3A3732)))
    }
}
