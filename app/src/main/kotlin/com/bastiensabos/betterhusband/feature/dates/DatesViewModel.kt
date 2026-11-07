package com.bastiensabos.betterhusband.feature.dates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.data.repository.ImportantDateRepository
import com.bastiensabos.betterhusband.domain.ObserveUpcomingDatesUseCase
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import com.bastiensabos.betterhusband.ui.component.UndoNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Liste des prochaines dates et formulaire d'édition. Le flux s'arrête 5 s après le passage en arrière-plan puis
 * repart au retour : « aujourd'hui » est ainsi recalculé après minuit.
 */
@HiltViewModel
class DatesViewModel
    @Inject
    constructor(
        observeUpcomingDates: ObserveUpcomingDatesUseCase,
        private val importantDateRepository: ImportantDateRepository,
    ) : ViewModel() {
        private val editor = MutableStateFlow<DateEditorState?>(null)
        private val undo = MutableStateFlow<UndoNotice?>(null)

        /** La dernière date supprimée, tant que « Annuler » est proposé. */
        private var deleted: ImportantDate? = null

        val uiState: StateFlow<DatesUiState> =
            combine(
                observeUpcomingDates().map { if (it.isEmpty()) DatesListState.Empty else DatesListState.Ready(it) },
                editor,
                undo,
            ) { list, editorState, undoNotice -> DatesUiState(list, editorState, undoNotice) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DatesUiState())

        fun onAddClick() {
            editor.value = DateEditorState()
        }

        fun onEdit(id: Long) {
            viewModelScope.launch {
                val date = importantDateRepository.observeAll().first().firstOrNull { it.id == id } ?: return@launch
                editor.value = DateEditorState(date.id, date.title, date.kind, date.date, date.recurring)
            }
        }

        fun onTitleChange(value: String) = editor.update { it?.copy(title = value.take(MAX_TITLE_LENGTH)) }

        fun onKindChange(value: DateKind) = editor.update { it?.copy(kind = value) }

        fun onDateChange(value: LocalDate?) = editor.update { it?.copy(date = value) }

        fun onRecurringChange(value: Boolean) = editor.update { it?.copy(recurring = value) }

        fun onDismissEditor() {
            editor.value = null
        }

        fun onSave() {
            val state = editor.value ?: return
            val date = state.date ?: return
            if (!state.canSave) return
            viewModelScope.launch {
                importantDateRepository.save(
                    ImportantDate(
                        id = state.editingId ?: 0,
                        title = state.title,
                        kind = state.kind,
                        date = date,
                        recurring = state.recurring,
                    ),
                )
                editor.value = null
            }
        }

        /** Supprime tout de suite (rappels compris), et garde la date le temps du bandeau « Annuler ». */
        fun onDelete() {
            val id = editor.value?.editingId ?: return
            viewModelScope.launch {
                deleted = importantDateRepository.observeAll().first().firstOrNull { it.id == id }
                importantDateRepository.delete(id)
                editor.value = null
                undo.value = UndoNotice(R.string.dates_deleted, id)
            }
        }

        fun onUndo() {
            val date = deleted ?: return
            deleted = null
            undo.value = null
            viewModelScope.launch { importantDateRepository.save(date) }
        }

        fun onUndoDismissed() {
            deleted = null
            undo.value = null
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
            const val MAX_TITLE_LENGTH = 60
        }
    }
