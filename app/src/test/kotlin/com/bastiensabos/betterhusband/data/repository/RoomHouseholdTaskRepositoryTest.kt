package com.bastiensabos.betterhusband.data.repository

import app.cash.turbine.test
import com.bastiensabos.betterhusband.data.local.dao.HouseholdTaskDao
import com.bastiensabos.betterhusband.data.local.entity.HouseholdTaskEntity
import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class RoomHouseholdTaskRepositoryTest {
    private val dao = FakeHouseholdTaskDao()
    private val repository = RoomHouseholdTaskRepository(dao)
    private val created = LocalDate.of(2027, 1, 4)

    @Test
    fun `saved task is trimmed and never done`() =
        runTest {
            val id = repository.save(HouseholdTask(title = "  Poubelles ", repeat = TaskRepeat.WEEKLY, createdOn = created))

            repository.observeAll().test {
                val task = awaitItem().single()
                assertThat(task.id).isEqualTo(id)
                assertThat(task.title).isEqualTo("Poubelles")
                assertThat(task.repeat).isEqualTo(TaskRepeat.WEEKLY)
                assertThat(task.createdOn).isEqualTo(created)
                assertThat(task.lastDoneOn).isNull()
            }
        }

    @Test
    fun `marking done sets the date and clearing it restores never done`() =
        runTest {
            val id = repository.save(HouseholdTask(title = "Linge", repeat = TaskRepeat.DAILY, createdOn = created))

            repository.observeAll().test {
                assertThat(awaitItem().single().lastDoneOn).isNull()
                repository.setLastDone(id, LocalDate.of(2027, 1, 20))
                assertThat(awaitItem().single().lastDoneOn).isEqualTo(LocalDate.of(2027, 1, 20))
                repository.setLastDone(id, null)
                assertThat(awaitItem().single().lastDoneOn).isNull()
            }
        }

    @Test
    fun `unknown repeat falls back to weekly and delete removes`() =
        runTest {
            dao.upsert(HouseholdTaskEntity(title = "X", repeat = "FROM_THE_FUTURE", createdOn = created, lastDoneOn = null))

            repository.observeAll().test {
                val task = awaitItem().single()
                assertThat(task.repeat).isEqualTo(TaskRepeat.WEEKLY)
                repository.delete(task.id)
                assertThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun `delete all clears every task`() =
        runTest {
            repository.save(HouseholdTask(title = "A", repeat = TaskRepeat.DAILY, createdOn = created))
            repository.save(HouseholdTask(title = "B", repeat = TaskRepeat.MONTHLY, createdOn = created))

            repository.deleteAll()

            repository.observeAll().test { assertThat(awaitItem()).isEmpty() }
        }

    private class FakeHouseholdTaskDao : HouseholdTaskDao {
        private val state = MutableStateFlow<List<HouseholdTaskEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<HouseholdTaskEntity>> = state

        override suspend fun upsert(entity: HouseholdTaskEntity): Long {
            val id = if (entity.id == 0L) nextId++ else entity.id
            state.value = state.value.filterNot { it.id == id } + entity.copy(id = id)
            return id
        }

        override suspend fun setLastDone(
            id: Long,
            date: LocalDate?,
        ) {
            state.value = state.value.map { if (it.id == id) it.copy(lastDoneOn = date) else it }
        }

        override suspend fun delete(id: Long) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun deleteAll() {
            state.value = emptyList()
        }
    }
}
