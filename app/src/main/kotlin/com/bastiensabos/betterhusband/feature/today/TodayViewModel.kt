package com.bastiensabos.betterhusband.feature.today

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Squelette du flux du jour. Les cartes viendront d'un repository (Room) dans la brique MVP n°4.
 */
@HiltViewModel
class TodayViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow<TodayUiState>(TodayUiState.Empty)
        val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

        fun onCardDone(cardId: String) {
            _uiState.update { state ->
                if (state !is TodayUiState.Ready) return@update state
                state.copy(cards = state.cards.map { if (it.id == cardId) it.copy(done = true) else it })
            }
        }

        internal fun setCards(cards: List<TodayCard>) {
            _uiState.value = if (cards.isEmpty()) TodayUiState.Empty else TodayUiState.Ready(cards)
        }
    }
