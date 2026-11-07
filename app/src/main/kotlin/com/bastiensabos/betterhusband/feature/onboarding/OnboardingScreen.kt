package com.bastiensabos.betterhusband.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.ui.component.AddressFormPicker
import com.bastiensabos.betterhusband.ui.component.BackTopBar
import com.bastiensabos.betterhusband.ui.component.DateField
import com.bastiensabos.betterhusband.ui.component.Heading
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing
import java.time.LocalDate

/** Ce que l'accueil renvoie au ViewModel. */
data class OnboardingActions(
    val onStart: () -> Unit,
    val onBack: () -> Unit,
    val onFirstNameChange: (String) -> Unit,
    val onAddressFormSelect: (AddressForm) -> Unit,
    val onContinue: () -> Unit,
    val onBirthdayChange: (LocalDate?) -> Unit,
    val onFinish: () -> Unit,
    val onLater: () -> Unit,
)

@Composable
fun OnboardingRoute(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(
        uiState = uiState,
        actions =
            OnboardingActions(
                onStart = viewModel::onStart,
                onBack = viewModel::onBack,
                onFirstNameChange = viewModel::onFirstNameChange,
                onAddressFormSelect = viewModel::onAddressFormSelect,
                onContinue = viewModel::onContinue,
                onBirthdayChange = viewModel::onBirthdayChange,
                onFinish = { viewModel.onSubmit() },
                onLater = { viewModel.onSubmit(withBirthday = false) },
            ),
        modifier = modifier,
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    val spacing = Spacing()
    val step = uiState.step
    BackHandler(enabled = step != OnboardingStep.WELCOME, onBack = actions.onBack)
    Scaffold(
        modifier = modifier,
        topBar = {
            if (step != OnboardingStep.WELCOME) {
                BackTopBar(
                    title = stringResource(R.string.onboarding_step, step.ordinal + 1, OnboardingStep.entries.size),
                    onBack = actions.onBack,
                )
            }
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
            if (step != OnboardingStep.WELCOME) {
                LinearProgressIndicator(
                    progress = { (step.ordinal + 1f) / OnboardingStep.entries.size },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(actions.onStart)
                OnboardingStep.PARTNER -> PartnerStep(uiState, actions)
                OnboardingStep.BIRTHDAY -> BirthdayStep(uiState, actions)
            }
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    val spacing = Spacing()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Heading(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(top = spacing.xl),
        )
        Text(text = stringResource(R.string.onboarding_welcome_body), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.onboarding_privacy_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(R.string.onboarding_submit, enabled = true, onClick = onStart)
    }
}

@Composable
private fun PartnerStep(
    state: OnboardingUiState,
    actions: OnboardingActions,
) {
    val spacing = Spacing()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Heading(text = stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
        Text(text = stringResource(R.string.onboarding_intro), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = state.firstName,
            onValueChange = actions.onFirstNameChange,
            label = { Text(stringResource(R.string.onboarding_first_name_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { actions.onContinue() }),
            modifier = Modifier.fillMaxWidth(),
        )
        AddressFormPicker(selected = state.addressForm, onSelect = actions.onAddressFormSelect)
        if (state.firstName.isNotBlank()) AddressPreview(state.firstName.trim(), state.addressForm)
        PrimaryButton(R.string.onboarding_continue, enabled = state.canContinue, onClick = actions.onContinue)
    }
}

/** Une phrase de l'app telle qu'elle parlera de la personne : le choix se comprend en le voyant. */
@Composable
private fun AddressPreview(
    name: String,
    form: AddressForm,
) {
    val spacing = Spacing()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(spacing.md).semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(text = stringResource(R.string.onboarding_preview_label), style = MaterialTheme.typography.labelLarge)
            Text(text = stringResource(form.previewRes(), name), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun BirthdayStep(
    state: OnboardingUiState,
    actions: OnboardingActions,
) {
    val spacing = Spacing()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Heading(
            text = stringResource(R.string.onboarding_birthday_title, state.firstName.trim()),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(text = stringResource(R.string.onboarding_birthday_body), style = MaterialTheme.typography.bodyLarge)
        DateField(
            label = stringResource(R.string.partner_profile_birthday),
            value = state.birthday,
            onValueChange = actions.onBirthdayChange,
        )
        PrimaryButton(R.string.onboarding_finish, enabled = state.canSubmit, onClick = actions.onFinish)
        TextButton(
            onClick = actions.onLater,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget),
        ) {
            Text(stringResource(R.string.onboarding_later))
        }
    }
}

@Composable
private fun PrimaryButton(
    @StringRes textRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing().touchTarget),
    ) {
        Text(stringResource(textRes))
    }
}

@StringRes
private fun AddressForm.previewRes(): Int =
    when (this) {
        AddressForm.MASCULINE -> R.string.onboarding_preview_masculine
        AddressForm.FEMININE -> R.string.onboarding_preview_feminine
        AddressForm.NEUTRAL -> R.string.onboarding_preview_neutral
    }

internal val previewOnboardingActions = OnboardingActions({}, {}, {}, {}, {}, {}, {}, {})

@Preview(showBackground = true)
@Composable
private fun OnboardingWelcomePreview() {
    BetterHusbandTheme(dynamicColor = false) { OnboardingScreen(OnboardingUiState(), previewOnboardingActions) }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun OnboardingPartnerPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        OnboardingScreen(
            OnboardingUiState(step = OnboardingStep.PARTNER, firstName = "Alex", addressForm = AddressForm.FEMININE),
            previewOnboardingActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingBirthdayPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        OnboardingScreen(OnboardingUiState(step = OnboardingStep.BIRTHDAY, firstName = "Alex"), previewOnboardingActions)
    }
}
