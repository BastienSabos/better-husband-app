package com.bastiensabos.betterhusband.domain

import java.time.LocalDate

/** Choisit l'attention du jour parmi celles qui conviennent, sans répéter les dernières. Pur et déterministe. */
object AttentionPicker {
    /** Nombre de jours pendant lesquels une attention ne revient pas (sauf si le choix est plus petit). */
    const val NO_REPEAT_DAYS = 30

    /**
     * Identifiant, pris dans [eligibleIds], de l'attention d'[date]. Le même jour donne toujours le même résultat tant
     * que [eligibleIds] et [recentIds] ne changent pas ; [skip] écarte en plus des identifiants (« Autre idée »).
     */
    fun pick(
        date: LocalDate,
        eligibleIds: List<Int>,
        recentIds: Collection<Int>,
        skip: Collection<Int> = emptyList(),
    ): Int {
        require(eligibleIds.isNotEmpty()) { "Aucune attention ne convient" }
        val size = eligibleIds.size
        val start = Math.floorMod(date.toEpochDay() * MULTIPLIER, size.toLong()).toInt()
        val excluded = recentIds.toSet() + skip.toSet()
        // Si tout est exclu (petit choix, beaucoup d'« autres idées »), on accepte une répétition plutôt que rien.
        val index = (0 until size).map { (start + it) % size }.firstOrNull { eligibleIds[it] !in excluded } ?: start
        return eligibleIds[index]
    }

    // Brasse les jours consécutifs pour que deux jours de suite ne proposent pas deux idées voisines de la banque.
    private const val MULTIPLIER = 7_919L
}
