package com.bastiensabos.betterhusband.data.repository

import app.cash.turbine.test
import com.bastiensabos.betterhusband.data.local.dao.AttentionLogDao
import com.bastiensabos.betterhusband.data.local.entity.AttentionLogEntity
import com.bastiensabos.betterhusband.domain.model.AttentionLogEntry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class RoomAttentionLogRepositoryTest {
    private val dao = FakeAttentionLogDao()
    private val repository = RoomAttentionLogRepository(dao)
    private val day = LocalDate.of(2027, 1, 20)

    @Test
    fun `saved attention is read back in date order`() =
        runTest {
            repository.save(AttentionLogEntry(day.plusDays(1), 7))
            repository.save(AttentionLogEntry(day, 3))

            repository.observeAll().test {
                assertThat(awaitItem()).containsExactly(AttentionLogEntry(day, 3), AttentionLogEntry(day.plusDays(1), 7)).inOrder()
            }
        }

    @Test
    fun `saving another attention the same day replaces it`() =
        runTest {
            repository.save(AttentionLogEntry(day, 3))
            repository.save(AttentionLogEntry(day, 9))

            repository.observeAll().test {
                assertThat(awaitItem()).containsExactly(AttentionLogEntry(day, 9))
            }
        }

    @Test
    fun `marking done only touches that day and can be undone`() =
        runTest {
            repository.save(AttentionLogEntry(day, 3))
            repository.save(AttentionLogEntry(day.plusDays(1), 4))

            repository.observeAll().test {
                awaitItem()
                repository.setDone(day, true)
                assertThat(awaitItem().map { it.done }).containsExactly(true, false).inOrder()
                repository.setDone(day, false)
                assertThat(awaitItem().map { it.done }).containsExactly(false, false).inOrder()
            }
        }

    @Test
    fun `marking a day without attention does nothing and delete all clears the history`() =
        runTest {
            repository.setDone(day, true)
            repository.save(AttentionLogEntry(day, 3))

            repository.deleteAll()

            repository.observeAll().test { assertThat(awaitItem()).isEmpty() }
        }

    private class FakeAttentionLogDao : AttentionLogDao {
        private val state = MutableStateFlow<List<AttentionLogEntity>>(emptyList())

        override fun observeAll(): Flow<List<AttentionLogEntity>> = state

        override suspend fun upsert(entity: AttentionLogEntity) {
            state.value = (state.value.filterNot { it.date == entity.date } + entity).sortedBy { it.date }
        }

        override suspend fun setDone(
            date: LocalDate,
            done: Boolean,
        ) {
            state.value = state.value.map { if (it.date == date) it.copy(done = done) else it }
        }

        override suspend fun deleteAll() {
            state.value = emptyList()
        }
    }
}
