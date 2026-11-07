package com.bastiensabos.betterhusband.feature.onboarding

import com.bastiensabos.betterhusband.domain.model.AddressForm
import java.time.LocalDate

/** Les trois temps de l'accueil : la promesse, la personne, son anniversaire (facultatif). */
enum class OnboardingStep { WELCOME, PARTNER, BIRTHDAY }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val firstName: String = "",
    val addressForm: AddressForm = AddressForm.NEUTRAL,
    val birthday: LocalDate? = null,
    val saving: Boolean = false,
) {
    val canContinue: Boolean get() = firstName.isNotBlank()
    val canSubmit: Boolean get() = canContinue && !saving
}
