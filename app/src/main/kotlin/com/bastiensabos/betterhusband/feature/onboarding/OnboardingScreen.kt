package com.bastiensabos.betterhusband.feature.onboarding

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.ui.component.AddressFormPicker
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing

@Composable
fun OnboardingRoute(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(
        uiState = uiState,
        onFirstNameChange = viewModel::onFirstNameChange,
        onAddressFormSelected = viewModel::onAddressFormSelected,
        onSubmit = viewModel::onSubmit,
        modifier = modifier,
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onFirstNameChange: (String) -> Unit,
    onAddressFormSelected: (AddressForm) -> Unit,
    onSubmit: () -> Unit,
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
            Text(text = stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
            Text(text = stringResource(R.string.onboarding_intro), style = MaterialTheme.typography.bodyLarge)

            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = onFirstNameChange,
                label = { Text(stringResource(R.string.onboarding_first_name_label)) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )

            AddressFormPicker(selected = uiState.addressForm, onSelected = onAddressFormSelected)

            Text(text = stringResource(R.string.onboarding_privacy_note), style = MaterialTheme.typography.bodyMedium)

            Button(
                onClick = onSubmit,
                enabled = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget),
            ) {
                Text(stringResource(R.string.onboarding_submit))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenEmptyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        OnboardingScreen(OnboardingUiState(), onFirstNameChange = {}, onAddressFormSelected = {}, onSubmit = {})
    }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun OnboardingScreenFilledPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        OnboardingScreen(
            OnboardingUiState(firstName = "Alex", addressForm = AddressForm.FEMININE),
            onFirstNameChange = {},
            onAddressFormSelected = {},
            onSubmit = {},
        )
    }
}
