package com.bastiensabos.betterhusband.domain.model

import java.time.LocalDate

/** Comment l'app parle de la personne quand aucun prénom n'est utilisable ("ton partenaire", "ta partenaire"). */
enum class AddressForm {
    MASCULINE,
    FEMININE,
    NEUTRAL,
}

/** Fiche du ou de la partenaire. Une seule fiche en v1 (couple, pas de multi-partenaires). */
data class Partner(
    val firstName: String,
    val addressForm: AddressForm,
    val birthday: LocalDate? = null,
    val relationshipStart: LocalDate? = null,
)
