package com.bastiensabos.betterhusband.feature.ideas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.data.repository.IdeaRepository
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShareIdeaUiState(
    val text: String = "",
    val category: IdeaCategory = IdeaCategory.GIFT,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = text.isNotBlank() && !saving
}

/** Reçoit un texte partagé depuis une autre appli et le range dans le carnet après validation de l'utilisateur. */
@HiltViewModel
class ShareIdeaViewModel
    @Inject
    constructor(
        private val ideaRepository: IdeaRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ShareIdeaUiState())
        val uiState: StateFlow<ShareIdeaUiState> = _uiState.asStateFlow()

        /** Pré-remplit le champ avec le texte partagé, sans écraser ce que l'utilisateur a déjà tapé. */
        fun onSharedText(value: CharSequence?) {
            val shared = value?.toString()?.trim().orEmpty()
            if (shared.isEmpty()) return
            _uiState.update { if (it.text.isEmpty()) it.copy(text = shared.take(MAX_TEXT_LENGTH)) else it }
        }

        fun onTextChange(value: String) = _uiState.update { it.copy(text = value.take(MAX_TEXT_LENGTH)) }

        fun onCategoryChange(value: IdeaCategory) = _uiState.update { it.copy(category = value) }

        fun onSave() {
            val state = _uiState.value
            if (!state.canSave) return
            _uiState.update { it.copy(saving = true) }
            viewModelScope.launch {
                try {
                    ideaRepository.add(state.text, state.category)
                    _uiState.update { it.copy(saved = true) }
                } finally {
                    _uiState.update { it.copy(saving = false) }
                }
            }
        }

        private companion object {
            const val MAX_TEXT_LENGTH = 200
        }
    }
