package com.bastiensabos.betterhusband.domain.model

import java.time.LocalDate

/**
 * L'attention proposée un jour donné. [attentionId] désigne une entrée de la banque d'attentions : les identifiants
 * sont stables (la banque ne fait que s'allonger), car l'historique les garde. [done] : l'utilisateur l'a faite.
 */
data class AttentionLogEntry(
    val date: LocalDate,
    val attentionId: Int,
    val done: Boolean = false,
)
