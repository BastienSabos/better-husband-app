package com.bastiensabos.betterhusband.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.data.repository.DuoRepository
import com.bastiensabos.betterhusband.data.repository.HouseholdTaskRepository
import com.bastiensabos.betterhusband.data.repository.PartnerRepository
import com.bastiensabos.betterhusband.domain.TaskMath
import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.bastiensabos.betterhusband.ui.component.UndoNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Tâches du foyer : ajout rapide en haut, liste triée (à faire d'abord) dessous, édition en boîte de dialogue. */
@HiltViewModel
class TasksViewModel
    @Inject
    constructor(
        private val repository: HouseholdTaskRepository,
        private val clock: Clock,
        duoRepository: DuoRepository,
        partners: PartnerRepository,
    ) : ViewModel() {
        private val duo =
            combine(duoRepository.observeSession(), partners.observePartner()) { session, partner ->
                session?.let { TasksDuo(it.memberId, it.partnerMemberId, partner?.firstName) }
            }

        private val local = MutableStateFlow(TasksUiState())

        /** La dernière tâche supprimée, tant que « Annuler » est proposé. */
        private var deleted: HouseholdTask? = null

        val uiState: StateFlow<TasksUiState> =
            combine(repository.observeAll(), local, duo) { tasks, state, duo ->
                val today = LocalDate.now(clock)
                val list =
                    if (tasks.isEmpty()) {
                        TasksListState.Empty
                    } else {
                        TasksListState.Ready(
                            TaskMath.sorted(tasks, today).map { task ->
                                TaskItem(
                                    task = task,
                                    done = !TaskMath.isDue(task, today),
                                    daysUntilDue = -TaskMath.daysOverdue(task, today),
                                )
                            },
                        )
                    }
                state.copy(list = list, duo = duo)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TasksUiState())

        fun onDraftChange(value: String) = local.update { it.copy(draft = value.take(MAX_TITLE_LENGTH)) }

        fun onDraftRepeatChange(value: TaskRepeat) = local.update { it.copy(draftRepeat = value) }

        fun onAdd() {
            val state = local.value
            if (!state.canAdd) return
            // Le champ est vidé tout de suite : un double appui ne crée pas deux tâches.
            local.update { it.copy(draft = "") }
            val task = HouseholdTask(title = state.draft, repeat = state.draftRepeat, createdOn = LocalDate.now(clock))
            viewModelScope.launch { repository.save(task) }
        }

        /** Cocher marque la tâche faite aujourd'hui ; décocher la remet à faire. */
        fun onToggleDone(
            id: Long,
            done: Boolean,
        ) {
            viewModelScope.launch { repository.setLastDone(id, if (done) LocalDate.now(clock) else null) }
        }

        fun onEdit(task: HouseholdTask) = local.update { it.copy(editor = TaskEditorState(task, task.title, task.repeat)) }

        fun onEditorTitleChange(value: String) = local.update { it.copy(editor = it.editor?.copy(title = value.take(MAX_TITLE_LENGTH))) }

        fun onEditorRepeatChange(value: TaskRepeat) = local.update { it.copy(editor = it.editor?.copy(repeat = value)) }

        /** `null` : l'un ou l'autre. */
        fun onEditorAssigneeChange(value: String?) = local.update { it.copy(editor = it.editor?.copy(assignee = value)) }

        fun onDismissEditor() = local.update { it.copy(editor = null) }

        fun onSaveEditor() {
            val editor = local.value.editor ?: return
            if (!editor.canSave) return
            viewModelScope.launch {
                repository.save(editor.task.copy(title = editor.title, repeat = editor.repeat, assignee = editor.assignee))
                local.update { it.copy(editor = null) }
            }
        }

        /** Supprime tout de suite, et garde la tâche (rythme, dernière fois faite) le temps du bandeau « Annuler ». */
        fun onDeleteEditor() {
            val editor = local.value.editor ?: return
            deleted = editor.task
            viewModelScope.launch {
                repository.delete(editor.task.id)
                local.update { it.copy(editor = null, undo = UndoNotice(R.string.tasks_deleted, editor.task.id)) }
            }
        }

        fun onUndo() {
            val task = deleted ?: return
            deleted = null
            local.update { it.copy(undo = null) }
            viewModelScope.launch { repository.save(task) }
        }

        fun onUndoDismissed() {
            deleted = null
            local.update { it.copy(undo = null) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
            const val MAX_TITLE_LENGTH = 100
        }
    }
