package com.bastiensabos.betterhusband.feature.wishes

import com.bastiensabos.betterhusband.data.repository.FakeWishRepository
import com.bastiensabos.betterhusband.data.repository.Wish
import com.bastiensabos.betterhusband.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class WishesViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = FakeWishRepository(theirs = listOf(Wish(1, "Une soirée sans écran", 0)))

    // Créé au premier usage, donc après MainDispatcherRule : sinon viewModelScope part sur un autre Main.
    private val viewModel by lazy { WishesViewModel(repository) }

    private fun TestScope.collectState() {
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        advanceUntilIdle()
    }

    @Test
    fun `shows their wishes apart from mine`() =
        runTest {
            collectState()

            assertThat(viewModel.uiState.value.mine).isEmpty()
            assertThat(
                viewModel.uiState.value.theirs
                    .map { it.text },
            ).containsExactly("Une soirée sans écran")
        }

    @Test
    fun `adding a wish clears the field and lists it`() =
        runTest {
            collectState()

            viewModel.onDraftChange("  Un livre ")
            viewModel.onAdd()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.draft).isEmpty()
            assertThat(
                viewModel.uiState.value.mine
                    .map { it.text },
            ).containsExactly("Un livre")
        }

    @Test
    fun `a blank draft cannot be added`() =
        runTest {
            collectState()

            viewModel.onDraftChange("   ")
            advanceUntilIdle()
            viewModel.onAdd()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.canAdd).isFalse()
            assertThat(viewModel.uiState.value.mine).isEmpty()
        }

    @Test
    fun `the draft is capped at the shared text limit`() =
        runTest {
            collectState()

            viewModel.onDraftChange("a".repeat(600))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.draft).hasLength(500)
        }

    @Test
    fun `deleting removes my wish`() =
        runTest {
            collectState()
            viewModel.onDraftChange("Un livre")
            viewModel.onAdd()
            advanceUntilIdle()

            viewModel.onDelete(
                viewModel.uiState.value.mine
                    .single()
                    .id,
            )
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.mine).isEmpty()
        }
}
