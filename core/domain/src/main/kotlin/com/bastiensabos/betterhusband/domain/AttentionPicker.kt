package com.bastiensabos.betterhusband.domain

import java.time.LocalDate

/** Choisit l'attention du jour dans la banque, sans répéter les dernières. Pur et déterministe. */
object AttentionPicker {
    /** Nombre de jours pendant lesquels une attention ne revient pas (sauf si la banque est plus petite). */
    const val NO_REPEAT_DAYS = 30

    /**
     * Identifiant (de 0 à [bankSize] - 1) de l'attention d'[date]. Le même jour donne toujours le même résultat tant
     * que [recentIds] ne change pas ; [skip] écarte en plus des identifiants (« Autre idée »).
     */
    fun pick(
        date: LocalDate,
        bankSize: Int,
        recentIds: Collection<Int>,
        skip: Collection<Int> = emptyList(),
    ): Int {
        require(bankSize > 0) { "La banque d'attentions est vide" }
        val start = Math.floorMod(date.toEpochDay() * MULTIPLIER, bankSize.toLong()).toInt()
        val excluded = recentIds.toSet() + skip.toSet()
        // Si tout est exclu (petite banque, beaucoup d'« autres idées »), on accepte une répétition plutôt que rien.
        return (0 until bankSize).map { (start + it) % bankSize }.firstOrNull { it !in excluded } ?: start
    }

    // Brasse les jours consécutifs pour que deux jours de suite ne proposent pas deux idées voisines de la banque.
    private const val MULTIPLIER = 7_919L
}
