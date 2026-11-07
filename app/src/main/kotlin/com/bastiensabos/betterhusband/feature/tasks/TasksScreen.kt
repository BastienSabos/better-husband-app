package com.bastiensabos.betterhusband.feature.tasks

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.bastiensabos.betterhusband.ui.component.Heading
import com.bastiensabos.betterhusband.ui.component.UndoSnackbarEffect
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing
import java.time.LocalDate

data class TasksActions(
    val onDraftChange: (String) -> Unit,
    val onDraftRepeatChange: (TaskRepeat) -> Unit,
    val onAdd: () -> Unit,
    val onToggleDone: (Long, Boolean) -> Unit,
    val onEdit: (HouseholdTask) -> Unit,
    val onEditorTitleChange: (String) -> Unit,
    val onEditorRepeatChange: (TaskRepeat) -> Unit,
    val onSaveEditor: () -> Unit,
    val onDeleteEditor: () -> Unit,
    val onDismissEditor: () -> Unit,
    val onUndo: () -> Unit = {},
    val onUndoDismissed: () -> Unit = {},
)

@Composable
fun TasksRoute(
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TasksScreen(
        uiState = uiState,
        actions =
            TasksActions(
                onDraftChange = viewModel::onDraftChange,
                onDraftRepeatChange = viewModel::onDraftRepeatChange,
                onAdd = viewModel::onAdd,
                onToggleDone = viewModel::onToggleDone,
                onEdit = viewModel::onEdit,
                onEditorTitleChange = viewModel::onEditorTitleChange,
                onEditorRepeatChange = viewModel::onEditorRepeatChange,
                onSaveEditor = viewModel::onSaveEditor,
                onDeleteEditor = viewModel::onDeleteEditor,
                onDismissEditor = viewModel::onDismissEditor,
                onUndo = viewModel::onUndo,
                onUndoDismissed = viewModel::onUndoDismissed,
            ),
        modifier = modifier,
    )
}

@Composable
fun TasksScreen(
    uiState: TasksUiState,
    actions: TasksActions,
    modifier: Modifier = Modifier,
) {
    val spacing = Spacing()
    val snackbar = remember { SnackbarHostState() }
    UndoSnackbarEffect(uiState.undo, snackbar, onUndo = actions.onUndo, onDismiss = actions.onUndoDismissed)
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        // Une seule liste qui défile : en police agrandie, l'ajout et les tâches restent tous accessibles.
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = spacing.screen, end = spacing.screen, top = spacing.md, bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item { Heading(text = stringResource(R.string.tasks_title), style = MaterialTheme.typography.headlineMedium) }
            item {
                // Ajout rapide : un titre, un rythme, un bouton.
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    OutlinedTextField(
                        value = uiState.draft,
                        onValueChange = actions.onDraftChange,
                        label = { Text(stringResource(R.string.tasks_input_label)) },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { actions.onAdd() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    RepeatChips(selected = uiState.draftRepeat, onSelect = actions.onDraftRepeatChange)
                    Button(
                        onClick = actions.onAdd,
                        enabled = uiState.canAdd,
                        modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget),
                    ) {
                        Text(stringResource(R.string.tasks_add))
                    }
                }
            }

            when (val list = uiState.list) {
                TasksListState.Loading -> {
                    item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                }

                TasksListState.Empty -> {
                    item { Text(text = stringResource(R.string.tasks_empty), style = MaterialTheme.typography.bodyLarge) }
                }

                is TasksListState.Ready -> {
                    items(list.items, key = { it.task.id }) { item -> TaskRow(item, actions) }
                }
            }
        }
    }
    uiState.editor?.let { TaskEditorDialog(it, actions) }
}

@Composable
private fun RepeatChips(
    selected: TaskRepeat,
    onSelect: (TaskRepeat) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing().xs),
    ) {
        TaskRepeat.entries.forEach { repeat ->
            FilterChip(
                selected = selected == repeat,
                onClick = { onSelect(repeat) },
                label = { Text(stringResource(repeat.labelRes())) },
                modifier = Modifier.heightIn(min = Spacing().touchTarget),
            )
        }
    }
}

@Composable
private fun TaskRow(
    item: TaskItem,
    actions: TasksActions,
) {
    val editDescription = stringResource(R.string.tasks_edit_description, item.task.title)
    // Toute la ligne coche la tâche (TalkBack lit son nom et l'état de la case) ; le crayon ouvre la modification.
    ListItem(
        modifier =
            Modifier
                .heightIn(min = Spacing().touchTarget)
                .toggleable(value = item.done, role = Role.Checkbox, onValueChange = { actions.onToggleDone(item.task.id, it) }),
        leadingContent = { Checkbox(checked = item.done, onCheckedChange = null) },
        headlineContent = {
            Text(text = item.task.title, textDecoration = if (item.done) TextDecoration.LineThrough else null)
        },
        supportingContent = { Text(text = dueText(item)) },
        trailingContent = {
            IconButton(onClick = { actions.onEdit(item.task) }) {
                Icon(Icons.Outlined.Edit, contentDescription = editDescription)
            }
        },
    )
}

@Composable
private fun dueText(item: TaskItem): String {
    val days = item.daysUntilDue
    val repeat = stringResource(item.task.repeat.labelRes())
    val status =
        when {
            days < 0 -> pluralStringResource(R.plurals.tasks_overdue_days, (-days).toInt(), (-days).toInt())
            days == 0L -> stringResource(R.string.tasks_due_today)
            else -> pluralStringResource(R.plurals.tasks_next_in_days, days.toInt(), days.toInt())
        }
    return stringResource(R.string.tasks_row_support, status, repeat)
}

@Composable
private fun TaskEditorDialog(
    state: TaskEditorState,
    actions: TasksActions,
) {
    AlertDialog(
        onDismissRequest = actions.onDismissEditor,
        title = { Text(stringResource(R.string.tasks_editor_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing().sm)) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = actions.onEditorTitleChange,
                    label = { Text(stringResource(R.string.tasks_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                RepeatChips(selected = state.repeat, onSelect = actions.onEditorRepeatChange)
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
private fun TaskRepeat.labelRes(): Int =
    when (this) {
        TaskRepeat.DAILY -> R.string.tasks_repeat_daily
        TaskRepeat.WEEKLY -> R.string.tasks_repeat_weekly
        TaskRepeat.BIWEEKLY -> R.string.tasks_repeat_biweekly
        TaskRepeat.MONTHLY -> R.string.tasks_repeat_monthly
    }

private val previewActions = TasksActions({}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

private val previewDay = LocalDate.of(2026, 11, 7)

private val previewItems =
    listOf(
        TaskItem(HouseholdTask(1, "Sortir les poubelles", TaskRepeat.WEEKLY, previewDay), done = false, daysUntilDue = -2),
        TaskItem(HouseholdTask(2, "Faire une lessive", TaskRepeat.WEEKLY, previewDay), done = false, daysUntilDue = 0),
        TaskItem(HouseholdTask(3, "Passer l'aspirateur", TaskRepeat.BIWEEKLY, previewDay), done = true, daysUntilDue = 12),
    )

@Preview(showBackground = true)
@Composable
private fun TasksScreenReadyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        TasksScreen(TasksUiState(list = TasksListState.Ready(previewItems), draft = "Arroser les plantes"), previewActions)
    }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun TasksScreenEmptyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        TasksScreen(TasksUiState(list = TasksListState.Empty), previewActions)
    }
}

@Preview(showBackground = true)
@Composable
private fun TasksEditorPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        val task = previewItems.first().task
        TasksScreen(
            TasksUiState(
                list = TasksListState.Ready(previewItems),
                editor = TaskEditorState(task, task.title, task.repeat),
            ),
            previewActions,
        )
    }
}
