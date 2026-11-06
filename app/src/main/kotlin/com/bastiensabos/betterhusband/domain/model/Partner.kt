package com.bastiensabos.betterhusband.domain.model

import java.time.LocalDate

/** Comment l'app parle de la personne quand aucun prénom n'est utilisable ("ton partenaire", "ta partenaire"). */
enum class AddressForm {
    MASCULINE,
    FEMININE,
    NEUTRAL,
}

enum class TasteCategory {
    FLOWERS,
    FOOD,
    COLORS,
    BRANDS,
}

enum class SizeKind {
    CLOTHES,
    SHOES,
    RING,
}

data class Taste(
    val category: TasteCategory,
    val label: String,
)

/** Proche important (parent, ami, collègue…). Le lien est un texte libre pour ne présupposer aucune famille. */
data class Relative(
    val name: String,
    val relation: String,
    val birthday: LocalDate? = null,
)

/** Fiche du ou de la partenaire. Une seule fiche en v1 (couple, pas de multi-partenaires). */
data class Partner(
    val firstName: String,
    val addressForm: AddressForm,
    val birthday: LocalDate? = null,
    val relationshipStart: LocalDate? = null,
    val tastes: List<Taste> = emptyList(),
    val sizes: Map<SizeKind, String> = emptyMap(),
    val relatives: List<Relative> = emptyList(),
)
