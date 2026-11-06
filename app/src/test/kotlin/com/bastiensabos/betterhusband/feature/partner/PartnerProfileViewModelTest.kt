package com.bastiensabos.betterhusband.feature.partner

import com.bastiensabos.betterhusband.data.repository.FakePartnerRepository
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.domain.model.Relative
import com.bastiensabos.betterhusband.domain.model.SizeKind
import com.bastiensabos.betterhusband.domain.model.Taste
import com.bastiensabos.betterhusband.domain.model.TasteCategory
import com.bastiensabos.betterhusband.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class PartnerProfileViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val existing =
        Partner(
            firstName = "Camille",
            addressForm = AddressForm.FEMININE,
            birthday = LocalDate.of(1992, 4, 12),
            tastes = listOf(Taste(TasteCategory.FLOWERS, "Pivoines"), Taste(TasteCategory.FLOWERS, "Tulipes")),
            sizes = mapOf(SizeKind.SHOES to "39"),
            relatives = listOf(Relative("Léa", "Sœur")),
        )

    @Test
    fun `loads the existing partner into the draft`() = runTest {
        val viewModel = PartnerProfileViewModel(FakePartnerRepository(existing))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.loaded).isTrue()
        assertThat(state.firstName).isEqualTo("Camille")
        assertThat(state.tastes[TasteCategory.FLOWERS]).isEqualTo("Pivoines, Tulipes")
        assertThat(state.relatives.single().name).isEqualTo("Léa")
    }

    @Test
    fun `nothing is written until save`() = runTest {
        val repository = FakePartnerRepository(existing)
        val viewModel = PartnerProfileViewModel(repository)
        advanceUntilIdle()

        viewModel.onFirstNameChange("Autre")
        advanceUntilIdle()

        assertThat(repository.state.value).isEqualTo(existing)
    }

    @Test
    fun `save parses tastes and sizes and drops blank entries`() = runTest {
        val repository = FakePartnerRepository(existing)
        val viewModel = PartnerProfileViewModel(repository)
        advanceUntilIdle()

        viewModel.onTastesChange(TasteCategory.FLOWERS, " Roses , ,Roses, Lys ")
        viewModel.onTastesChange(TasteCategory.COLORS, "")
        viewModel.onSizeChange(SizeKind.SHOES, "  ")
        viewModel.onSizeChange(SizeKind.RING, " 52 ")
        viewModel.onAddRelative()
        viewModel.onSave()
        advanceUntilIdle()

        val saved = repository.state.value!!
        assertThat(saved.tastes).containsExactly(Taste(TasteCategory.FLOWERS, "Roses"), Taste(TasteCategory.FLOWERS, "Lys"))
        assertThat(saved.sizes).containsExactly(SizeKind.RING, "52")
        assertThat(saved.relatives).containsExactly(Relative("Léa", "Sœur"))
        assertThat(viewModel.uiState.value.saved).isTrue()
    }

    @Test
    fun `cannot save without a first name`() = runTest {
        val repository = FakePartnerRepository(existing)
        val viewModel = PartnerProfileViewModel(repository)
        advanceUntilIdle()

        viewModel.onFirstNameChange("  ")
        viewModel.onSave()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.canSave).isFalse()
        assertThat(repository.state.value).isEqualTo(existing)
    }

    @Test
    fun `added relatives can be edited and removed`() = runTest {
        val viewModel = PartnerProfileViewModel(FakePartnerRepository(existing))
        advanceUntilIdle()

        viewModel.onAddRelative()
        val added = viewModel.uiState.value.relatives.last()
        viewModel.onRelativeChange(added.copy(name = "Sam", birthday = LocalDate.of(1990, 1, 1)))
        assertThat(viewModel.uiState.value.relatives.last().name).isEqualTo("Sam")

        viewModel.onRemoveRelative(added.id)
        assertThat(viewModel.uiState.value.relatives.map { it.name }).containsExactly("Léa")
    }
}
