package com.bastiensabos.betterhusband.feature.ideas

import com.bastiensabos.betterhusband.domain.model.Idea
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import com.bastiensabos.betterhusband.ui.component.UndoNotice

sealed interface IdeasListState {
    data object Loading : IdeasListState

    /** Aucune idée du tout : on explique à quoi sert le carnet. */
    data object Empty : IdeasListState

    /** [items] peut être vide quand un filtre ne retient rien. */
    data class Ready(
        val items: List<Idea>,
    ) : IdeasListState
}

data class IdeaEditorState(
    val id: Long,
    val text: String,
    val category: IdeaCategory,
) {
    val canSave: Boolean get() = text.isNotBlank()
}

data class IdeasUiState(
    val list: IdeasListState = IdeasListState.Loading,
    /** `null` : toutes les catégories. */
    val filter: IdeaCategory? = null,
    val draft: String = "",
    /** Catégorie de la prochaine idée ; elle reste celle de la dernière saisie pour enchaîner vite. */
    val draftCategory: IdeaCategory = IdeaCategory.GIFT,
    val editor: IdeaEditorState? = null,
    /** Idée qui vient d'être supprimée, encore récupérable. */
    val undo: UndoNotice? = null,
    /** Un duo est rejoint : l'onglet propose les envies. */
    val wishesAvailable: Boolean = false,
) {
    val canAdd: Boolean get() = draft.isNotBlank()
}
