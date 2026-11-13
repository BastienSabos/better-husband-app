package com.bastiensabos.betterhusband.feature.dates

import com.bastiensabos.betterhusband.domain.DateGroups
import com.bastiensabos.betterhusband.domain.MonthGroup
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import com.bastiensabos.betterhusband.ui.component.UndoNotice
import java.time.LocalDate

sealed interface DatesListState {
    data object Loading : DatesListState

    data object Empty : DatesListState

    /** [items] triées par échéance, jamais vide : la première est mise en avant, les autres sont rangées par mois. */
    data class Ready(
        val items: List<UpcomingDate>,
        /** Idées du carnet qui peuvent servir pour la prochaine date. */
        val nextIdeas: Int = 0,
    ) : DatesListState {
        val next: UpcomingDate get() = items.first()
        val months: List<MonthGroup> get() = DateGroups.byMonth(items.drop(1))
    }
}

/** Formulaire d'ajout (editingId = null) ou de modification d'une date saisie à la main. */
data class DateEditorState(
    val editingId: Long? = null,
    val title: String = "",
    val kind: DateKind = DateKind.BIRTHDAY,
    val date: LocalDate? = null,
    val recurring: Boolean = true,
    /** Partagée avec l'autre membre du duo ; ne compte qu'en duo. */
    val shared: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank() && date != null
}

data class DatesUiState(
    val list: DatesListState = DatesListState.Loading,
    val editor: DateEditorState? = null,
    /** Date qui vient d'être supprimée, encore récupérable. */
    val undo: UndoNotice? = null,
    /** Présent en duo : l'éditeur propose alors de partager la date. */
    val duo: DatesDuo? = null,
)

/** Le duo vu des dates : le prénom de l'autre vient de la fiche partenaire locale, s'il est connu. */
data class DatesDuo(
    val partnerName: String?,
)
