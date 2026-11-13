package com.bastiensabos.betterhusband.domain.model

import java.time.LocalDate

enum class DateKind {
    BIRTHDAY,
    ANNIVERSARY,
    OTHER,
}

/**
 * Date qui compte, saisie par l'utilisateur. Si [recurring], elle revient chaque année (anniversaire, fête) ;
 * sinon c'est un événement ponctuel (rendez-vous, voyage). En duo, [shared] la montre aussi à l'autre (`docs/duo.md`).
 */
data class ImportantDate(
    val id: Long = 0,
    val title: String,
    val kind: DateKind,
    val date: LocalDate,
    val recurring: Boolean,
    val shared: Boolean = false,
)

/** D'où vient une échéance affichée. Le domaine ne porte aucun texte : l'UI traduit chaque libellé. */
sealed interface DateLabel {
    data class PartnerBirthday(
        val name: String,
    ) : DateLabel

    data object RelationshipStart : DateLabel

    data class RelativeBirthday(
        val name: String,
    ) : DateLabel

    data class Custom(
        val title: String,
    ) : DateLabel
}

/**
 * Prochaine occurrence d'une date. [key] est stable (utile pour les clés de liste et les rappels) ;
 * [yearsSince] est le nombre d'années depuis la date d'origine, `null` pour un événement ponctuel.
 */
data class UpcomingDate(
    val key: String,
    val label: DateLabel,
    val kind: DateKind,
    val date: LocalDate,
    val daysUntil: Long,
    val yearsSince: Int?,
    val editableId: Long?,
    /** Date partagée avec l'autre membre du duo. */
    val shared: Boolean = false,
)
