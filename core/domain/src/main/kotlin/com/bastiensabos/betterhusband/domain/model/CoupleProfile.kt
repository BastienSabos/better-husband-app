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
)
