package com.bastiensabos.betterhusband.feature.ideas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.data.repository.DuoRepository
import com.bastiensabos.betterhusband.data.repository.IdeaRepository
import com.bastiensabos.betterhusband.domain.model.Idea
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import com.bastiensabos.betterhusband.ui.component.UndoNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Carnet d'idées : capture rapide en haut, liste filtrable dessous, édition en boîte de dialogue. */
@HiltViewModel
class IdeasViewModel
    @Inject
    constructor(
        private val ideaRepository: IdeaRepository,
        duoRepository: DuoRepository,
    ) : ViewModel() {
        private val local = MutableStateFlow(IdeasUiState())

        /** La dernière idée supprimée, tant que « Annuler » est proposé. */
        private var deleted: Idea? = null

        val uiState: StateFlow<IdeasUiState> =
            combine(ideaRepository.observeAll(), duoRepository.observeSession(), local) { ideas, session, state ->
                val list =
                    when {
                        ideas.isEmpty() -> IdeasListState.Empty
                        else -> IdeasListState.Ready(ideas.filter { state.filter == null || it.category == state.filter })
                    }
                state.copy(list = list, wishesAvailable = session != null)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), IdeasUiState())

        fun onDraftChange(value: String) = local.update { it.copy(draft = value.take(MAX_TEXT_LENGTH)) }

        fun onDraftCategoryChange(value: IdeaCategory) = local.update { it.copy(draftCategory = value) }

        fun onAdd() {
            val state = local.value
            if (!state.canAdd) return
            // Le champ est vidé tout de suite : un double appui ne crée pas deux idées.
            local.update { it.copy(draft = "") }
            viewModelScope.launch { ideaRepository.add(state.draft, state.draftCategory) }
        }

        fun onFilterChange(value: IdeaCategory?) = local.update { it.copy(filter = value) }

        fun onToggleDone(
            id: Long,
            done: Boolean,
        ) {
            viewModelScope.launch { ideaRepository.setDone(id, done) }
        }

        fun onEdit(idea: Idea) = local.update { it.copy(editor = IdeaEditorState(idea.id, idea.text, idea.category)) }

        fun onEditorTextChange(value: String) = local.update { it.copy(editor = it.editor?.copy(text = value.take(MAX_TEXT_LENGTH))) }

        fun onEditorCategoryChange(value: IdeaCategory) = local.update { it.copy(editor = it.editor?.copy(category = value)) }

        fun onDismissEditor() = local.update { it.copy(editor = null) }

        fun onSaveEditor() {
            val editor = local.value.editor ?: return
            if (!editor.canSave) return
            viewModelScope.launch {
                ideaRepository.updateContent(editor.id, editor.text, editor.category)
                local.update { it.copy(editor = null) }
            }
        }

        /** Supprime tout de suite, et garde l'idée le temps du bandeau « Annuler ». */
        fun onDeleteEditor() {
            val editor = local.value.editor ?: return
            viewModelScope.launch {
                deleted = ideaRepository.observeAll().first().firstOrNull { it.id == editor.id }
                ideaRepository.delete(editor.id)
                local.update { it.copy(editor = null, undo = UndoNotice(R.string.ideas_deleted, editor.id)) }
            }
        }

        fun onUndo() {
            val idea = deleted ?: return
            deleted = null
            local.update { it.copy(undo = null) }
            viewModelScope.launch { ideaRepository.restore(idea) }
        }

        fun onUndoDismissed() {
            deleted = null
            local.update { it.copy(undo = null) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
            const val MAX_TEXT_LENGTH = 200
        }
    }
