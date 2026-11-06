package com.bastiensabos.betterhusband

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.data.repository.PartnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AppUiState {
    data object Loading : AppUiState

    /** Pas encore de fiche partenaire : on montre l'onboarding. */
    data object NeedsOnboarding : AppUiState

    data object Ready : AppUiState
}

/** Décide de l'écran racine à partir de la seule présence de la fiche partenaire. */
@HiltViewModel
class AppViewModel
    @Inject
    constructor(
        partnerRepository: PartnerRepository,
    ) : ViewModel() {
        val uiState: StateFlow<AppUiState> =
            partnerRepository
                .observePartner()
                .map { if (it == null) AppUiState.NeedsOnboarding else AppUiState.Ready }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppUiState.Loading)

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
