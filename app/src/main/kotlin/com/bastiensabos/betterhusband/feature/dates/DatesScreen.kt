package com.bastiensabos.betterhusband.feature.dates

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.DateLabel
import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import com.bastiensabos.betterhusband.ui.component.DateField
import com.bastiensabos.betterhusband.ui.component.Heading
import com.bastiensabos.betterhusband.ui.component.NotificationPermissionPrompt
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DatesRoute(
    modifier: Modifier = Modifier,
    viewModel: DatesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DatesScreen(
        uiState = uiState,
        onAddClick = viewModel::onAddClick,
        onEdit = viewModel::onEdit,
        editor =
            DateEditorActions(
                onTitleChange = viewModel::onTitleChange,
                onKindChange = viewModel::onKindChange,
                onDateChange = viewModel::onDateChange,
                onRecurringChange = viewModel::onRecurringChange,
                onSave = viewModel::onSave,
                onDelete = viewModel::onDelete,
                onDismiss = viewModel::onDismissEditor,
            ),
        modifier = modifier,
    )
}

data class DateEditorActions(
    val onTitleChange: (String) -> Unit,
    val onKindChange: (DateKind) -> Unit,
    val onDateChange: (LocalDate?) -> Unit,
    val onRecurringChange: (Boolean) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
fun DatesScreen(
    uiState: DatesUiState,
    onAddClick: () -> Unit,
    onEdit: (Long) -> Unit,
    editor: DateEditorActions,
    modifier: Modifier = Modifier,
) {
    val spacing = Spacing()
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.dates_add)) },
            )
        },
    ) { padding ->
        // Une seule liste qui défile, sous le bouton d'ajout flottant : en police agrandie, rien n'est coupé.
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = spacing.screen, end = spacing.screen, top = spacing.md, bottom = FAB_CLEARANCE),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item { Heading(text = stringResource(R.string.dates_title), style = MaterialTheme.typography.headlineMedium) }
            item { NotificationPermissionPrompt() }
            when (val list = uiState.list) {
                DatesListState.Loading -> {
                    item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                }

                DatesListState.Empty -> {
                    item { Text(text = stringResource(R.string.dates_empty), style = MaterialTheme.typography.bodyLarge) }
                }

                is DatesListState.Ready -> {
                    items(list.items, key = { it.key }) { item -> UpcomingDateRow(item, onEdit) }
                }
            }
        }
    }
    uiState.editor?.let { DateEditorDialog(it, editor) }
}

/** Place laissée sous la liste pour que la dernière date ne reste pas cachée par le bouton flottant. */
private val FAB_CLEARANCE = 96.dp

@Composable
private fun UpcomingDateRow(
    item: UpcomingDate,
    onEdit: (Long) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateText = remember(item.date, locale) { item.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)) }
    val years = item.yearsSince?.takeIf { it > 0 }
    // Seules les dates saisies à la main se modifient ici ; les autres se changent dans la fiche.
    val rowModifier = item.editableId?.let { id -> Modifier.clickable { onEdit(id) } } ?: Modifier
    // L'échéance passe sous le nom plutôt qu'à droite : en police agrandie, le nom garde toute la largeur.
    ListItem(
        modifier = rowModifier.heightIn(min = Spacing().touchTarget),
        overlineContent = { Text(daysUntilText(item.daysUntil)) },
        headlineContent = { Text(item.label.text()) },
        supportingContent = {
            val yearsText = years?.let { pluralStringResource(R.plurals.dates_years, it, it) }
            Text(listOfNotNull(yearsText, dateText).joinToString(" · "))
        },
    )
}

@Composable
internal fun DateLabel.text(): String =
    when (this) {
        is DateLabel.PartnerBirthday -> stringResource(R.string.dates_label_birthday_of, name)
        DateLabel.RelationshipStart -> stringResource(R.string.dates_label_relationship_start)
        is DateLabel.RelativeBirthday -> stringResource(R.string.dates_label_birthday_of, name)
        is DateLabel.Custom -> title
    }

@Composable
internal fun daysUntilText(days: Long): String =
    when (days) {
        0L -> stringResource(R.string.dates_today)
        1L -> stringResource(R.string.dates_tomorrow)
        else -> pluralStringResource(R.plurals.dates_in_days, days.toInt(), days.toInt())
    }

@Composable
private fun DateEditorDialog(
    state: DateEditorState,
    actions: DateEditorActions,
) {
    val spacing = Spacing()
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = {
            Text(stringResource(if (state.editingId == null) R.string.dates_editor_new else R.string.dates_editor_edit))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = actions.onTitleChange,
                    label = { Text(stringResource(R.string.dates_editor_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(modifier = Modifier.selectableGroup()) {
                    DateKind.entries.forEach { kind ->
                        KindRow(kind = kind, selected = state.kind == kind, onSelect = { actions.onKindChange(kind) })
                    }
                }
                DateField(
                    label = stringResource(R.string.dates_editor_date_label),
                    value = state.date,
                    onValueChange = actions.onDateChange,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = spacing.touchTarget)
                            .toggleable(value = state.recurring, onValueChange = actions.onRecurringChange, role = Role.Switch),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.dates_editor_recurring))
                    Switch(checked = state.recurring, onCheckedChange = null)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onSave, enabled = state.canSave) { Text(stringResource(R.string.partner_profile_save)) }
        },
        dismissButton = {
            Row {
                if (state.editingId != null) {
                    TextButton(onClick = actions.onDelete) { Text(stringResource(R.string.dates_editor_delete)) }
                }
                TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.date_field_cancel)) }
            }
        },
    )
}

@Composable
private fun KindRow(
    kind: DateKind,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing().touchTarget)
                .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = stringResource(kind.labelRes()), modifier = Modifier.padding(start = Spacing().sm))
    }
}

@StringRes
private fun DateKind.labelRes(): Int =
    when (this) {
        DateKind.BIRTHDAY -> R.string.dates_kind_birthday
        DateKind.ANNIVERSARY -> R.string.dates_kind_anniversary
        DateKind.OTHER -> R.string.dates_kind_other
    }

private val previewActions = DateEditorActions({}, {}, {}, {}, {}, {}, {})

@Preview(showBackground = true)
@Composable
private fun DatesScreenReadyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        DatesScreen(
            uiState =
                DatesUiState(
                    DatesListState.Ready(
                        listOf(
                            UpcomingDate("a", DateLabel.RelativeBirthday("Léa"), DateKind.BIRTHDAY, LocalDate.of(2027, 3, 11), 1, 38, null),
                            UpcomingDate("b", DateLabel.Custom("Mariage"), DateKind.ANNIVERSARY, LocalDate.of(2027, 6, 20), 102, 12, 7),
                        ),
                    ),
                ),
            onAddClick = {},
            onEdit = {},
            editor = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DatesScreenEmptyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        DatesScreen(
            uiState = DatesUiState(DatesListState.Empty),
            onAddClick = {},
            onEdit = {},
            editor = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DatesEditorPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        DatesScreen(
            uiState = DatesUiState(DatesListState.Empty, DateEditorState(title = "Mariage", date = LocalDate.of(2015, 6, 20))),
            onAddClick = {},
            onEdit = {},
            editor = previewActions,
        )
    }
}
