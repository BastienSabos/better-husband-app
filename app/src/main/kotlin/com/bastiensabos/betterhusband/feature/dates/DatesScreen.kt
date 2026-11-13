package com.bastiensabos.betterhusband.feature.dates

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.bastiensabos.betterhusband.ui.component.UndoSnackbarEffect
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun DatesRoute(
    onOpenIdeas: () -> Unit,
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
                onSharedChange = viewModel::onSharedChange,
                onSave = viewModel::onSave,
                onDelete = viewModel::onDelete,
                onDismiss = viewModel::onDismissEditor,
                onUndo = viewModel::onUndo,
                onUndoDismissed = viewModel::onUndoDismissed,
            ),
        modifier = modifier,
        onPrepare = onOpenIdeas,
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
    val onUndo: () -> Unit = {},
    val onUndoDismissed: () -> Unit = {},
    val onSharedChange: (Boolean) -> Unit = {},
)

@Composable
fun DatesScreen(
    uiState: DatesUiState,
    onAddClick: () -> Unit,
    onEdit: (Long) -> Unit,
    editor: DateEditorActions,
    modifier: Modifier = Modifier,
    onPrepare: () -> Unit = {},
) {
    val spacing = Spacing()
    val snackbar = remember { SnackbarHostState() }
    UndoSnackbarEffect(uiState.undo, snackbar, onUndo = editor.onUndo, onDismiss = editor.onUndoDismissed)
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
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
            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    Heading(text = stringResource(R.string.dates_title), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = stringResource(R.string.dates_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { NotificationPermissionPrompt() }
            when (val list = uiState.list) {
                DatesListState.Loading -> {
                    item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                }

                DatesListState.Empty -> {
                    item { Text(text = stringResource(R.string.dates_empty), style = MaterialTheme.typography.bodyLarge) }
                }

                is DatesListState.Ready -> {
                    item(key = "next") { NextDateCard(list.next, list.nextIdeas, onPrepare) }
                    list.months.forEach { group ->
                        item(key = "month-${group.month}") { MonthHeader(group.month, thisYear = list.next.date.year) }
                        items(group.dates, key = { it.key }) { item -> UpcomingDateRow(item, onEdit) }
                    }
                }
            }
        }
    }
    uiState.editor?.let { DateEditorDialog(it, uiState.duo, editor) }
}

/** Place laissée sous la liste pour que la dernière date ne reste pas cachée par le bouton flottant. */
private val FAB_CLEARANCE = 96.dp

/** La prochaine date, en bordeaux : ce qu'il faut préparer en premier, avec ce que le carnet propose. */
@Composable
private fun NextDateCard(
    date: UpcomingDate,
    ideas: Int,
    onPrepare: () -> Unit,
) {
    val spacing = Spacing()
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    Card(
        onClick = onPrepare,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.primary, contentColor = colors.onPrimary),
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DayBadge(
                top = remember(date.date, locale) { date.date.format(DateTimeFormatter.ofPattern("MMM", locale)).uppercase(locale) },
                bottom = date.date.dayOfMonth.toString(),
                container = colors.onPrimary,
                content = colors.primary,
                bigBottom = true,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text =
                        stringResource(
                            R.string.dates_next_overline,
                            daysUntilText(date.daysUntil).replaceFirstChar { it.lowercase(locale) },
                        ),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryContainer,
                )
                Text(text = date.label.text(), style = MaterialTheme.typography.titleLarge)
                Text(
                    text =
                        if (ideas > 0) {
                            pluralStringResource(R.plurals.dates_next_ideas, ideas, ideas)
                        } else {
                            stringResource(R.string.dates_next_no_idea)
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primaryContainer,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** Le mois, et l'année quand ce n'est pas celle de la prochaine date (« Janvier 2028 »). */
@Composable
private fun MonthHeader(
    month: YearMonth,
    thisYear: Int,
) {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = if (month.year == thisYear) "LLLL" else "LLLL yyyy"
    val text =
        remember(
            month,
            locale,
            pattern,
        ) { month.format(DateTimeFormatter.ofPattern(pattern, locale)).replaceFirstChar { it.titlecase(locale) } }
    Heading(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = Spacing().xs),
    )
}

@Composable
private fun DayBadge(
    top: String,
    bottom: String,
    container: Color,
    content: Color,
    bigBottom: Boolean = false,
) {
    Column(
        modifier =
            Modifier
                .widthIn(min = 52.dp)
                .background(container, MaterialTheme.shapes.medium)
                .padding(horizontal = Spacing().xs, vertical = Spacing().xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (bigBottom) {
            Text(text = top, style = MaterialTheme.typography.labelMedium, color = content)
            Text(text = bottom, style = MaterialTheme.typography.headlineSmall, color = content)
        } else {
            Text(text = bottom, style = MaterialTheme.typography.titleLarge, color = content)
            Text(text = top, style = MaterialTheme.typography.labelSmall, color = content)
        }
    }
}

@Composable
private fun UpcomingDateRow(
    item: UpcomingDate,
    onEdit: (Long) -> Unit,
) {
    val spacing = Spacing()
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val dayOfWeek = remember(item.date, locale) { item.date.format(DateTimeFormatter.ofPattern("EEE", locale)) }
    val years = item.yearsSince?.takeIf { it > 0 }
    // Ce qui se fête à deux en rose, le reste (anniversaires, événements) en abricot, comme dans les maquettes.
    val couple = item.kind == DateKind.ANNIVERSARY || item.label == DateLabel.RelationshipStart
    // Seules les dates saisies à la main se modifient ici ; les autres se changent dans la fiche.
    val rowModifier = item.editableId?.let { id -> Modifier.clickable { onEdit(id) } } ?: Modifier
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Row(
            modifier = rowModifier.fillMaxWidth().heightIn(min = spacing.touchTarget).padding(spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DayBadge(
                top = dayOfWeek,
                bottom = item.date.dayOfMonth.toString(),
                container = if (couple) colors.primaryContainer else colors.secondaryContainer,
                content = if (couple) colors.onPrimaryContainer else colors.onSecondaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.label.text(), style = MaterialTheme.typography.titleMedium)
                val yearsText = years?.let { pluralStringResource(R.plurals.dates_years, it, it) }
                val sharedText = if (item.shared) stringResource(R.string.dates_row_shared) else null
                Text(
                    text = listOfNotNull(daysUntilText(item.daysUntil), yearsText, sharedText).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
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
    duo: DatesDuo?,
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
                if (duo != null) {
                    SwitchRow(
                        label =
                            duo.partnerName?.let { stringResource(R.string.dates_editor_shared_with, it) }
                                ?: stringResource(R.string.dates_editor_shared),
                        checked = state.shared,
                        onCheckedChange = actions.onSharedChange,
                    )
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
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing().touchTarget)
                .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing().sm),
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
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
                            UpcomingDate(
                                "b",
                                DateLabel.Custom("Mariage"),
                                DateKind.ANNIVERSARY,
                                LocalDate.of(2027, 6, 20),
                                102,
                                12,
                                7,
                                shared = true,
                            ),
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
            uiState =
                DatesUiState(
                    DatesListState.Empty,
                    DateEditorState(title = "Mariage", date = LocalDate.of(2015, 6, 20), shared = true),
                    duo = DatesDuo(partnerName = "Camille"),
                ),
            onAddClick = {},
            onEdit = {},
            editor = previewActions,
        )
    }
}
