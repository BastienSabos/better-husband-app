package com.bastiensabos.betterhusband.domain.duo

import com.bastiensabos.betterhusband.domain.model.SharedRecord
import com.bastiensabos.betterhusband.domain.model.SyncMeta

/** Révisions des enregistrements partagés : un instant en millisecondes qui ne recule jamais. */
object Revision {
    /**
     * Révision d'une modification faite maintenant. Elle dépasse toujours la révision déjà enregistrée, même si
     * l'horloge a reculé : une modification de l'utilisateur ne peut pas perdre en silence contre l'ancienne version.
     */
    fun next(
        nowMillis: Long,
        previous: Long?,
    ): Long = if (previous == null) nowMillis else maxOf(nowMillis, previous + 1)
}

/**
 * Fusion « la dernière modification gagne », enregistrement par enregistrement (règle reprise du budget planner,
 * ADR 0003 du dépôt frère). La fusion est commutative et idempotente : recevoir deux fois le même bloc, ou deux blocs
 * dans le désordre, donne le même état sur les deux téléphones.
 */
object DuoMerge {
    /** Vrai si [incoming] doit remplacer [local]. Égalité de révision : le plus grand auteur l'emporte, puis le contenu. */
    fun incomingWins(
        local: SharedRecord?,
        incoming: SharedRecord,
    ): Boolean {
        if (local == null) return true
        require(local.meta.uid == incoming.meta.uid) { "Fusion de deux enregistrements différents" }
        return compare(incoming, local) > 0
    }

    /** L'enregistrement qui reste après fusion. */
    fun merge(
        local: SharedRecord?,
        incoming: SharedRecord,
    ): SharedRecord = if (local != null && !incomingWins(local, incoming)) local else incoming

    private fun compare(
        a: SharedRecord,
        b: SharedRecord,
    ): Int =
        compareValuesBy(a.meta, b.meta, SyncMeta::revision, SyncMeta::author).takeIf { it != 0 }
            // Même révision et même auteur mais contenu différent (cas pathologique) : un ordre total reste
            // nécessaire pour que les deux téléphones convergent. Une suppression l'emporte, puis le texte.
            ?: compareValuesBy(a, b, { it.meta.deleted }, { it.toString() })
}
