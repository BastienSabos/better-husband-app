package com.bastiensabos.betterhusband.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.data.repository.PartnerRepository
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.Partner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Accueil en trois temps qui crée la fiche partenaire. Une fois enregistrée, l'écran racine bascule seul sur l'app
 * (voir AppViewModel). La saisie survit à la mort du processus : un appel ou un SMS ne la fait pas perdre.
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val partnerRepository: PartnerRepository,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                OnboardingUiState(
                    step = savedState.get<String>(KEY_STEP)?.let(OnboardingStep::valueOf) ?: OnboardingStep.WELCOME,
                    firstName = savedState[KEY_FIRST_NAME] ?: "",
                    addressForm = savedState.get<String>(KEY_ADDRESS_FORM)?.let(AddressForm::valueOf) ?: AddressForm.NEUTRAL,
                    birthday = savedState.get<String>(KEY_BIRTHDAY)?.let(LocalDate::parse),
                ),
            )
        val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

        fun onStart() = moveTo(OnboardingStep.PARTNER)

        fun onFirstNameChange(value: String) = edit { it.copy(firstName = value.take(MAX_NAME_LENGTH)) }

        fun onAddressFormSelect(value: AddressForm) = edit { it.copy(addressForm = value) }

        fun onContinue() {
            if (_uiState.value.canContinue) moveTo(OnboardingStep.BIRTHDAY)
        }

        fun onBirthdayChange(value: LocalDate?) = edit { it.copy(birthday = value) }

        /** Retour d'une étape ; rien à faire depuis la première, où le système reprend la main. */
        fun onBack() =
            when (_uiState.value.step) {
                OnboardingStep.WELCOME -> Unit
                OnboardingStep.PARTNER -> moveTo(OnboardingStep.WELCOME)
                OnboardingStep.BIRTHDAY -> moveTo(OnboardingStep.PARTNER)
            }

        /** Termine l'accueil ; [withBirthday] à faux, c'est « Plus tard » : la date saisie est ignorée. */
        fun onSubmit(withBirthday: Boolean = true) {
            val state = _uiState.value
            if (!state.canSubmit) return
            _uiState.update { it.copy(saving = true) }
            viewModelScope.launch {
                try {
                    partnerRepository.savePartner(
                        Partner(
                            firstName = state.firstName.trim(),
                            addressForm = state.addressForm,
                            birthday = state.birthday.takeIf { withBirthday },
                        ),
                    )
                } finally {
                    _uiState.update { it.copy(saving = false) }
                }
            }
        }

        private fun moveTo(step: OnboardingStep) = edit { it.copy(step = step) }

        private fun edit(change: (OnboardingUiState) -> OnboardingUiState) {
            val state = _uiState.updateAndGet(change)
            savedState[KEY_STEP] = state.step.name
            savedState[KEY_FIRST_NAME] = state.firstName
            savedState[KEY_ADDRESS_FORM] = state.addressForm.name
            savedState[KEY_BIRTHDAY] = state.birthday?.toString()
        }

        private companion object {
            const val MAX_NAME_LENGTH = 40
            const val KEY_STEP = "step"
            const val KEY_FIRST_NAME = "firstName"
            const val KEY_ADDRESS_FORM = "addressForm"
            const val KEY_BIRTHDAY = "birthday"
        }
    }
