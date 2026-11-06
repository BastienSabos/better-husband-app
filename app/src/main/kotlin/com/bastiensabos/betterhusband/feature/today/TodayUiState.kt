package com.bastiensabos.betterhusband.feature.today

/** État de l'écran « Flux du jour ». Immuable, produit uniquement par [TodayViewModel]. */
sealed interface TodayUiState {
    data object Loading : TodayUiState

    data object Empty : TodayUiState

    data class Ready(
        val cards: List<TodayCard>,
    ) : TodayUiState
}

data class TodayCard(
    val id: String,
    val title: String,
    val done: Boolean = false,
)
