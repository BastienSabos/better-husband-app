package com.bastiensabos.betterhusband.feature.partner

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.SizeKind
import com.bastiensabos.betterhusband.domain.model.TasteCategory
import com.bastiensabos.betterhusband.ui.component.AddressFormPicker
import com.bastiensabos.betterhusband.ui.component.BackTopBar
import com.bastiensabos.betterhusband.ui.component.DateField
import com.bastiensabos.betterhusband.ui.component.Heading
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ce que l'écran renvoie au ViewModel ; [onBack] quitte la fiche. */
data class PartnerProfileActions(
    val onBack: () -> Unit,
    val onEdit: (PartnerSection) -> Unit,
    val onCancelEdit: () -> Unit,
    val onFirstNameChange: (String) -> Unit,
    val onAddressFormSelect: (AddressForm) -> Unit,
    val onBirthdayChange: (LocalDate?) -> Unit,
    val onRelationshipStartChange: (LocalDate?) -> Unit,
    val onTastesChange: (TasteCategory, String) -> Unit,
    val onSizeChange: (SizeKind, String) -> Unit,
    val onAddRelative: () -> Unit,
    val onRelativeChange: (RelativeDraft) -> Unit,
    val onRemoveRelative: (Long) -> Unit,
    val onSave: () -> Unit,
)

@Composable
fun PartnerProfileRoute(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PartnerProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PartnerProfileScreen(
        uiState = uiState,
        actions =
            PartnerProfileActions(
                onBack = onFinish,
                onEdit = viewModel::onEdit,
                // Sans fiche enregistrée, il n'y a pas de lecture où revenir : annuler quitte l'écran.
                onCancelEdit = { if (uiState.hasProfile) viewModel.onCancelEdit() else onFinish() },
                onFirstNameChange = viewModel::onFirstNameChange,
                onAddressFormSelect = viewModel::onAddressFormSelect,
                onBirthdayChange = viewModel::onBirthdayChange,
                onRelationshipStartChange = viewModel::onRelationshipStartChange,
                onTastesChange = viewModel::onTastesChange,
                onSizeChange = viewModel::onSizeChange,
                onAddRelative = viewModel::onAddRelative,
                onRelativeChange = viewModel::onRelativeChange,
                onRemoveRelative = viewModel::onRemoveRelative,
                onSave = viewModel::onSave,
            ),
        modifier = modifier,
    )
}

@Composable
fun PartnerProfileScreen(
    uiState: PartnerProfileUiState,
    actions: PartnerProfileActions,
    modifier: Modifier = Modifier,
) {
    val spacing = Spacing()
    val editing = uiState.editing
    BackHandler(enabled = editing != null, onBack = actions.onCancelEdit)
    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(
                title = stringResource(editing?.titleRes() ?: R.string.partner_profile_title),
                onBack = if (editing != null) actions.onCancelEdit else actions.onBack,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.screen),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            when {
                !uiState.loaded -> Unit
                editing == null -> ProfileReading(uiState, actions.onEdit)
                else -> SectionEditor(editing, uiState, actions)
            }
        }
    }
}

// --- Lecture ---------------------------------------------------------------------------------------------------

@Composable
private fun ProfileReading(
    state: PartnerProfileUiState,
    onEdit: (PartnerSection) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(Spacing().lg)) {
        ProfileHeader(state, onEdit = { onEdit(PartnerSection.IDENTITY) })
        ReadingSection(R.string.partner_profile_section_tastes, onEdit = { onEdit(PartnerSection.TASTES) }) {
            val tastes = state.tasteLabels
            if (tastes.isEmpty()) {
                EmptyText(R.string.partner_profile_tastes_empty)
            } else {
                TasteCategory.entries.filter { it in tastes }.forEach { category ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing().xxs)) {
                        Text(
                            text = stringResource(category.labelRes()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing().xs),
                            verticalArrangement = Arrangement.spacedBy(Spacing().xs),
                        ) {
                            tastes.getValue(category).forEach { Pill(it.replaceFirstChar { c -> c.titlecase(locale) }) }
                        }
                    }
                }
            }
        }
        ReadingSection(R.string.partner_profile_section_sizes, onEdit = { onEdit(PartnerSection.SIZES) }) {
            val sizes =
                SizeKind.entries.mapNotNull { kind ->
                    state.sizes[kind]
                        ?.trim()
                        ?.takeIf(String::isNotEmpty)
                        ?.let { kind to it }
                }
            if (sizes.isEmpty()) {
                EmptyText(R.string.partner_profile_sizes_empty)
            } else {
                sizes.forEach { (kind, value) -> LabelValue(stringResource(kind.labelRes()), value) }
            }
        }
        ReadingSection(R.string.partner_profile_section_relatives, onEdit = { onEdit(PartnerSection.RELATIVES) }) {
            val relatives = state.relatives.filter { it.name.isNotBlank() }
            if (relatives.isEmpty()) {
                EmptyText(R.string.partner_profile_relatives_empty)
            } else {
                relatives.forEach { RelativeRow(it) }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    state: PartnerProfileUiState,
    onEdit: () -> Unit,
) {
    val spacing = Spacing()
    val locale = LocalConfiguration.current.locales[0]
    val editDescription =
        stringResource(R.string.partner_profile_edit_description, stringResource(R.string.partner_profile_section_identity))
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Initial(state.firstName, size = 96.dp, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        Heading(text = state.firstName, style = MaterialTheme.typography.headlineMedium)
        val chips =
            listOfNotNull(
                state.yearsTogether?.let { years ->
                    if (years == 0) {
                        stringResource(R.string.partner_profile_together_new)
                    } else {
                        pluralStringResource(R.plurals.partner_profile_years_together, years, years)
                    }
                },
                state.birthday?.let { stringResource(R.string.partner_profile_birthday_on, dayAndMonth(it, locale)) },
            )
        if (chips.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                chips.forEach { Pill(it) }
            }
        }
        TextButton(
            onClick = onEdit,
            modifier =
                Modifier
                    .heightIn(min = spacing.touchTarget)
                    .semantics { contentDescription = editDescription },
        ) {
            Text(stringResource(R.string.partner_profile_edit))
        }
    }
}

@Composable
private fun ReadingSection(
    @StringRes titleRes: Int,
    onEdit: () -> Unit,
    content: @Composable () -> Unit,
) {
    val spacing = Spacing()
    val title = stringResource(titleRes)
    val editDescription = stringResource(R.string.partner_profile_edit_description, title)
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier.padding(start = spacing.md, end = spacing.xs, top = spacing.xs, bottom = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Heading(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onEdit,
                    modifier =
                        Modifier
                            .heightIn(min = spacing.touchTarget)
                            .semantics { contentDescription = editDescription },
                ) {
                    Text(stringResource(R.string.partner_profile_edit))
                }
            }
            Column(modifier = Modifier.padding(end = spacing.xs), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                content()
            }
        }
    }
}

@Composable
private fun Pill(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing().sm, vertical = Spacing().xs - 2.dp),
        )
    }
}

@Composable
private fun LabelValue(
    label: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing().md), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
    }
}

@Composable
private fun RelativeRow(relative: RelativeDraft) {
    val locale = LocalConfiguration.current.locales[0]
    val details =
        listOfNotNull(
            relative.relation.trim().takeIf(String::isNotEmpty),
            relative.birthday?.let { stringResource(R.string.partner_profile_birthday_on, dayAndMonth(it, locale)) },
        ).joinToString(" · ")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing().sm), verticalAlignment = Alignment.CenterVertically) {
        Initial(relative.name, size = 40.dp, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = relative.name.trim(), style = MaterialTheme.typography.titleMedium)
            if (details.isNotEmpty()) {
                Text(text = details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Pastille avec l'initiale : décorative, le prénom est lu juste à côté. */
@Composable
private fun Initial(
    name: String,
    size: Dp,
    container: Color,
    content: Color,
) {
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = Modifier.size(size).clearAndSetSemantics {},
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.trim().take(1).uppercase(),
                style = if (size > 48.dp) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun EmptyText(
    @StringRes textRes: Int,
) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun dayAndMonth(
    date: LocalDate,
    locale: Locale,
): String = remember(date, locale) { date.format(DateTimeFormatter.ofPattern("d MMMM", locale)) }

// --- Édition d'une section -------------------------------------------------------------------------------------

@Composable
private fun SectionEditor(
    section: PartnerSection,
    state: PartnerProfileUiState,
    actions: PartnerProfileActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing().lg)) {
        val spacing = Spacing()
        when (section) {
            PartnerSection.IDENTITY -> {
                IdentityFields(state, actions)
            }

            PartnerSection.TASTES -> {
                TasteCategory.entries.forEach { category ->
                    OutlinedTextField(
                        value = state.tastes[category].orEmpty(),
                        onValueChange = { actions.onTastesChange(category, it) },
                        label = { Text(stringResource(category.labelRes())) },
                        supportingText = { Text(stringResource(R.string.partner_profile_tastes_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            PartnerSection.SIZES -> {
                SizeKind.entries.forEach { kind ->
                    OutlinedTextField(
                        value = state.sizes[kind].orEmpty(),
                        onValueChange = { actions.onSizeChange(kind, it) },
                        label = { Text(stringResource(kind.labelRes())) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            PartnerSection.RELATIVES -> {
                state.relatives.forEach { relative ->
                    key(relative.id) {
                        RelativeEditor(relative, onChange = actions.onRelativeChange, onRemove = { actions.onRemoveRelative(relative.id) })
                    }
                }
                OutlinedButton(
                    onClick = actions.onAddRelative,
                    modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget),
                ) {
                    Text(stringResource(R.string.partner_profile_add_relative))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            OutlinedButton(
                onClick = actions.onCancelEdit,
                modifier = Modifier.weight(1f).heightIn(min = spacing.touchTarget),
            ) {
                Text(stringResource(R.string.partner_profile_cancel))
            }
            Button(
                onClick = actions.onSave,
                enabled = state.canSave,
                modifier = Modifier.weight(1f).heightIn(min = spacing.touchTarget),
            ) {
                Text(stringResource(R.string.partner_profile_save))
            }
        }
    }
}

@Composable
private fun IdentityFields(
    state: PartnerProfileUiState,
    actions: PartnerProfileActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing().lg)) {
        OutlinedTextField(
            value = state.firstName,
            onValueChange = actions.onFirstNameChange,
            label = { Text(stringResource(R.string.onboarding_first_name_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        AddressFormPicker(selected = state.addressForm, onSelect = actions.onAddressFormSelect)
        DateField(
            label = stringResource(R.string.partner_profile_birthday),
            value = state.birthday,
            onValueChange = actions.onBirthdayChange,
        )
        DateField(
            label = stringResource(R.string.partner_profile_relationship_start),
            value = state.relationshipStart,
            onValueChange = actions.onRelationshipStartChange,
        )
    }
}

@Composable
private fun RelativeEditor(
    relative: RelativeDraft,
    onChange: (RelativeDraft) -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = Spacing()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        OutlinedTextField(
            value = relative.name,
            onValueChange = { onChange(relative.copy(name = it.take(MAX_FIELD_LENGTH))) },
            label = { Text(stringResource(R.string.partner_profile_relative_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = relative.relation,
            onValueChange = { onChange(relative.copy(relation = it.take(MAX_FIELD_LENGTH))) },
            label = { Text(stringResource(R.string.partner_profile_relative_relation)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        DateField(
            label = stringResource(R.string.partner_profile_birthday),
            value = relative.birthday,
            onValueChange = { onChange(relative.copy(birthday = it)) },
        )
        TextButton(onClick = onRemove, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
            Text(stringResource(R.string.partner_profile_remove_relative))
        }
    }
}

private const val MAX_FIELD_LENGTH = 40

@StringRes
private fun PartnerSection.titleRes(): Int =
    when (this) {
        PartnerSection.IDENTITY -> R.string.partner_profile_section_identity
        PartnerSection.TASTES -> R.string.partner_profile_section_tastes
        PartnerSection.SIZES -> R.string.partner_profile_section_sizes
        PartnerSection.RELATIVES -> R.string.partner_profile_section_relatives
    }

@StringRes
private fun TasteCategory.labelRes(): Int =
    when (this) {
        TasteCategory.FLOWERS -> R.string.taste_flowers
        TasteCategory.FOOD -> R.string.taste_food
        TasteCategory.COLORS -> R.string.taste_colors
        TasteCategory.BRANDS -> R.string.taste_brands
    }

@StringRes
private fun SizeKind.labelRes(): Int =
    when (this) {
        SizeKind.CLOTHES -> R.string.size_clothes
        SizeKind.SHOES -> R.string.size_shoes
        SizeKind.RING -> R.string.size_ring
    }

internal val previewActions = PartnerProfileActions({}, {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}, {})

internal val previewProfile =
    PartnerProfileUiState(
        loaded = true,
        hasProfile = true,
        today = LocalDate.of(2026, 11, 7),
        firstName = "Camille",
        addressForm = AddressForm.FEMININE,
        birthday = LocalDate.of(1992, 11, 21),
        relationshipStart = LocalDate.of(2017, 6, 3),
        tastes = mapOf(TasteCategory.FLOWERS to "Pivoines, tulipes", TasteCategory.FOOD to "Thaï, chocolat noir"),
        sizes = mapOf(SizeKind.SHOES to "39", SizeKind.CLOTHES to "M"),
        relatives = listOf(RelativeDraft(0, "Sœur", "Léa", LocalDate.of(1989, 9, 3))),
    )

@Preview(showBackground = true)
@Composable
private fun PartnerProfileReadingPreview() {
    BetterHusbandTheme(dynamicColor = false) { PartnerProfileScreen(previewProfile, previewActions) }
}

@Preview(showBackground = true)
@Composable
private fun PartnerProfileEmptyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        PartnerProfileScreen(PartnerProfileUiState(loaded = true, hasProfile = true, firstName = "Camille"), previewActions)
    }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun PartnerProfileEditingPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        PartnerProfileScreen(previewProfile.copy(editing = PartnerSection.TASTES), previewActions)
    }
}
