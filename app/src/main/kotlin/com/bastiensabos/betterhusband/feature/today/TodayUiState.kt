package com.bastiensabos.betterhusband.feature.today

import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import java.time.LocalDate

/** État de l'écran « Flux du jour ». Immuable, produit uniquement par [TodayViewModel]. */
sealed interface TodayUiState {
    data object Loading : TodayUiState

    data class Ready(
        /** Le jour affiché, pour l'en-tête. */
        val date: LocalDate,
        val attention: AttentionCard,
        /** Tâches à faire aujourd'hui, les plus en retard d'abord (trois au plus). */
        val tasks: List<TodayTask>,
        /** Tâches à faire qui ne tiennent pas dans [tasks]. */
        val moreTasks: Int,
        /** Date à préparer dans les 30 prochains jours, s'il y en a une. */
        val nextDate: UpcomingDate?,
        /** Une ou deux idées du carnet pour préparer [nextDate]. */
        val nextDateIdeas: List<String> = emptyList(),
        /** Jours d'affilée où l'attention a été faite (0 : pas de série en cours). */
        val streak: Int,
        /** Semaine en cours, du lundi au dimanche : `true` les jours où l'attention a été faite. */
        val week: List<Boolean> = List(DAYS_IN_WEEK) { false },
        /** Aucune réponse dans « Notre couple » : on invite à le remplir, une carte qui disparaît dès la première. */
        val suggestCoupleProfile: Boolean = false,
        /** Prénom du ou de la partenaire, pour l'avatar qui ouvre la fiche. */
        val partnerName: String? = null,
    ) : TodayUiState
}

const val DAYS_IN_WEEK = 7

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
