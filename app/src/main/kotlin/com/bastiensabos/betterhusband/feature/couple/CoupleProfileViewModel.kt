package com.bastiensabos.betterhusband.feature.couple

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.data.repository.CoupleProfileRepository
import com.bastiensabos.betterhusband.domain.model.Budget
import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import com.bastiensabos.betterhusband.domain.model.Interest
import com.bastiensabos.betterhusband.domain.model.KidsAtHome
import com.bastiensabos.betterhusband.domain.model.LifeRhythm
import com.bastiensabos.betterhusband.domain.model.LivingArrangement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoupleProfileUiState(
    val loading: Boolean = true,
    val profile: CoupleProfile = CoupleProfile(),
    /** Enregistré : l'écran se referme. */
    val saved: Boolean = false,
)

/**
 * « Notre couple » : quelques réponses facultatives pour choisir des attentions qui vous ressemblent. Toucher une
 * réponse déjà choisie la retire (retour à « pas répondu »).
 */
@HiltViewModel
class CoupleProfileViewModel
    @Inject
    constructor(
        private val repository: CoupleProfileRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CoupleProfileUiState())
        val uiState: StateFlow<CoupleProfileUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val stored = repository.observe().first()
                _uiState.update { it.copy(loading = false, profile = stored) }
            }
        }

        fun onKidsChange(value: KidsAtHome) = edit { it.copy(kids = value.takeUnless { v -> v == it.kids }) }

        fun onLivingChange(value: LivingArrangement) = edit { it.copy(living = value.takeUnless { v -> v == it.living }) }

        fun onBudgetChange(value: Budget) = edit { it.copy(budget = value.takeUnless { v -> v == it.budget }) }

        fun onRhythmChange(value: LifeRhythm) = edit { it.copy(rhythm = value.takeUnless { v -> v == it.rhythm }) }

        fun onInterestToggle(value: Interest) =
            edit { it.copy(interests = if (value in it.interests) it.interests - value else it.interests + value) }

        fun onSave() {
            val state = _uiState.value
            if (state.loading || state.saved) return
            viewModelScope.launch {
                repository.save(state.profile)
                _uiState.update { it.copy(saved = true) }
            }
        }

        private fun edit(change: (CoupleProfile) -> CoupleProfile) = _uiState.update { it.copy(profile = change(it.profile)) }
    }
