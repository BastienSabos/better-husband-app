package com.bastiensabos.betterhusband.feature.ideas

import com.bastiensabos.betterhusband.data.repository.FakeIdeaRepository
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import com.bastiensabos.betterhusband.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class IdeasViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = FakeIdeaRepository()
    private val viewModel = IdeasViewModel(repository)

    /** Garde le flux actif (WhileSubscribed) pendant le test. */
    private fun TestScope.collectState() {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    @Test
    fun `shows the empty state when the notebook is empty`() = runTest {
        collectState()

        assertThat(viewModel.uiState.value.list).isEqualTo(IdeasListState.Empty)
    }

    @Test
    fun `quick capture saves the idea, clears the field and keeps the category`() = runTest {
        collectState()

        viewModel.onDraftChange("Pivoines")
        viewModel.onDraftCategoryChange(IdeaCategory.OUTING)
        viewModel.onAdd()
        advanceUntilIdle()

        assertThat(repository.state.value.single().text).isEqualTo("Pivoines")
        assertThat(repository.state.value.single().category).isEqualTo(IdeaCategory.OUTING)
        assertThat(viewModel.uiState.value.draft).isEmpty()
        assertThat(viewModel.uiState.value.draftCategory).isEqualTo(IdeaCategory.OUTING)
    }

    @Test
    fun `a blank draft cannot be added`() = runTest {
        collectState()

        viewModel.onDraftChange("   ")
        viewModel.onAdd()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.canAdd).isFalse()
        assertThat(repository.state.value).isEmpty()
    }

    @Test
    fun `filter keeps only the chosen category`() = runTest {
        repository.add("Pivoines", IdeaCategory.GIFT)
        repository.add("Resto", IdeaCategory.OUTING)
        collectState()

        viewModel.onFilterChange(IdeaCategory.OUTING)
        advanceUntilIdle()

        val ready = viewModel.uiState.value.list as IdeasListState.Ready
        assertThat(ready.items.map { it.text }).containsExactly("Resto")

        viewModel.onFilterChange(IdeaCategory.ATTENTION)
        advanceUntilIdle()
        assertThat((viewModel.uiState.value.list as IdeasListState.Ready).items).isEmpty()
    }

    @Test
    fun `toggling done updates the idea`() = runTest {
        val id = repository.add("Pivoines", IdeaCategory.GIFT)
        collectState()

        viewModel.onToggleDone(id, true)
        advanceUntilIdle()

        assertThat(repository.state.value.single().done).isTrue()
    }

    @Test
    fun `editing saves changes, delete removes, dismiss discards`() = runTest {
        val id = repository.add("Resto", IdeaCategory.OUTING)
        collectState()

        viewModel.onEdit(repository.state.value.single())
        viewModel.onEditorTextChange("Resto italien")
        viewModel.onEditorCategoryChange(IdeaCategory.ATTENTION)
        viewModel.onSaveEditor()
        advanceUntilIdle()
        assertThat(repository.state.value.single().text).isEqualTo("Resto italien")
        assertThat(repository.state.value.single().category).isEqualTo(IdeaCategory.ATTENTION)
        assertThat(viewModel.uiState.value.editor).isNull()

        viewModel.onEdit(repository.state.value.single())
        viewModel.onEditorTextChange("Brouillon")
        viewModel.onDismissEditor()
        advanceUntilIdle()
        assertThat(repository.state.value.single().text).isEqualTo("Resto italien")

        viewModel.onEdit(repository.state.value.single())
        viewModel.onDeleteEditor()
        advanceUntilIdle()
        assertThat(repository.state.value.none { it.id == id }).isTrue()
        assertThat(viewModel.uiState.value.editor).isNull()
    }
}
