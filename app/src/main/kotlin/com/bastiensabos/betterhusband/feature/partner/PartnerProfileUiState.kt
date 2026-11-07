package com.bastiensabos.betterhusband.feature.partner

import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.SizeKind
import com.bastiensabos.betterhusband.domain.model.TasteCategory
import java.time.LocalDate
import java.time.Period

/** Proche en cours d'édition. L'identifiant est local à l'écran et ne sert qu'à garder des clés stables. */
data class RelativeDraft(
    val id: Long,
    val relation: String = "",
    val name: String = "",
    val birthday: LocalDate? = null,
)

/** Partie de la fiche qui s'édite d'un bloc ; le reste de l'écran reste en lecture. */
enum class PartnerSection { IDENTITY, TASTES, SIZES, RELATIVES }

/**
 * La fiche, en lecture quand [editing] est nul. Les goûts sont saisis en texte libre séparé par des virgules (une
 * ligne par catégorie) pour rester sous les 5 minutes par jour.
 */
data class PartnerProfileUiState(
    val loaded: Boolean = false,
    /** Une fiche est déjà enregistrée : sans elle, il n'y a rien à lire et l'écran s'ouvre sur l'identité. */
    val hasProfile: Boolean = false,
    val editing: PartnerSection? = null,
    val today: LocalDate = LocalDate.MIN,
    val firstName: String = "",
    val addressForm: AddressForm = AddressForm.NEUTRAL,
    val birthday: LocalDate? = null,
    val relationshipStart: LocalDate? = null,
    val tastes: Map<TasteCategory, String> = emptyMap(),
    val sizes: Map<SizeKind, String> = emptyMap(),
    val relatives: List<RelativeDraft> = emptyList(),
    val saving: Boolean = false,
) {
    val canSave: Boolean get() = loaded && firstName.isNotBlank() && !saving

    /** Années pleines depuis le début de la relation, nul si la date n'est pas connue ou dans le futur. */
    val yearsTogether: Int?
        get() = relationshipStart?.takeIf { !it.isAfter(today) }?.let { Period.between(it, today).years }

    /** Goûts de la fiche, découpés comme ils seront enregistrés : ce que la lecture affiche. */
    val tasteLabels: Map<TasteCategory, List<String>>
        get() = tastes.mapValues { splitTastes(it.value) }.filterValues { it.isNotEmpty() }
}

internal fun splitTastes(text: String): List<String> =
    text
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
