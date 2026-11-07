package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.Budget
import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import com.bastiensabos.betterhusband.domain.model.Interest
import com.bastiensabos.betterhusband.domain.model.KidsAtHome
import com.bastiensabos.betterhusband.domain.model.LifeRhythm
import com.bastiensabos.betterhusband.domain.model.LivingArrangement
import java.time.Month

/** Ce qu'on sait au moment de choisir l'attention du jour. */
data class AttentionContext(
    val profile: CoupleProfile,
    /** Années pleines depuis le début de la relation, `null` si la date n'est pas renseignée. */
    val relationshipYears: Int?,
    val month: Month,
)

/**
 * Groupes de la banque d'attentions : chaque groupe est une liste de textes qui ne conviennent qu'à une situation.
 * L'identifiant d'une attention vaut [offset] + sa position dans son groupe : l'[offset] d'un groupe ne change jamais
 * et un groupe ne se réordonne pas (l'historique garde les identifiants), on n'ajoute qu'à la fin.
 */
enum class AttentionGroup(
    val offset: Int,
    private val appliesTo: (AttentionContext) -> Boolean,
) {
    /** Convient à tout le monde : gratuit ou presque, court, sans présupposer de situation. */
    EVERYONE(0, { true }),

    TOGETHER(10_000, { it.profile.living == LivingArrangement.TOGETHER }),

    /** Chacun chez soi ou à distance. */
    APART(20_000, { it.profile.living == LivingArrangement.APART || it.profile.living == LivingArrangement.LONG_DISTANCE }),
    LONG_DISTANCE(30_000, { it.profile.living == LivingArrangement.LONG_DISTANCE }),

    NO_KIDS(40_000, { it.profile.kids == KidsAtHome.NONE }),
    KIDS(50_000, { it.profile.kids.let { kids -> kids != null && kids != KidsAtHome.NONE } }),
    YOUNG_KIDS(60_000, { it.profile.kids == KidsAtHome.YOUNG || it.profile.kids == KidsAtHome.BOTH }),
    TEENS(70_000, { it.profile.kids == KidsAtHome.TEENS || it.profile.kids == KidsAtHome.BOTH }),

    /** Coûte un peu (un restaurant simple, un petit cadeau) : pas proposé à qui a dit avoir un budget serré. */
    TREATS(80_000, { it.profile.budget == Budget.MODERATE || it.profile.budget == Budget.COMFORTABLE }),
    SPLURGE(90_000, { it.profile.budget == Budget.COMFORTABLE }),

    /** Demande une soirée ou une demi-journée : pas proposé à qui a dit avoir un rythme très chargé. */
    TAKES_TIME(100_000, { it.profile.rhythm == LifeRhythm.BALANCED || it.profile.rhythm == LifeRhythm.RELAXED }),

    /** Moins de deux ans ensemble. */
    NEW_COUPLE(110_000, { years -> years.relationshipYears.let { it != null && it < NEW_COUPLE_YEARS } }),

    /** Dix ans ou plus ensemble. */
    LONG_COUPLE(120_000, { years -> years.relationshipYears.let { it != null && it >= LONG_COUPLE_YEARS } }),

    WINTER(130_000, { it.month in WINTER_MONTHS }),
    SPRING(140_000, { it.month in SPRING_MONTHS }),
    SUMMER(150_000, { it.month in SUMMER_MONTHS }),
    AUTUMN(160_000, { it.month in AUTUMN_MONTHS }),

    NATURE(200_000, { Interest.NATURE in it.profile.interests }),
    CULTURE(210_000, { Interest.CULTURE in it.profile.interests }),
    SPORT(220_000, { Interest.SPORT in it.profile.interests }),
    COOKING(230_000, { Interest.COOKING in it.profile.interests }),
    GAMES(240_000, { Interest.GAMES in it.profile.interests }),
    TRAVEL(250_000, { Interest.TRAVEL in it.profile.interests }),
    MUSIC(260_000, { Interest.MUSIC in it.profile.interests }),
    READING(270_000, { Interest.READING in it.profile.interests }),
    DIY(280_000, { Interest.DIY in it.profile.interests }),
    WELLBEING(290_000, { Interest.WELLBEING in it.profile.interests }),
    ;

    fun appliesTo(context: AttentionContext): Boolean = appliesTo.invoke(context)

    companion object {
        /** Place réservée à chaque groupe : bien plus que ce qu'on écrira jamais. */
        const val GROUP_CAPACITY = 10_000

        /** Groupe d'un identifiant, `null` s'il ne correspond à aucun groupe connu. */
        fun of(id: Int): AttentionGroup? = entries.firstOrNull { id >= it.offset && id < it.offset + GROUP_CAPACITY }
    }
}

private const val NEW_COUPLE_YEARS = 2
private const val LONG_COUPLE_YEARS = 10
private val WINTER_MONTHS = setOf(Month.DECEMBER, Month.JANUARY, Month.FEBRUARY)
private val SPRING_MONTHS = setOf(Month.MARCH, Month.APRIL, Month.MAY)
private val SUMMER_MONTHS = setOf(Month.JUNE, Month.JULY, Month.AUGUST)
private val AUTUMN_MONTHS = setOf(Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER)

/** Les attentions proposables dans un contexte, à partir de la taille de chaque groupe de la banque. */
object AttentionCatalog {
    fun eligibleIds(
        groupSizes: Map<AttentionGroup, Int>,
        context: AttentionContext,
    ): List<Int> =
        AttentionGroup.entries
            .filter { it.appliesTo(context) }
            .flatMap { group -> (0 until (groupSizes[group] ?: 0)).map { group.offset + it } }
}
