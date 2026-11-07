package com.bastiensabos.betterhusband.feature.dates

import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import com.bastiensabos.betterhusband.ui.component.UndoNotice
import java.time.LocalDate

sealed interface DatesListState {
    data object Loading : DatesListState

    data object Empty : DatesListState

    data class Ready(
        val items: List<UpcomingDate>,
    ) : DatesListState
}

/** Formulaire d'ajout (editingId = null) ou de modification d'une date saisie à la main. */
data class DateEditorState(
    val editingId: Long? = null,
    val title: String = "",
    val kind: DateKind = DateKind.BIRTHDAY,
    val date: LocalDate? = null,
    val recurring: Boolean = true,
) {
    val canSave: Boolean get() = title.isNotBlank() && date != null
}

data class DatesUiState(
    val list: DatesListState = DatesListState.Loading,
    val editor: DateEditorState? = null,
    /** Date qui vient d'être supprimée, encore récupérable. */
    val undo: UndoNotice? = null,
)
