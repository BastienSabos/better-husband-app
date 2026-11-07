package com.bastiensabos.betterhusband.feature.partner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.data.repository.PartnerRepository
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.domain.model.Relative
import com.bastiensabos.betterhusband.domain.model.SizeKind
import com.bastiensabos.betterhusband.domain.model.Taste
import com.bastiensabos.betterhusband.domain.model.TasteCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Fiche en lecture, éditée une section à la fois. Le brouillon d'une section n'est écrit qu'à l'enregistrement ;
 * l'annuler rend la fiche telle qu'elle est enregistrée.
 */
@HiltViewModel
class PartnerProfileViewModel
    @Inject
    constructor(
        private val partnerRepository: PartnerRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PartnerProfileUiState())
        val uiState: StateFlow<PartnerProfileUiState> = _uiState.asStateFlow()
        private var nextRelativeId = 0L

        /** La fiche telle qu'elle est enregistrée, rendue quand une édition est annulée. */
        private var persisted = PartnerProfileUiState()

        init {
            viewModelScope.launch {
                val partner = partnerRepository.observePartner().first()
                persisted = partner.toDraft()
                _uiState.value = persisted.copy(editing = if (partner == null) PartnerSection.IDENTITY else null)
            }
        }

        fun onEdit(section: PartnerSection) = _uiState.update { if (it.loaded && it.editing == null) it.copy(editing = section) else it }

        fun onCancelEdit() = _uiState.update { persisted }

        fun onFirstNameChange(value: String) = _uiState.update { it.copy(firstName = value.take(MAX_NAME_LENGTH)) }

        fun onAddressFormSelect(value: AddressForm) = _uiState.update { it.copy(addressForm = value) }

        fun onBirthdayChange(value: LocalDate?) = _uiState.update { it.copy(birthday = value) }

        fun onRelationshipStartChange(value: LocalDate?) = _uiState.update { it.copy(relationshipStart = value) }

        fun onTastesChange(
            category: TasteCategory,
            text: String,
        ) = _uiState.update { it.copy(tastes = it.tastes + (category to text)) }

        fun onSizeChange(
            kind: SizeKind,
            text: String,
        ) = _uiState.update { it.copy(sizes = it.sizes + (kind to text.take(MAX_SIZE_LENGTH))) }

        fun onAddRelative() = _uiState.update { it.copy(relatives = it.relatives + RelativeDraft(id = nextRelativeId++)) }

        fun onRelativeChange(draft: RelativeDraft) =
            _uiState.update { state -> state.copy(relatives = state.relatives.map { if (it.id == draft.id) draft else it }) }

        fun onRemoveRelative(id: Long) = _uiState.update { state -> state.copy(relatives = state.relatives.filterNot { it.id == id }) }

        fun onSave() {
            val state = _uiState.value
            if (!state.canSave) return
            _uiState.update { it.copy(saving = true) }
            viewModelScope.launch {
                try {
                    val partner = state.toPartner()
                    partnerRepository.savePartner(partner)
                    // Relu depuis ce qui est enregistré : la lecture montre les goûts dédoublonnés, les lignes vides en moins.
                    persisted = partner.toDraft()
                    _uiState.value = persisted
                } finally {
                    _uiState.update { it.copy(saving = false) }
                }
            }
        }

        private fun Partner?.toDraft(): PartnerProfileUiState {
            val today = LocalDate.now(clock)
            if (this == null) return PartnerProfileUiState(loaded = true, today = today)
            return PartnerProfileUiState(
                loaded = true,
                hasProfile = true,
                today = today,
                firstName = firstName,
                addressForm = addressForm,
                birthday = birthday,
                relationshipStart = relationshipStart,
                tastes = tastes.groupBy({ it.category }, { it.label }).mapValues { it.value.joinToString(SEPARATOR) },
                sizes = sizes,
                relatives = relatives.map { RelativeDraft(nextRelativeId++, it.relation, it.name, it.birthday) },
            )
        }

        private fun PartnerProfileUiState.toPartner() =
            Partner(
                firstName = firstName,
                addressForm = addressForm,
                birthday = birthday,
                relationshipStart = relationshipStart,
                tastes =
                    tastes.flatMap { (category, text) ->
                        splitTastes(text).map { Taste(category, it) }
                    },
                sizes = sizes.mapValues { it.value.trim() }.filterValues(String::isNotEmpty),
                // Un proche sans prénom n'a pas de sens : la ligne vide est simplement ignorée.
                relatives =
                    relatives
                        .filter { it.name.isNotBlank() }
                        .map { Relative(name = it.name.trim(), relation = it.relation.trim(), birthday = it.birthday) },
            )

        private companion object {
            const val MAX_NAME_LENGTH = 40
            const val MAX_SIZE_LENGTH = 20
            const val SEPARATOR = ", "
        }
    }
