package com.bastiensabos.betterhusband.feature.wishes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.data.repository.IdeaRepository
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
    /** Envies de l'autre dont je m'occupe déjà (une idée de mon carnet y est liée). */
    val takenCare: Set<String> = emptySet(),
) {
    val canAdd: Boolean get() = draft.isNotBlank()
}

/** Mes envies (que l'autre voit) et ses envies (en lecture seule). */
@HiltViewModel
class WishesViewModel
    @Inject
    constructor(
        private val repository: WishRepository,
        private val ideas: IdeaRepository,
    ) : ViewModel() {
        private val draft = MutableStateFlow("")

        val uiState: StateFlow<WishesUiState> =
            combine(repository.observeMine(), repository.observeTheirs(), draft, ideas.observeAll()) { mine, theirs, text, notebook ->
                WishesUiState(mine, theirs, text, notebook.mapNotNullTo(mutableSetOf()) { it.wishUid })
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WishesUiState())

        fun onDraftChange(value: String) = draft.update { value.take(DuoLimits.MAX_TEXT) }

        fun onAdd() {
            val text = draft.value
            if (text.isBlank()) return
            // Le champ est vidé tout de suite : un double appui ne crée pas deux envies.
            draft.value = ""
            viewModelScope.launch { repository.add(text) }
        }

        /** Crée une idée privée dans mon carnet : rien ne part vers l'autre, la surprise reste une surprise. */
        fun onTakeCare(wish: Wish) {
            viewModelScope.launch { ideas.addForWish(wish.text, wish.uid) }
        }

        fun onDelete(id: Long) {
            viewModelScope.launch { repository.delete(id) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
