package com.bastiensabos.betterhusband.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle

/** Titre d'écran ou de section : TalkBack permet de sauter de titre en titre. */
@Composable
fun Heading(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Text(text = text, style = style, modifier = modifier.semantics { heading() })
}
