package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.DateLabel
import com.bastiensabos.betterhusband.domain.model.KidsAtHome
import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import java.time.temporal.ChronoUnit

/**
 * Temps fort de l'année : une date saisie (anniversaires, rencontre, mariage) ou une fête du calendrier. Les jours de
 * [prepareDays] avant la date, puis le jour même, l'attention du jour vient de [prepare] ou de [day] plutôt que du
 * tirage habituel : assez pour préparer sans en parler tous les jours.
 */
enum class Moment(
    val prepare: AttentionGroup,
    val day: AttentionGroup,
    val prepareDays: Set<Long>,
    /** Jour fixe des fêtes du calendrier ; `null` pour un temps fort qui vient d'une date saisie. */
    val calendarDay: MonthDay? = null,
) {
    PARTNER_BIRTHDAY(AttentionGroup.PARTNER_BIRTHDAY_PREPARE, AttentionGroup.PARTNER_BIRTHDAY_DAY, setOf(21, 14, 7, 3, 1)),

    /** Rencontre, mariage, pacs : toute date saisie comme anniversaire de couple. */
    ANNIVERSARY(AttentionGroup.ANNIVERSARY_PREPARE, AttentionGroup.ANNIVERSARY_DAY, setOf(14, 7, 3, 1)),

    /** Anniversaire d'un proche : enfant, parent, ami. Souvent une charge à partager. */
    RELATIVE_BIRTHDAY(AttentionGroup.RELATIVE_BIRTHDAY_PREPARE, AttentionGroup.RELATIVE_BIRTHDAY_DAY, setOf(10, 3)),
    VALENTINES(
        AttentionGroup.VALENTINES_PREPARE,
        AttentionGroup.VALENTINES_DAY,
        setOf(10, 5, 1),
        MonthDay.of(Month.FEBRUARY, 14),
    ),
    CHRISTMAS(
        AttentionGroup.CHRISTMAS_PREPARE,
        AttentionGroup.CHRISTMAS_DAY,
        setOf(35, 28, 21, 14, 7, 3, 1),
        MonthDay.of(Month.DECEMBER, 25),
    ),

    /** Le 31 décembre : réveillon et bilan de l'année. */
    NEW_YEAR(AttentionGroup.NEW_YEAR_PREPARE, AttentionGroup.NEW_YEAR_DAY, setOf(4), MonthDay.of(Month.DECEMBER, 31)),

    /** Rentrée des classes, autour du 1er septembre : seulement avec des enfants à la maison. */
    BACK_TO_SCHOOL(
        AttentionGroup.BACK_TO_SCHOOL_PREPARE,
        AttentionGroup.BACK_TO_SCHOOL_DAY,
        setOf(21, 10, 3),
        MonthDay.of(Month.SEPTEMBER, 1),
    ),
}

/** Quels temps forts commandent l'attention d'un jour. Pur : tout vient des dates déjà calculées. */
object MomentCalendar {
    /**
     * Groupes prioritaires pour [today], à partir des prochaines dates ([upcoming], aujourd'hui compris) et des fêtes
     * du calendrier. Vide les jours ordinaires. La rentrée ne compte que pour qui a dit avoir des enfants à la maison.
     */
    fun priorityGroups(
        upcoming: List<UpcomingDate>,
        kids: KidsAtHome?,
        today: LocalDate,
    ): Set<AttentionGroup> {
        val fromDates = upcoming.mapNotNull { date -> date.moment()?.let { it to date.daysUntil } }
        val fromCalendar =
            Moment.entries
                .filter { it != Moment.BACK_TO_SCHOOL || (kids != null && kids != KidsAtHome.NONE) }
                .mapNotNull { moment -> moment.calendarDay?.let { moment to daysUntil(it, today) } }
        return (fromDates + fromCalendar)
            .mapNotNull { (moment, days) ->
                when (days) {
                    0L -> moment.day
                    in moment.prepareDays -> moment.prepare
                    else -> null
                }
            }.toSet()
    }

    /** Identifiants écrits pour [groups], dans l'ordre des groupes : vide si aucun texte n'existe encore. */
    fun priorityIds(
        groupSizes: Map<AttentionGroup, Int>,
        groups: Set<AttentionGroup>,
    ): List<Int> =
        AttentionGroup.entries
            .filter { it in groups }
            .flatMap { group -> (0 until (groupSizes[group] ?: 0)).map { group.offset + it } }

    private fun UpcomingDate.moment(): Moment? =
        when (label) {
            is DateLabel.PartnerBirthday -> {
                Moment.PARTNER_BIRTHDAY
            }

            DateLabel.RelationshipStart -> {
                Moment.ANNIVERSARY
            }

            is DateLabel.RelativeBirthday -> {
                Moment.RELATIVE_BIRTHDAY
            }

            // Une date saisie à la main n'est un temps fort que si elle revient chaque année.
            is DateLabel.Custom -> {
                when (kind.takeIf { yearsSince != null }) {
                    DateKind.ANNIVERSARY -> Moment.ANNIVERSARY
                    DateKind.BIRTHDAY -> Moment.RELATIVE_BIRTHDAY
                    DateKind.OTHER, null -> null
                }
            }
        }

    private fun daysUntil(
        day: MonthDay,
        today: LocalDate,
    ): Long {
        val thisYear = day.atYear(today.year)
        val next = if (thisYear.isBefore(today)) day.atYear(today.year + 1) else thisYear
        return ChronoUnit.DAYS.between(today, next)
    }
}
