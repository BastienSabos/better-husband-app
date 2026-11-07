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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class PartnerProfileViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-07T09:00:00Z"), ZoneOffset.UTC)

    private val existing =
        Partner(
            firstName = "Camille",
            addressForm = AddressForm.FEMININE,
            birthday = LocalDate.of(1992, 4, 12),
            tastes = listOf(Taste(TasteCategory.FLOWERS, "Pivoines"), Taste(TasteCategory.FLOWERS, "Tulipes")),
            sizes = mapOf(SizeKind.SHOES to "39"),
            relationshipStart = LocalDate.of(2017, 11, 8),
            relatives = listOf(Relative("Léa", "Sœur")),
        )

    @Test
    fun `loads the existing partner into the draft`() =
        runTest {
            val viewModel = PartnerProfileViewModel(FakePartnerRepository(existing), clock)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.loaded).isTrue()
            assertThat(state.firstName).isEqualTo("Camille")
            assertThat(state.tastes[TasteCategory.FLOWERS]).isEqualTo("Pivoines, Tulipes")
            assertThat(state.relatives.single().name).isEqualTo("Léa")
            assertThat(state.editing).isNull()
            // Depuis le 8 novembre 2017 : les neuf ans ne tombent que demain.
            assertThat(state.yearsTogether).isEqualTo(8)
        }

    @Test
    fun `without a saved profile the screen opens on the identity editor`() =
        runTest {
            val viewModel = PartnerProfileViewModel(FakePartnerRepository(), clock)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.hasProfile).isFalse()
            assertThat(viewModel.uiState.value.editing).isEqualTo(PartnerSection.IDENTITY)
        }

    @Test
    fun `saving a section writes it and goes back to reading, cleaned up`() =
        runTest {
            val repository = FakePartnerRepository(existing)
            val viewModel = PartnerProfileViewModel(repository, clock)
            advanceUntilIdle()

            viewModel.onEdit(PartnerSection.TASTES)
            viewModel.onTastesChange(TasteCategory.COLORS, "Vert sauge, , Vert sauge")
            viewModel.onSave()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.editing).isNull()
            assertThat(state.tasteLabels[TasteCategory.COLORS]).containsExactly("Vert sauge")
            assertThat(repository.state.value!!.tastes).contains(Taste(TasteCategory.COLORS, "Vert sauge"))
        }

    @Test
    fun `cancelling a section drops the draft and writes nothing`() =
        runTest {
            val repository = FakePartnerRepository(existing)
            val viewModel = PartnerProfileViewModel(repository, clock)
            advanceUntilIdle()

            viewModel.onEdit(PartnerSection.SIZES)
            viewModel.onSizeChange(SizeKind.SHOES, "41")
            viewModel.onCancelEdit()

            val state = viewModel.uiState.value
            assertThat(state.editing).isNull()
            assertThat(state.sizes[SizeKind.SHOES]).isEqualTo("39")
            assertThat(repository.state.value).isEqualTo(existing)
        }

    @Test
    fun `nothing is written until save`() =
        runTest {
            val repository = FakePartnerRepository(existing)
            val viewModel = PartnerProfileViewModel(repository, clock)
            advanceUntilIdle()

            viewModel.onFirstNameChange("Autre")
            advanceUntilIdle()

            assertThat(repository.state.value).isEqualTo(existing)
        }

    @Test
    fun `save parses tastes and sizes and drops blank entries`() =
        runTest {
            val repository = FakePartnerRepository(existing)
            val viewModel = PartnerProfileViewModel(repository, clock)
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
            assertThat(viewModel.uiState.value.editing).isNull()
        }

    @Test
    fun `cannot save without a first name`() =
        runTest {
            val repository = FakePartnerRepository(existing)
            val viewModel = PartnerProfileViewModel(repository, clock)
            advanceUntilIdle()

            viewModel.onFirstNameChange("  ")
            viewModel.onSave()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.canSave).isFalse()
            assertThat(repository.state.value).isEqualTo(existing)
        }

    @Test
    fun `added relatives can be edited and removed`() =
        runTest {
            val viewModel = PartnerProfileViewModel(FakePartnerRepository(existing), clock)
            advanceUntilIdle()

            viewModel.onAddRelative()
            val added =
                viewModel.uiState.value.relatives
                    .last()
            viewModel.onRelativeChange(added.copy(name = "Sam", birthday = LocalDate.of(1990, 1, 1)))
            assertThat(
                viewModel.uiState.value.relatives
                    .last()
                    .name,
            ).isEqualTo("Sam")

            viewModel.onRemoveRelative(added.id)
            assertThat(
                viewModel.uiState.value.relatives
                    .map { it.name },
            ).containsExactly("Léa")
        }
}
