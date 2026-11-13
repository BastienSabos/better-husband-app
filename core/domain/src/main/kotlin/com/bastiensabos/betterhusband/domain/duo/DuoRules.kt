package com.bastiensabos.betterhusband.domain.duo

import com.bastiensabos.betterhusband.domain.model.SharedRecord
import java.time.LocalDate

/** Pourquoi un bloc reçu est refusé. Un seul refus fait rejeter tout le lot, avant la moindre écriture. */
enum class DuoRejection {
    INVALID_UID,
    UNKNOWN_MEMBER,
    REVISION_OUT_OF_RANGE,
    TEXT_TOO_LONG,
    YEAR_OUT_OF_RANGE,
    INVALID_LINK,
    WISH_EDITED_BY_OTHER,
    DUPLICATE_UID,
    TOO_MANY_RECORDS,
}

/**
 * Bornes d'un bloc reçu de l'autre téléphone (`docs/duo.md`, « Format d'un bloc »). Le serveur ne peut rien lire,
 * mais un bloc reste une entrée non fiable : un téléphone modifié ou une version future boguée peut écrire n'importe
 * quoi avec la clé de couple.
 */
object DuoLimits {
    const val MAX_TEXT = 500
    const val MAX_LINK = 2_000
    const val MAX_RECORDS = 10_000
    const val MIN_YEAR = 1900
    const val MAX_YEAR = 2200

    /** Une révision « du futur » gagnerait tous les conflits : au-delà de 24 h d'avance sur notre montre, refus. */
    const val MAX_CLOCK_AHEAD_MILLIS = 24L * 60 * 60 * 1000

    private val UUID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

    /** Premier refus trouvé dans [batch], ou `null` si tout le lot peut s'appliquer. */
    fun check(
        batch: List<SharedRecord>,
        members: Set<String>,
        nowMillis: Long,
    ): DuoRejection? =
        when {
            batch.size > MAX_RECORDS -> DuoRejection.TOO_MANY_RECORDS
            batch.map { it.type to it.meta.uid }.toSet().size != batch.size -> DuoRejection.DUPLICATE_UID
            else -> batch.firstNotNullOfOrNull { check(it, members, nowMillis) }
        }

    /** Premier refus pour un seul enregistrement, ou `null`. */
    fun check(
        record: SharedRecord,
        members: Set<String>,
        nowMillis: Long,
    ): DuoRejection? {
        val meta = record.meta
        return when {
            !UUID.matches(meta.uid) -> DuoRejection.INVALID_UID
            meta.author !in members -> DuoRejection.UNKNOWN_MEMBER
            meta.revision !in 0..nowMillis + MAX_CLOCK_AHEAD_MILLIS -> DuoRejection.REVISION_OUT_OF_RANGE
            else -> checkContent(record, members)
        }
    }

    private fun checkContent(
        record: SharedRecord,
        members: Set<String>,
    ): DuoRejection? =
        when (record) {
            is SharedRecord.Wish -> checkWish(record)
            is SharedRecord.Task -> checkTask(record, members)
            is SharedRecord.Date -> checkDate(record)
        }

    private fun checkWish(wish: SharedRecord.Wish): DuoRejection? =
        when {
            wish.owner != wish.meta.author -> DuoRejection.WISH_EDITED_BY_OTHER
            wish.text.length > MAX_TEXT -> DuoRejection.TEXT_TOO_LONG
            wish.link != null && !isLink(wish.link) -> DuoRejection.INVALID_LINK
            else -> null
        }

    private fun checkTask(
        task: SharedRecord.Task,
        members: Set<String>,
    ): DuoRejection? =
        when {
            task.title.length > MAX_TEXT -> DuoRejection.TEXT_TOO_LONG
            listOfNotNull(task.assignee, task.lastDoneBy).any { it !in members } -> DuoRejection.UNKNOWN_MEMBER
            listOfNotNull(task.createdOn, task.lastDoneOn).any { !inYears(it) } -> DuoRejection.YEAR_OUT_OF_RANGE
            else -> null
        }

    private fun checkDate(date: SharedRecord.Date): DuoRejection? =
        when {
            date.title.length > MAX_TEXT -> DuoRejection.TEXT_TOO_LONG
            !inYears(date.date) -> DuoRejection.YEAR_OUT_OF_RANGE
            else -> null
        }

    private fun inYears(day: LocalDate) = day.year in MIN_YEAR..MAX_YEAR

    private fun isLink(link: String) = link.length <= MAX_LINK && link.startsWith("https://") && link.none { it.isWhitespace() }
}

/** Ce qui reste à envoyer : les écritures faites ici depuis le dernier envoi réussi. */
object DuoOutbox {
    /**
     * Enregistrements écrits sur ce téléphone à partir de [pushedUntil] (borne incluse : une écriture dans la même
     * milliseconde que l'envoi précédent repart au lieu de se perdre ; la fusion rend le doublon sans effet).
     */
    fun <T> pending(
        records: List<T>,
        pushedUntil: Long,
        localChangedAt: (T) -> Long,
    ): List<T> = records.filter { localChangedAt(it) >= pushedUntil }
}

/** Ce que garde un téléphone quand le duo se termine (`docs/duo.md`, « Sortie du duo »). */
data class ExitPlan(
    val keep: List<SharedRecord>,
    val drop: List<SharedRecord>,
)

object DuoExit {
    /**
     * Chacun garde les tâches et les dates (copiées en solo, sans responsable) ; toutes les envies et les marqueurs de
     * suppression disparaissent. Les envies ne servent qu'en duo et portent l'identifiant de membre de ce duo : les
     * garder ferait des données invisibles en solo (`docs/duo.md`). Le carnet, la fiche et les attentions ne sont pas
     * concernés : ils n'ont jamais été partagés.
     */
    fun plan(records: List<SharedRecord>): ExitPlan {
        val (keep, drop) = records.partition { record -> !record.meta.deleted && record !is SharedRecord.Wish }
        return ExitPlan(keep = keep.map(::toSolo), drop = drop)
    }

    private fun toSolo(record: SharedRecord): SharedRecord =
        when (record) {
            is SharedRecord.Task -> record.copy(assignee = null, lastDoneBy = null)
            is SharedRecord.Wish, is SharedRecord.Date -> record
        }
}
