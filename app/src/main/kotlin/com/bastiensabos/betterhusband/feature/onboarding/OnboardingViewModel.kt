package com.bastiensabos.betterhusband.feature.onboarding

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
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Crée la fiche partenaire. Une fois enregistrée, l'écran racine bascule seul sur l'app (voir AppViewModel). */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val partnerRepository: PartnerRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

        fun onFirstNameChange(value: String) = _uiState.update { it.copy(firstName = value.take(MAX_NAME_LENGTH)) }

        fun onAddressFormSelected(value: AddressForm) = _uiState.update { it.copy(addressForm = value) }

        fun onSubmit() {
            val state = _uiState.value
            if (!state.canSubmit) return
            _uiState.update { it.copy(saving = true) }
            viewModelScope.launch {
                try {
                    partnerRepository.savePartner(Partner(state.firstName, state.addressForm))
                } finally {
                    _uiState.update { it.copy(saving = false) }
                }
            }
        }

        private companion object {
            const val MAX_NAME_LENGTH = 40
        }
    }
