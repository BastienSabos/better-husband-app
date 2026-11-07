package com.bastiensabos.betterhusband.domain.model

import java.time.LocalDate

/** Rythme d'une tâche du foyer. Le domaine ne porte aucun texte : l'UI traduit chaque valeur. */
enum class TaskRepeat {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
}

/**
 * Tâche récurrente du foyer. [lastDoneOn] est `null` tant qu'elle n'a jamais été faite : elle est alors à faire dès
 * [createdOn].
 */
data class HouseholdTask(
    val id: Long = 0,
    val title: String,
    val repeat: TaskRepeat,
    val createdOn: LocalDate,
    val lastDoneOn: LocalDate? = null,
)
