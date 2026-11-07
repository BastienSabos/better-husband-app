package com.bastiensabos.betterhusband.feature.ideas

import com.bastiensabos.betterhusband.data.repository.FakeIdeaRepository
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import com.bastiensabos.betterhusband.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ShareIdeaViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = FakeIdeaRepository()
    private val viewModel = ShareIdeaViewModel(repository)

    @Test
    fun `shared text prefills the field, trimmed and capped`() {
        viewModel.onSharedText("  " + "a".repeat(300) + "  ")

        assertThat(viewModel.uiState.value.text).hasLength(200)
    }

    @Test
    fun `null or blank shared text is ignored`() {
        viewModel.onSharedText(null)
        viewModel.onSharedText("   ")

        assertThat(viewModel.uiState.value.text).isEmpty()
        assertThat(viewModel.uiState.value.canSave).isFalse()
    }

    @Test
    fun `shared text does not overwrite what the user already typed`() {
        viewModel.onTextChange("Mon texte")

        viewModel.onSharedText("Autre texte")

        assertThat(viewModel.uiState.value.text).isEqualTo("Mon texte")
    }

    @Test
    fun `save adds the idea with the chosen category and flags it saved`() =
        runTest {
            viewModel.onSharedText("Billets de concert")
            viewModel.onCategoryChange(IdeaCategory.OUTING)

            viewModel.onSave()
            advanceUntilIdle()

            val idea = repository.state.value.single()
            assertThat(idea.text).isEqualTo("Billets de concert")
            assertThat(idea.category).isEqualTo(IdeaCategory.OUTING)
            assertThat(viewModel.uiState.value.saved).isTrue()
            assertThat(viewModel.uiState.value.saving).isFalse()
        }

    @Test
    fun `nothing is saved without an explicit save`() =
        runTest {
            viewModel.onSharedText("Brouillon")
            advanceUntilIdle()

            assertThat(repository.state.value).isEmpty()
        }

    @Test
    fun `an empty field cannot be saved`() =
        runTest {
            viewModel.onSave()
            advanceUntilIdle()

            assertThat(repository.state.value).isEmpty()
            assertThat(viewModel.uiState.value.saved).isFalse()
        }
}
