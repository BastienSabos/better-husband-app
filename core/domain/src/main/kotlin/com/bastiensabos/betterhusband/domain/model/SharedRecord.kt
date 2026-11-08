package com.bastiensabos.betterhusband.domain.model

import java.time.LocalDate

/**
 * Métadonnées de synchro d'un enregistrement partagé du mode duo (ADR 0004, `docs/duo.md`).
 *
 * [revision] est l'instant de la dernière modification sur l'horloge du téléphone qui l'a faite, jamais en recul
 * ([com.bastiensabos.betterhusband.domain.duo.Revision]). [author] est l'identifiant de membre du duo qui l'a écrite.
 * [deleted] est un marqueur de suppression : un élément partagé n'est pas effacé avant d'être envoyé.
 */
data class SyncMeta(
    val uid: String,
    val revision: Long,
    val author: String,
    val deleted: Boolean = false,
)

/** Type d'un bloc du duo. Rien d'autre ne passe : un type inconnu est refusé à la lecture. */
enum class SharedType {
    WISH,
    TASK,
    DATE,
}

/** Ce qui circule entre les deux téléphones d'un duo. Le carnet, la fiche et les attentions n'ont pas de type ici. */
sealed interface SharedRecord {
    val meta: SyncMeta
    val type: SharedType

    /** Une envie notée par son auteur ([owner]), visible par l'autre en lecture. */
    data class Wish(
        override val meta: SyncMeta,
        val owner: String,
        val text: String,
        val link: String? = null,
    ) : SharedRecord {
        override val type = SharedType.WISH
    }

    /** Une tâche du foyer partagée ; [assignee] et [lastDoneBy] sont des identifiants de membre, ou `null`. */
    data class Task(
        override val meta: SyncMeta,
        val title: String,
        val repeat: TaskRepeat,
        val createdOn: LocalDate,
        val lastDoneOn: LocalDate? = null,
        val lastDoneBy: String? = null,
        val assignee: String? = null,
    ) : SharedRecord {
        override val type = SharedType.TASK
    }

    /** Une date commune (anniversaire de couple, date d'un enfant…). */
    data class Date(
        override val meta: SyncMeta,
        val title: String,
        val kind: DateKind,
        val date: LocalDate,
        val recurring: Boolean,
    ) : SharedRecord {
        override val type = SharedType.DATE
    }
}
