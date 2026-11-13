package com.bastiensabos.betterhusband.feature.wishes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.data.repository.Wish
import com.bastiensabos.betterhusband.data.repository.WishRepository
import com.bastiensabos.betterhusband.domain.duo.DuoLimits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WishesUiState(
    val mine: List<Wish> = emptyList(),
    val theirs: List<Wish> = emptyList(),
    val draft: String = "",
) {
    val canAdd: Boolean get() = draft.isNotBlank()
}

/** Mes envies (que l'autre voit) et ses envies (en lecture seule). */
@HiltViewModel
class WishesViewModel
    @Inject
    constructor(
        private val repository: WishRepository,
    ) : ViewModel() {
        private val draft = MutableStateFlow("")

        val uiState: StateFlow<WishesUiState> =
            combine(repository.observeMine(), repository.observeTheirs(), draft) { mine, theirs, text ->
                WishesUiState(mine, theirs, text)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WishesUiState())

        fun onDraftChange(value: String) = draft.update { value.take(DuoLimits.MAX_TEXT) }

        fun onAdd() {
            val text = draft.value
            if (text.isBlank()) return
            // Le champ est vidé tout de suite : un double appui ne crée pas deux envies.
            draft.value = ""
            viewModelScope.launch { repository.add(text) }
        }

        fun onDelete(id: Long) {
            viewModelScope.launch { repository.delete(id) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
