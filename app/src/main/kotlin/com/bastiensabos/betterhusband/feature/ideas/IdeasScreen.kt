package com.bastiensabos.betterhusband.feature.ideas

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.model.Idea
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import com.bastiensabos.betterhusband.ui.component.Heading
import com.bastiensabos.betterhusband.ui.component.ToneTag
import com.bastiensabos.betterhusband.ui.component.UndoSnackbarEffect
import com.bastiensabos.betterhusband.ui.theme.AppTheme
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing
import com.bastiensabos.betterhusband.ui.theme.ToneColors
import java.time.Instant

data class IdeasActions(
    val onDraftChange: (String) -> Unit,
    val onDraftCategoryChange: (IdeaCategory) -> Unit,
    val onAdd: () -> Unit,
    val onFilterChange: (IdeaCategory?) -> Unit,
    val onToggleDone: (Long, Boolean) -> Unit,
    val onEdit: (Idea) -> Unit,
    val onEditorTextChange: (String) -> Unit,
    val onEditorCategoryChange: (IdeaCategory) -> Unit,
    val onSaveEditor: () -> Unit,
    val onDeleteEditor: () -> Unit,
    val onDismissEditor: () -> Unit,
    val onUndo: () -> Unit = {},
    val onUndoDismissed: () -> Unit = {},
    val onOpenWishes: () -> Unit = {},
)

@Composable
fun IdeasRoute(
    onOpenWishes: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IdeasViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    IdeasScreen(
        uiState = uiState,
        actions =
            IdeasActions(
                onDraftChange = viewModel::onDraftChange,
                onDraftCategoryChange = viewModel::onDraftCategoryChange,
                onAdd = viewModel::onAdd,
                onFilterChange = viewModel::onFilterChange,
                onToggleDone = viewModel::onToggleDone,
                onEdit = viewModel::onEdit,
                onEditorTextChange = viewModel::onEditorTextChange,
                onEditorCategoryChange = viewModel::onEditorCategoryChange,
                onSaveEditor = viewModel::onSaveEditor,
                onDeleteEditor = viewModel::onDeleteEditor,
                onDismissEditor = viewModel::onDismissEditor,
                onUndo = viewModel::onUndo,
                onUndoDismissed = viewModel::onUndoDismissed,
                onOpenWishes = onOpenWishes,
            ),
        modifier = modifier,
    )
}

@Composable
fun IdeasScreen(
    uiState: IdeasUiState,
    actions: IdeasActions,
    modifier: Modifier = Modifier,
) {
    val spacing = Spacing()
    val snackbar = remember { SnackbarHostState() }
    UndoSnackbarEffect(uiState.undo, snackbar, onUndo = actions.onUndo, onDismiss = actions.onUndoDismissed)
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        // Une seule liste qui défile : en police agrandie, la saisie ne mange plus l'écran.
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = spacing.screen, end = spacing.screen, top = spacing.md, bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item { Heading(text = stringResource(R.string.ideas_title), style = MaterialTheme.typography.headlineMedium) }
            if (uiState.wishesAvailable) {
                item {
                    OutlinedButton(onClick = actions.onOpenWishes, modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget)) {
                        Text(stringResource(R.string.wishes_open))
                    }
                }
            }
            item {
                // Capture rapide : une idée = un champ, une catégorie, un bouton.
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    OutlinedTextField(
                        value = uiState.draft,
                        onValueChange = actions.onDraftChange,
                        label = { Text(stringResource(R.string.ideas_input_label)) },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { actions.onAdd() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CategoryChips(selected = uiState.draftCategory, onSelect = { it?.let(actions.onDraftCategoryChange) }, withAll = false)
                    Button(
                        onClick = actions.onAdd,
                        enabled = uiState.canAdd,
                        modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget),
                    ) {
                        Text(stringResource(R.string.ideas_add))
                    }
                }
            }

            when (val list = uiState.list) {
                IdeasListState.Loading -> {
                    item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                }

                IdeasListState.Empty -> {
                    item { Text(text = stringResource(R.string.ideas_empty), style = MaterialTheme.typography.bodyLarge) }
                }

                is IdeasListState.Ready -> {
                    item { CategoryChips(selected = uiState.filter, onSelect = actions.onFilterChange, withAll = true) }
                    if (list.items.isEmpty()) {
                        item { Text(text = stringResource(R.string.ideas_empty_filtered), style = MaterialTheme.typography.bodyLarge) }
                    }
                    items(list.items, key = { it.id }) { idea -> IdeaRow(idea, actions) }
                }
            }
        }
    }
    uiState.editor?.let { IdeaEditorDialog(it, actions) }
}

/** Une rangée de pastilles de catégorie ; [withAll] ajoute « Toutes » (filtre) et accepte `null`. */
@Composable
internal fun CategoryChips(
    selected: IdeaCategory?,
    onSelect: (IdeaCategory?) -> Unit,
    withAll: Boolean,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing().xs),
    ) {
        if (withAll) {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.ideas_filter_all)) },
                modifier = Modifier.heightIn(min = Spacing().touchTarget),
            )
        }
        IdeaCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(stringResource(category.labelRes())) },
                modifier = Modifier.heightIn(min = Spacing().touchTarget),
            )
        }
    }
}

@Composable
private fun IdeaRow(
    idea: Idea,
    actions: IdeasActions,
) {
    // La case dit de quelle idée il s'agit : TalkBack ne lit plus « fait » sans contexte.
    val doneDescription = stringResource(R.string.ideas_done_description, idea.text)
    ListItem(
        modifier = Modifier.heightIn(min = Spacing().touchTarget).clickable { actions.onEdit(idea) },
        leadingContent = {
            Checkbox(
                checked = idea.done,
                onCheckedChange = { actions.onToggleDone(idea.id, it) },
                modifier = Modifier.semantics { contentDescription = doneDescription },
            )
        },
        headlineContent = {
            Text(text = idea.text, textDecoration = if (idea.done) TextDecoration.LineThrough else null)
        },
        supportingContent = { ToneTag(text = stringResource(idea.category.labelRes()), tone = idea.category.tone()) },
    )
}

@Composable
private fun IdeaEditorDialog(
    state: IdeaEditorState,
    actions: IdeasActions,
) {
    AlertDialog(
        onDismissRequest = actions.onDismissEditor,
        title = { Text(stringResource(R.string.ideas_editor_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing().sm)) {
                OutlinedTextField(
                    value = state.text,
                    onValueChange = actions.onEditorTextChange,
                    label = { Text(stringResource(R.string.ideas_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                CategoryChips(
                    selected = state.category,
                    onSelect = { it?.let(actions.onEditorCategoryChange) },
                    withAll = false,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onSaveEditor, enabled = state.canSave) {
                Text(stringResource(R.string.partner_profile_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = actions.onDeleteEditor) { Text(stringResource(R.string.dates_editor_delete)) }
                TextButton(onClick = actions.onDismissEditor) { Text(stringResource(R.string.date_field_cancel)) }
            }
        },
    )
}

@StringRes
private fun IdeaCategory.labelRes(): Int =
    when (this) {
        IdeaCategory.GIFT -> R.string.ideas_category_gift
        IdeaCategory.OUTING -> R.string.ideas_category_outing
        IdeaCategory.ATTENTION -> R.string.ideas_category_attention
    }

@Composable
private fun IdeaCategory.tone(): ToneColors =
    when (this) {
        IdeaCategory.GIFT -> AppTheme.categoryColors.gift
        IdeaCategory.OUTING -> AppTheme.categoryColors.outing
        IdeaCategory.ATTENTION -> AppTheme.categoryColors.care
    }

private val previewActions = IdeasActions({}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

private val previewIdeas =
    listOf(
        Idea(1, "Pivoines pour son anniversaire", IdeaCategory.GIFT, Instant.EPOCH),
        Idea(2, "Restaurant italien du marché", IdeaCategory.OUTING, Instant.EPOCH),
        Idea(3, "Café au lit dimanche", IdeaCategory.ATTENTION, Instant.EPOCH, done = true),
    )

@Preview(showBackground = true)
@Composable
private fun IdeasScreenReadyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        IdeasScreen(IdeasUiState(list = IdeasListState.Ready(previewIdeas), draft = "Billets de concert"), previewActions)
    }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun IdeasScreenEmptyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        IdeasScreen(IdeasUiState(list = IdeasListState.Empty), previewActions)
    }
}

@Preview(showBackground = true)
@Composable
private fun IdeasEditorPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        IdeasScreen(
            IdeasUiState(
                list = IdeasListState.Ready(previewIdeas),
                editor = IdeaEditorState(1, "Pivoines pour son anniversaire", IdeaCategory.GIFT),
            ),
            previewActions,
        )
    }
}
