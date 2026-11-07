package com.bastiensabos.betterhusband.feature.today

import com.bastiensabos.betterhusband.domain.model.UpcomingDate

/** État de l'écran « Flux du jour ». Immuable, produit uniquement par [TodayViewModel]. */
sealed interface TodayUiState {
    data object Loading : TodayUiState

    data class Ready(
        val attention: AttentionCard,
        /** Tâches à faire aujourd'hui, les plus en retard d'abord (trois au plus). */
        val tasks: List<TodayTask>,
        /** Tâches à faire qui ne tiennent pas dans [tasks]. */
        val moreTasks: Int,
        /** Date à préparer dans les 30 prochains jours, s'il y en a une. */
        val nextDate: UpcomingDate?,
        /** Jours d'affilée où l'attention a été faite (0 : pas de série en cours). */
        val streak: Int,
    ) : TodayUiState
}

data class AttentionCard(
    val text: String,
    val done: Boolean,
) {
    /** « Autre idée » n'est proposée que tant que l'attention n'est pas faite. */
    val canSwap: Boolean get() = !done
}

data class TodayTask(
    val id: Long,
    val title: String,
    val overdueDays: Long,
)
