package com.bastiensabos.betterhusband.data.repository

import app.cash.turbine.test
import com.bastiensabos.betterhusband.data.local.dao.IdeaDao
import com.bastiensabos.betterhusband.data.local.entity.IdeaEntity
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RoomIdeaRepositoryTest {
    private val dao = FakeIdeaDao()
    private val clock = Clock.fixed(Instant.parse("2027-03-10T09:00:00Z"), ZoneOffset.UTC)
    private val repository = RoomIdeaRepository(dao, clock)

    @Test
    fun `added idea is dated now, trimmed and not done`() =
        runTest {
            val id = repository.add("  Pivoines  ", IdeaCategory.GIFT)

            repository.observeAll().test {
                val idea = awaitItem().single()
                assertThat(idea.id).isEqualTo(id)
                assertThat(idea.text).isEqualTo("Pivoines")
                assertThat(idea.category).isEqualTo(IdeaCategory.GIFT)
                assertThat(idea.createdAt).isEqualTo(Instant.parse("2027-03-10T09:00:00Z"))
                assertThat(idea.done).isFalse()
            }
        }

    @Test
    fun `update changes content and done flag`() =
        runTest {
            val id = repository.add("Resto", IdeaCategory.OUTING)

            repository.updateContent(id, " Resto italien ", IdeaCategory.ATTENTION)
            repository.setDone(id, true)

            repository.observeAll().test {
                val idea = awaitItem().single()
                assertThat(idea.text).isEqualTo("Resto italien")
                assertThat(idea.category).isEqualTo(IdeaCategory.ATTENTION)
                assertThat(idea.done).isTrue()
            }
        }

    @Test
    fun `unknown category falls back to attention and delete removes`() =
        runTest {
            dao.insert(IdeaEntity(text = "X", category = "FROM_THE_FUTURE", createdAt = 0, done = false))

            repository.observeAll().test {
                val idea = awaitItem().single()
                assertThat(idea.category).isEqualTo(IdeaCategory.ATTENTION)
                repository.delete(idea.id)
                assertThat(awaitItem()).isEmpty()
            }
        }

    private class FakeIdeaDao : IdeaDao {
        private val state = MutableStateFlow<List<IdeaEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<IdeaEntity>> = state

        override suspend fun insert(entity: IdeaEntity): Long {
            val id = nextId++
            state.value = state.value + entity.copy(id = id)
            return id
        }

        override suspend fun updateContent(
            id: Long,
            text: String,
            category: String,
        ) {
            state.value = state.value.map { if (it.id == id) it.copy(text = text, category = category) else it }
        }

        override suspend fun setDone(
            id: Long,
            done: Boolean,
        ) {
            state.value = state.value.map { if (it.id == id) it.copy(done = done) else it }
        }

        override suspend fun delete(id: Long) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun deleteAll() {
            state.value = emptyList()
        }
    }
}
