package com.bastiensabos.betterhusband.feature.partner

import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.SizeKind
import com.bastiensabos.betterhusband.domain.model.TasteCategory
import java.time.LocalDate

/** Proche en cours d'édition. L'identifiant est local à l'écran et ne sert qu'à garder des clés stables. */
data class RelativeDraft(
    val id: Long,
    val relation: String = "",
    val name: String = "",
    val birthday: LocalDate? = null,
)

/**
 * Brouillon de la fiche. Les goûts sont saisis en texte libre séparé par des virgules (une ligne par catégorie)
 * pour rester sous les 5 minutes par jour.
 */
data class PartnerProfileUiState(
    val loaded: Boolean = false,
    val firstName: String = "",
    val addressForm: AddressForm = AddressForm.NEUTRAL,
    val birthday: LocalDate? = null,
    val relationshipStart: LocalDate? = null,
    val tastes: Map<TasteCategory, String> = emptyMap(),
    val sizes: Map<SizeKind, String> = emptyMap(),
    val relatives: List<RelativeDraft> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = loaded && firstName.isNotBlank() && !saving
}
