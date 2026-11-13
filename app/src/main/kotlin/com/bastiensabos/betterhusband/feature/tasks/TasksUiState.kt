package com.bastiensabos.betterhusband.feature.tasks

import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.bastiensabos.betterhusband.ui.component.UndoNotice

/**
 * Une tâche prête à afficher. [daysUntilDue] est négatif quand la tâche est en retard, 0 le jour même ;
 * [done] vaut `true` quand elle n'est pas à refaire tout de suite (elle a été faite dans son rythme).
 */
data class TaskItem(
    val task: HouseholdTask,
    val done: Boolean,
    val daysUntilDue: Long,
)

sealed interface TasksListState {
    data object Loading : TasksListState

    /** Aucune tâche du tout : on explique à quoi servent les tâches du foyer. */
    data object Empty : TasksListState

    data class Ready(
        val items: List<TaskItem>,
    ) : TasksListState
}

data class TaskEditorState(
    val task: HouseholdTask,
    val title: String,
    val repeat: TaskRepeat,
    val assignee: String? = task.assignee,
) {
    val canSave: Boolean get() = title.isNotBlank()
}

/** En duo : qui je suis, qui est l'autre et son prénom (pris dans la fiche partenaire locale, jamais synchronisé). */
data class TasksDuo(
    val me: String,
    val partner: String?,
    val partnerName: String?,
)

data class TasksUiState(
    val list: TasksListState = TasksListState.Loading,
    val draft: String = "",
    /** Rythme de la prochaine tâche ; il reste celui de la dernière saisie pour enchaîner vite. */
    val draftRepeat: TaskRepeat = TaskRepeat.WEEKLY,
    val editor: TaskEditorState? = null,
    /** Tâche qui vient d'être supprimée, encore récupérable. */
    val undo: UndoNotice? = null,
    /** `null` en solo : rien sur « qui fait quoi » n'est affiché. */
    val duo: TasksDuo? = null,
) {
    val canAdd: Boolean get() = draft.isNotBlank()
}
