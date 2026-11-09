package com.bastiensabos.betterhusband.domain.duo

import com.bastiensabos.betterhusband.domain.model.SharedRecord
import com.bastiensabos.betterhusband.domain.model.SharedType

/** Un bloc lu sur le serveur. [serverMillis] est l'horodatage du serveur, qui ordonne la lecture incrémentale. */
class RemoteBlock(
    val blobId: String,
    val sealed: ByteArray,
    val serverMillis: Long,
)

/** Un bloc à écrire sur le serveur : le dernier remplace l'ancien de même [blobId]. */
class OutgoingBlock(
    val blobId: String,
    val sealed: ByteArray,
)

/** Le duo n'existe plus sur le serveur : l'autre l'a quitté. */
class DuoEndedException : Exception("Le duo est terminé")

/** Le serveur, vu du domaine. L'application le branche sur Firestore (ADR 0004) ; les appels sont bloquants. */
interface DuoMailbox {
    /** Blocs écrits après [afterMillis] (heure du serveur), et la plus grande heure vue. Lève [DuoEndedException]. */
    fun pull(afterMillis: Long): List<RemoteBlock>

    fun push(blocks: List<OutgoingBlock>)
}

/** Un enregistrement partagé avec l'instant de son écriture locale, pour savoir ce qui reste à envoyer. */
class LocalRecord(
    val record: SharedRecord,
    val localChangedAt: Long,
)

/** Où vivent les enregistrements partagés : la base locale. */
interface DuoStore {
    /** Membres du duo (moi et l'autre), mon identifiant et l'état de la synchro. */
    fun state(): DuoState

    fun find(
        type: SharedType,
        uid: String,
    ): SharedRecord?

    /**
     * Applique [records] et retient [lastSeen], en une seule transaction. Ces écritures ne comptent **pas** comme des
     * changements locaux : elles ne repartent pas vers le serveur.
     */
    fun apply(
        records: List<SharedRecord>,
        lastSeen: Long,
    )

    /** Enregistrements écrits ici à partir de [since] (borne incluse), suppressions comprises. */
    fun pending(since: Long): List<LocalRecord>

    /** Tout ce qui a été écrit avant [until] est parti : le prochain [pending] reprend à [until]. */
    fun markPushed(until: Long)

    fun markEnded()
}

data class DuoState(
    val duoId: String,
    val me: String,
    val members: Set<String>,
    val lastSeen: Long,
    val pushedUntil: Long,
)

sealed interface SyncResult {
    data class Done(
        val applied: Int,
        val pushed: Int,
        val unreadable: Int,
    ) : SyncResult

    /** Un bloc reçu sort des bornes : tout le lot est refusé, rien n'est écrit. */
    data class Rejected(
        val reason: DuoRejection,
    ) : SyncResult

    data object Ended : SyncResult
}

/**
 * Une synchro : on lit ce que l'autre a écrit, on le fusionne, puis on envoie ce qui a changé ici. Tout ce qui
 * arrive est une entrée non fiable : déchiffré, décodé, vérifié par [DuoLimits], puis appliqué tout ou rien.
 */
class DuoSyncEngine(
    private val keys: CoupleKeys,
    private val mailbox: DuoMailbox,
    private val store: DuoStore,
    private val nowMillis: () -> Long,
) {
    fun sync(): SyncResult {
        // Repère pris avant de lire quoi que ce soit : une écriture faite pendant la synchro porte un instant au moins
        // égal, donc repart à la suivante. Au pire, ce qui a été écrit dans la même milliseconde repart une fois.
        val startedAt = nowMillis()
        val state = store.state()
        val incoming = pullOrNull(state) ?: return SyncResult.Ended
        val read = read(incoming, state.duoId)
        val rejection = DuoLimits.check(read.records, state.members, nowMillis())
        return if (rejection != null) SyncResult.Rejected(rejection) else applyAndPush(state, incoming, read, startedAt)
    }

    /** Les blocs écrits depuis la dernière lecture (avec marge), ou `null` si le duo n'existe plus. */
    private fun pullOrNull(state: DuoState): List<RemoteBlock>? =
        try {
            mailbox.pull((state.lastSeen - PULL_MARGIN_MILLIS).coerceAtLeast(0))
        } catch (ignored: DuoEndedException) {
            store.markEnded()
            null
        }

    private fun applyAndPush(
        state: DuoState,
        incoming: List<RemoteBlock>,
        read: Read,
        startedAt: Long,
    ): SyncResult {
        val winners = read.records.filter { DuoMerge.incomingWins(store.find(it.type, it.meta.uid), it) }
        val newest = incoming.maxOfOrNull { it.serverMillis } ?: state.lastSeen
        store.apply(winners, maxOf(newest, state.lastSeen))

        val pending = store.pending(state.pushedUntil)
        if (pending.isNotEmpty()) mailbox.push(pending.map { seal(it.record, state.duoId) })
        store.markPushed(startedAt)
        return SyncResult.Done(applied = winners.size, pushed = pending.size, unreadable = read.unreadable)
    }

    private class Read(
        val records: List<SharedRecord>,
        val unreadable: Int,
    )

    /** Un bloc illisible (autre clé, octet modifié, version inconnue) est ignoré ; les autres sont gardés. */
    private fun read(
        incoming: List<RemoteBlock>,
        duoId: String,
    ): Read {
        var unreadable = 0
        val records = mutableListOf<SharedRecord>()
        for (block in incoming) {
            try {
                val record = DuoCodec.decode(keys.open(block.sealed, duoId, block.blobId))
                // Le bloc doit porter l'identifiant que sa propre clé en déduit : pas de bloc déplacé ni rejoué.
                if (keys.blobId(record.type, record.meta.uid) == block.blobId) records += record else unreadable++
            } catch (ignored: UnreadableBlockException) {
                unreadable++
            }
        }
        return Read(records, unreadable)
    }

    private fun seal(
        record: SharedRecord,
        duoId: String,
    ): OutgoingBlock {
        val blobId = keys.blobId(record.type, record.meta.uid)
        return OutgoingBlock(blobId, keys.seal(DuoCodec.encode(record), duoId, blobId))
    }

    companion object {
        /** Marge de relecture : une écriture validée par le serveur dans le désordre n'est pas manquée (fusion idempotente). */
        const val PULL_MARGIN_MILLIS = 5L * 60 * 1000
    }
}
