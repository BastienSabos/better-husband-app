package com.bastiensabos.betterhusband.domain.model

/** Enfants qui vivent à la maison. Le domaine ne porte aucun texte : l'UI traduit chaque valeur. */
enum class KidsAtHome {
    NONE,

    /** Moins de 11 ans environ. */
    YOUNG,

    /** Préados, ados ou grands encore à la maison. */
    TEENS,
    BOTH,
}

enum class LivingArrangement {
    TOGETHER,

    /** Chacun chez soi, pas loin. */
    APART,
    LONG_DISTANCE,
}

/** Ce qui décrit la famille au-delà du nombre d'enfants. Plusieurs réponses possibles, aucune obligatoire. */
enum class FamilySituation {
    /** Des enfants de l'un ou de l'autre, d'une union précédente. */
    STEPFAMILY,

    /** Les enfants vivent une partie du temps dans une autre maison. */
    SHARED_CUSTODY,
}

enum class Budget {
    TIGHT,
    MODERATE,
    COMFORTABLE,
}

/** Le temps libre qui reste dans une semaine ordinaire. */
enum class LifeRhythm {
    VERY_BUSY,
    BALANCED,
    RELAXED,
}

enum class Interest {
    NATURE,
    CULTURE,
    SPORT,
    COOKING,
    GAMES,
    TRAVEL,
    MUSIC,
    READING,
    DIY,
    WELLBEING,
}

/**
 * Ce que l'utilisateur veut bien dire de sa situation, pour choisir des attentions qui lui ressemblent. Tout est
 * facultatif : `null` (ou vide) veut dire « pas répondu », et les attentions qui en dépendent ne sont pas proposées.
 */
data class CoupleProfile(
    val kids: KidsAtHome? = null,
    val living: LivingArrangement? = null,
    val budget: Budget? = null,
    val rhythm: LifeRhythm? = null,
    val interests: Set<Interest> = emptySet(),
    val family: Set<FamilySituation> = emptySet(),
)
