package com.bastiensabos.betterhusband.feature.ideas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing

/** Écran « stateless » du partage : le texte reçu est modifiable, la catégorie se choisit, un bouton valide. */
@Composable
fun ShareIdeaScreen(
    uiState: ShareIdeaUiState,
    onTextChange: (String) -> Unit,
    onCategoryChange: (IdeaCategory) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = Spacing()
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.screen),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(text = stringResource(R.string.share_idea_title), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = uiState.text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.ideas_input_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            CategoryChips(
                selected = uiState.category,
                onSelected = { it?.let(onCategoryChange) },
                withAll = false,
            )
            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget),
            ) {
                Text(stringResource(R.string.share_idea_save))
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget),
            ) {
                Text(stringResource(R.string.date_field_cancel))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareIdeaScreenPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        ShareIdeaScreen(
            uiState = ShareIdeaUiState(text = "https://exemple.fr/billets-concert", category = IdeaCategory.OUTING),
            onTextChange = {},
            onCategoryChange = {},
            onSave = {},
            onCancel = {},
        )
    }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun ShareIdeaScreenEmptyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        ShareIdeaScreen(ShareIdeaUiState(), onTextChange = {}, onCategoryChange = {}, onSave = {}, onCancel = {})
    }
}
