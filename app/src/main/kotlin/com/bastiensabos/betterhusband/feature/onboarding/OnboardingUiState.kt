package com.bastiensabos.betterhusband.feature.onboarding

import com.bastiensabos.betterhusband.domain.model.AddressForm

data class OnboardingUiState(
    val firstName: String = "",
    val addressForm: AddressForm = AddressForm.NEUTRAL,
    val saving: Boolean = false,
) {
    val canSubmit: Boolean get() = firstName.isNotBlank() && !saving
}
