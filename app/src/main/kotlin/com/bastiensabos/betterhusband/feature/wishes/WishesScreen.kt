package com.bastiensabos.betterhusband.feature.wishes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.data.repository.Wish
import com.bastiensabos.betterhusband.ui.component.BackTopBar
import com.bastiensabos.betterhusband.ui.component.Heading
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing

@Composable
fun WishesRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WishesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WishesScreen(
        state = state,
        onBack = onBack,
        onDraftChange = viewModel::onDraftChange,
        onAdd = viewModel::onAdd,
        onDelete = viewModel::onDelete,
        onTakeCare = viewModel::onTakeCare,
        modifier = modifier,
    )
}

/** « Mes envies » (ton ou ta partenaire les voit) puis « Ses envies » (à toi seul de décider quoi en faire). */
@Composable
fun WishesScreen(
    state: WishesUiState,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onTakeCare: (Wish) -> Unit = {},
) {
    val spacing = Spacing()
    Scaffold(modifier = modifier, topBar = { BackTopBar(title = stringResource(R.string.wishes_title), onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = spacing.screen, end = spacing.screen, top = spacing.md, bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item { Heading(text = stringResource(R.string.wishes_mine_title), style = MaterialTheme.typography.titleLarge) }
            item { Text(stringResource(R.string.wishes_mine_body)) }
            item {
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    label = { Text(stringResource(R.string.wishes_input_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAdd() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(onClick = onAdd, enabled = state.canAdd, modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget)) {
                    Text(stringResource(R.string.wishes_add))
                }
            }
            items(state.mine, key = { it.id }) { wish -> MyWishRow(wish, onDelete) }

            item { Heading(text = stringResource(R.string.wishes_theirs_title), style = MaterialTheme.typography.titleLarge) }
            if (state.theirs.isEmpty()) {
                item { Text(stringResource(R.string.wishes_theirs_empty)) }
            }
            items(state.theirs, key = { -it.id }) { wish -> TheirWishRow(wish, taken = wish.uid in state.takenCare, onTakeCare) }
        }
    }
}

@Composable
private fun MyWishRow(
    wish: Wish,
    onDelete: (Long) -> Unit,
) {
    val description = stringResource(R.string.wishes_delete_description, wish.text)
    ListItem(
        headlineContent = { Text(wish.text) },
        trailingContent = {
            IconButton(onClick = { onDelete(wish.id) }, modifier = Modifier.heightIn(min = Spacing().touchTarget)) {
                Icon(Icons.Outlined.Delete, contentDescription = description)
            }
        },
    )
}

@Composable
private fun TheirWishRow(
    wish: Wish,
    taken: Boolean,
    onTakeCare: (Wish) -> Unit,
) {
    ListItem(
        headlineContent = { Text(wish.text) },
        supportingContent = if (taken) ({ Text(stringResource(R.string.wishes_taken_care)) }) else null,
        trailingContent =
            if (taken) {
                null
            } else {
                {
                    TextButton(onClick = { onTakeCare(wish) }, modifier = Modifier.heightIn(min = Spacing().touchTarget)) {
                        Text(stringResource(R.string.wishes_take_care))
                    }
                }
            },
    )
}

private val previewState =
    WishesUiState(
        mine = listOf(Wish(1, "Le dernier album de mon groupe préféré", 0)),
        theirs = listOf(Wish(2, "Une soirée sans écran", 0, "u2"), Wish(3, "Un week-end à la mer", 0, "u3")),
        takenCare = setOf("u3"),
    )

@Preview(showBackground = true)
@Composable
private fun WishesPreview() {
    BetterHusbandTheme(dynamicColor = false) { WishesScreen(previewState, {}, {}, {}, {}) }
}

@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun WishesLargeFontEmptyPreview() {
    BetterHusbandTheme(dynamicColor = false) { WishesScreen(WishesUiState(), {}, {}, {}, {}) }
}
