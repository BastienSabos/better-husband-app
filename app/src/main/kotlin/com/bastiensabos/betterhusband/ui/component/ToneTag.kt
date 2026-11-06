package com.bastiensabos.betterhusband.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bastiensabos.betterhusband.ui.theme.AppTheme
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.ToneColors

/** Pastille de catégorie (« Cadeau », « Sortie »…). Le libellé porte le sens, la couleur ne fait qu'aider. */
@Composable
fun ToneTag(
    text: String,
    tone: ToneColors,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = tone.container,
        contentColor = tone.content,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Preview(name = "Clair")
@Preview(name = "Sombre", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToneTagPreview() {
    BetterHusbandTheme {
        Surface {
            val colors = AppTheme.categoryColors
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
                ToneTag(text = "Attention", tone = colors.care)
                ToneTag(text = "Cadeau", tone = colors.gift)
                ToneTag(text = "Sortie", tone = colors.outing)
                ToneTag(text = "Fait", tone = colors.done)
            }
        }
    }
}
