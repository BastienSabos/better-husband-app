package com.bastiensabos.betterhusband.feature.tasks

import com.bastiensabos.betterhusband.data.repository.FakeHouseholdTaskRepository
import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
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

class TasksViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val today = LocalDate.of(2027, 1, 20)
    private val clock = Clock.fixed(Instant.parse("2027-01-20T09:00:00Z"), ZoneOffset.UTC)
    private val repository = FakeHouseholdTaskRepository()
    private val viewModel = TasksViewModel(repository, clock)

    /** Garde le flux actif (WhileSubscribed) pendant le test. */
    private fun TestScope.collectState() {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun ready() = (viewModel.uiState.value.list as TasksListState.Ready).items

    @Test
    fun `shows the empty state when there is no task`() =
        runTest {
            collectState()

            assertThat(viewModel.uiState.value.list).isEqualTo(TasksListState.Empty)
        }

    @Test
    fun `quick add saves a task dated today, clears the field and keeps the repeat`() =
        runTest {
            collectState()

            viewModel.onDraftChange("  Poubelles ")
            viewModel.onDraftRepeatChange(TaskRepeat.DAILY)
            viewModel.onAdd()
            advanceUntilIdle()

            val task = repository.state.value.single()
            assertThat(task.title).isEqualTo("Poubelles")
            assertThat(task.repeat).isEqualTo(TaskRepeat.DAILY)
            assertThat(task.createdOn).isEqualTo(today)
            assertThat(viewModel.uiState.value.draft).isEmpty()
            assertThat(viewModel.uiState.value.draftRepeat).isEqualTo(TaskRepeat.DAILY)
        }

    @Test
    fun `blank draft cannot be added`() =
        runTest {
            collectState()

            viewModel.onDraftChange("   ")
            viewModel.onAdd()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.canAdd).isFalse()
            assertThat(repository.state.value).isEmpty()
        }

    @Test
    fun `due tasks come first and carry their delay`() =
        runTest {
            repository.save(HouseholdTask(title = "Faite", repeat = TaskRepeat.WEEKLY, createdOn = today, lastDoneOn = today))
            repository.save(
                HouseholdTask(
                    title = "En retard",
                    repeat = TaskRepeat.WEEKLY,
                    createdOn = today.minusDays(30),
                    lastDoneOn = today.minusDays(9),
                ),
            )
            collectState()

            val items = ready()
            assertThat(items.map { it.task.title }).containsExactly("En retard", "Faite").inOrder()
            assertThat(items[0].done).isFalse()
            assertThat(items[0].daysUntilDue).isEqualTo(-2)
            assertThat(items[1].done).isTrue()
            assertThat(items[1].daysUntilDue).isEqualTo(7)
        }

    @Test
    fun `checking a task marks it done today and unchecking makes it due again`() =
        runTest {
            val id = repository.save(HouseholdTask(title = "Linge", repeat = TaskRepeat.WEEKLY, createdOn = today.minusDays(3)))
            collectState()
            assertThat(ready().single().done).isFalse()

            viewModel.onToggleDone(id, true)
            advanceUntilIdle()
            assertThat(
                repository.state.value
                    .single()
                    .lastDoneOn,
            ).isEqualTo(today)
            assertThat(ready().single().done).isTrue()

            viewModel.onToggleDone(id, false)
            advanceUntilIdle()
            assertThat(
                repository.state.value
                    .single()
                    .lastDoneOn,
            ).isNull()
            assertThat(ready().single().done).isFalse()
        }

    @Test
    fun `editor saves the new title and repeat and keeps the history`() =
        runTest {
            val id =
                repository.save(
                    HouseholdTask(title = "Linge", repeat = TaskRepeat.WEEKLY, createdOn = today.minusDays(3), lastDoneOn = today),
                )
            collectState()

            viewModel.onEdit(repository.state.value.single())
            viewModel.onEditorTitleChange("Grosse lessive")
            viewModel.onEditorRepeatChange(TaskRepeat.MONTHLY)
            viewModel.onSaveEditor()
            advanceUntilIdle()

            val task = repository.state.value.single()
            assertThat(task.id).isEqualTo(id)
            assertThat(task.title).isEqualTo("Grosse lessive")
            assertThat(task.repeat).isEqualTo(TaskRepeat.MONTHLY)
            assertThat(task.createdOn).isEqualTo(today.minusDays(3))
            assertThat(task.lastDoneOn).isEqualTo(today)
            assertThat(viewModel.uiState.value.editor).isNull()
        }

    @Test
    fun `editor cannot save a blank title and delete removes the task`() =
        runTest {
            repository.save(HouseholdTask(title = "Linge", repeat = TaskRepeat.WEEKLY, createdOn = today))
            collectState()
            viewModel.onEdit(repository.state.value.single())

            viewModel.onEditorTitleChange("  ")
            viewModel.onSaveEditor()
            advanceUntilIdle()
            assertThat(
                repository.state.value
                    .single()
                    .title,
            ).isEqualTo("Linge")
            assertThat(viewModel.uiState.value.editor).isNotNull()

            viewModel.onDeleteEditor()
            advanceUntilIdle()
            assertThat(repository.state.value).isEmpty()
            assertThat(viewModel.uiState.value.editor).isNull()
        }
}
