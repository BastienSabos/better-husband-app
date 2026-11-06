package com.bastiensabos.betterhusband.feature.partner

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.SizeKind
import com.bastiensabos.betterhusband.domain.model.TasteCategory
import com.bastiensabos.betterhusband.ui.component.AddressFormPicker
import com.bastiensabos.betterhusband.ui.component.DateField
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing
import java.time.LocalDate

@Composable
fun PartnerProfileRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PartnerProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }
    PartnerProfileScreen(
        uiState = uiState,
        onBack = onDone,
        onFirstNameChange = viewModel::onFirstNameChange,
        onAddressFormSelected = viewModel::onAddressFormSelected,
        onBirthdayChange = viewModel::onBirthdayChange,
        onRelationshipStartChange = viewModel::onRelationshipStartChange,
        onTastesChange = viewModel::onTastesChange,
        onSizeChange = viewModel::onSizeChange,
        onAddRelative = viewModel::onAddRelative,
        onRelativeChange = viewModel::onRelativeChange,
        onRemoveRelative = viewModel::onRemoveRelative,
        onSave = viewModel::onSave,
        modifier = modifier,
    )
}

@Composable
fun PartnerProfileScreen(
    uiState: PartnerProfileUiState,
    onBack: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onAddressFormSelected: (AddressForm) -> Unit,
    onBirthdayChange: (LocalDate?) -> Unit,
    onRelationshipStartChange: (LocalDate?) -> Unit,
    onTastesChange: (TasteCategory, String) -> Unit,
    onSizeChange: (SizeKind, String) -> Unit,
    onAddRelative: () -> Unit,
    onRelativeChange: (RelativeDraft) -> Unit,
    onRemoveRelative: (Long) -> Unit,
    onSave: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                Text(stringResource(R.string.partner_profile_back))
            }
            Text(text = stringResource(R.string.partner_profile_title), style = MaterialTheme.typography.headlineMedium)

            if (!uiState.loaded) return@Column

            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = onFirstNameChange,
                label = { Text(stringResource(R.string.onboarding_first_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            AddressFormPicker(selected = uiState.addressForm, onSelected = onAddressFormSelected)

            Section(R.string.partner_profile_section_dates) {
                DateField(
                    label = stringResource(R.string.partner_profile_birthday),
                    value = uiState.birthday,
                    onValueChange = onBirthdayChange,
                )
                DateField(
                    label = stringResource(R.string.partner_profile_relationship_start),
                    value = uiState.relationshipStart,
                    onValueChange = onRelationshipStartChange,
                )
            }

            Section(R.string.partner_profile_section_tastes) {
                TasteCategory.entries.forEach { category ->
                    OutlinedTextField(
                        value = uiState.tastes[category].orEmpty(),
                        onValueChange = { onTastesChange(category, it) },
                        label = { Text(stringResource(category.labelRes())) },
                        supportingText = { Text(stringResource(R.string.partner_profile_tastes_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Section(R.string.partner_profile_section_sizes) {
                SizeKind.entries.forEach { kind ->
                    OutlinedTextField(
                        value = uiState.sizes[kind].orEmpty(),
                        onValueChange = { onSizeChange(kind, it) },
                        label = { Text(stringResource(kind.labelRes())) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Section(R.string.partner_profile_section_relatives) {
                uiState.relatives.forEach { relative ->
                    key(relative.id) {
                        RelativeEditor(relative, onChange = onRelativeChange, onRemove = { onRemoveRelative(relative.id) })
                    }
                }
                OutlinedButton(
                    onClick = onAddRelative,
                    modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget),
                ) {
                    Text(stringResource(R.string.partner_profile_add_relative))
                }
            }

            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget),
            ) {
                Text(stringResource(R.string.partner_profile_save))
            }
        }
    }
}

@Composable
private fun Section(
    @StringRes titleRes: Int,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing().sm)) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleLarge)
        content()
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

@Preview(showBackground = true)
@Composable
private fun PartnerProfileScreenEmptyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        PartnerProfileScreen(
            uiState = PartnerProfileUiState(loaded = true, firstName = "Camille"),
            onBack = {},
            onFirstNameChange = {},
            onAddressFormSelected = {},
            onBirthdayChange = {},
            onRelationshipStartChange = {},
            onTastesChange = { _, _ -> },
            onSizeChange = { _, _ -> },
            onAddRelative = {},
            onRelativeChange = {},
            onRemoveRelative = {},
            onSave = {},
        )
    }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun PartnerProfileScreenFilledPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        PartnerProfileScreen(
            uiState =
                PartnerProfileUiState(
                    loaded = true,
                    firstName = "Camille",
                    addressForm = AddressForm.FEMININE,
                    birthday = LocalDate.of(1992, 4, 12),
                    tastes = mapOf(TasteCategory.FLOWERS to "Pivoines, tulipes", TasteCategory.COLORS to "Vert sauge"),
                    sizes = mapOf(SizeKind.SHOES to "39"),
                    relatives = listOf(RelativeDraft(0, "Sœur", "Léa", LocalDate.of(1989, 9, 3))),
                ),
            onBack = {},
            onFirstNameChange = {},
            onAddressFormSelected = {},
            onBirthdayChange = {},
            onRelationshipStartChange = {},
            onTastesChange = { _, _ -> },
            onSizeChange = { _, _ -> },
            onAddRelative = {},
            onRelativeChange = {},
            onRemoveRelative = {},
            onSave = {},
        )
    }
}
