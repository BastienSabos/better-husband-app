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
import java.time.LocalDate
import javax.inject.Inject

/** Édite un brouillon de la fiche ; rien n'est écrit tant que l'utilisateur n'enregistre pas. */
@HiltViewModel
class PartnerProfileViewModel
    @Inject
    constructor(
        private val partnerRepository: PartnerRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PartnerProfileUiState())
        val uiState: StateFlow<PartnerProfileUiState> = _uiState.asStateFlow()
        private var nextRelativeId = 0L

        init {
            viewModelScope.launch {
                val partner = partnerRepository.observePartner().first()
                _uiState.value = partner.toDraft()
            }
        }

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
                    partnerRepository.savePartner(state.toPartner())
                    _uiState.update { it.copy(saved = true) }
                } finally {
                    _uiState.update { it.copy(saving = false) }
                }
            }
        }

        private fun Partner?.toDraft(): PartnerProfileUiState {
            if (this == null) return PartnerProfileUiState(loaded = true)
            return PartnerProfileUiState(
                loaded = true,
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
                        text
                            .split(',')
                            .map(String::trim)
                            .filter(String::isNotEmpty)
                            .distinct()
                            .map { Taste(category, it) }
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
