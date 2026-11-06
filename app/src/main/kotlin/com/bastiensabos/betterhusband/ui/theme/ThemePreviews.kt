package com.bastiensabos.betterhusband.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Planche de contrôle du design system, à comparer avec les maquettes. Textes fictifs, previews uniquement.

@Preview(name = "Clair", widthDp = 390, heightDp = 640)
@Preview(name = "Sombre", widthDp = 390, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Police agrandie", widthDp = 390, heightDp = 640, fontScale = 1.5f)
@Composable
private fun DesignSystemPreview() {
    BetterHusbandTheme {
        Surface {
            val spacing = AppTheme.spacing
            Column(
                modifier = Modifier.padding(spacing.screen),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Text(text = "Bonjour Alex", style = MaterialTheme.typography.headlineLarge)
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        Text(text = "L'attention du jour", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "Glisse un mot doux dans la poche de son manteau.",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            Button(
                                onClick = {},
                                modifier = Modifier.heightIn(min = spacing.touchTarget),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimary,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    ),
                            ) { Text("C'est fait") }
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.heightIn(min = spacing.touchTarget),
                            ) { Text("Autre idée", color = MaterialTheme.colorScheme.onPrimary) }
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(modifier = Modifier.padding(spacing.md)) {
                        Text(text = "Anniversaire de Camille", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Samedi 21 novembre",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
