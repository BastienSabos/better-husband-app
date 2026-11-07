package com.bastiensabos.betterhusband.feature.dates

import com.bastiensabos.betterhusband.data.repository.FakeImportantDateRepository
import com.bastiensabos.betterhusband.data.repository.FakePartnerRepository
import com.bastiensabos.betterhusband.domain.ObserveUpcomingDatesUseCase
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import com.bastiensabos.betterhusband.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DatesViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2027-03-10T09:00:00Z"), ZoneOffset.UTC)
    private val repository = FakeImportantDateRepository()
    private val viewModel = DatesViewModel(ObserveUpcomingDatesUseCase(FakePartnerRepository(), repository, clock), repository)

    /** Garde le flux actif (WhileSubscribed) pendant le test. */
    private fun TestScope.collectState() {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    @Test
    fun `shows the empty state when there is no date`() =
        runTest {
            collectState()

            assertThat(viewModel.uiState.value.list).isEqualTo(DatesListState.Empty)
        }

    @Test
    fun `adding a date saves it, closes the editor and lists it`() =
        runTest {
            collectState()

            viewModel.onAddClick()
            viewModel.onTitleChange("Mariage")
            viewModel.onDateChange(LocalDate.of(2015, 6, 20))
            viewModel.onKindChange(DateKind.ANNIVERSARY)
            viewModel.onSave()
            advanceUntilIdle()

            assertThat(
                repository.state.value
                    .single()
                    .title,
            ).isEqualTo("Mariage")
            assertThat(viewModel.uiState.value.editor).isNull()
            val ready = viewModel.uiState.value.list as DatesListState.Ready
            assertThat(ready.items.single().yearsSince).isEqualTo(12)
        }

    @Test
    fun `cannot save without a title or a date`() =
        runTest {
            collectState()

            viewModel.onAddClick()
            viewModel.onTitleChange("Mariage")
            viewModel.onSave()
            advanceUntilIdle()

            assertThat(repository.state.value).isEmpty()
            assertThat(viewModel.uiState.value.editor).isNotNull()
        }

    @Test
    fun `editing loads the date and delete removes it`() =
        runTest {
            val trip = ImportantDate(title = "Voyage", kind = DateKind.OTHER, date = LocalDate.of(2027, 7, 1), recurring = false)
            val id = repository.save(trip)
            collectState()

            viewModel.onEdit(id)
            advanceUntilIdle()
            val editor = viewModel.uiState.value.editor!!
            assertThat(editor.title).isEqualTo("Voyage")
            assertThat(editor.recurring).isFalse()

            viewModel.onDelete()
            advanceUntilIdle()

            assertThat(repository.state.value).isEmpty()
            assertThat(viewModel.uiState.value.editor).isNull()
        }

    @Test
    fun `dismissing the editor discards the draft`() =
        runTest {
            collectState()

            viewModel.onAddClick()
            viewModel.onTitleChange("Brouillon")
            viewModel.onDismissEditor()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.editor).isNull()
            assertThat(repository.state.value).isEmpty()
        }
}
