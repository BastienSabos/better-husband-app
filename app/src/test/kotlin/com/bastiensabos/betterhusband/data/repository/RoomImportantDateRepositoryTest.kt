package com.bastiensabos.betterhusband.data.repository

import app.cash.turbine.test
import com.bastiensabos.betterhusband.data.local.dao.ImportantDateDao
import com.bastiensabos.betterhusband.data.local.entity.ImportantDateEntity
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class RoomImportantDateRepositoryTest {
    private val dao = FakeImportantDateDao()
    private val repository = RoomImportantDateRepository(dao)

    @Test
    fun `saved date is observed with its generated id and trimmed title`() =
        runTest {
            val id =
                repository.save(
                    ImportantDate(title = "  Mariage ", kind = DateKind.ANNIVERSARY, date = LocalDate.of(2015, 6, 20), recurring = true),
                )

            repository.observeAll().test {
                assertThat(awaitItem()).containsExactly(
                    ImportantDate(id, "Mariage", DateKind.ANNIVERSARY, LocalDate.of(2015, 6, 20), recurring = true),
                )
            }
        }

    @Test
    fun `saving with an existing id updates the date`() =
        runTest {
            val id = repository.save(ImportantDate(title = "A", kind = DateKind.OTHER, date = LocalDate.of(2027, 1, 1), recurring = false))

            repository.save(ImportantDate(id, "B", DateKind.OTHER, LocalDate.of(2027, 1, 1), recurring = false))

            repository.observeAll().test {
                assertThat(awaitItem().map { it.title }).containsExactly("B")
            }
        }

    @Test
    fun `unknown kind falls back to other and delete removes`() =
        runTest {
            dao.upsert(ImportantDateEntity(title = "X", kind = "FROM_THE_FUTURE", date = LocalDate.of(2027, 1, 1), recurring = false))

            repository.observeAll().test {
                val item = awaitItem().single()
                assertThat(item.kind).isEqualTo(DateKind.OTHER)
                repository.delete(item.id)
                assertThat(awaitItem()).isEmpty()
            }
        }

    private class FakeImportantDateDao : ImportantDateDao {
        private val state = MutableStateFlow<List<ImportantDateEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<ImportantDateEntity>> = state

        override suspend fun upsert(entity: ImportantDateEntity): Long {
            val id = if (entity.id == 0L) nextId++ else entity.id
            state.value = state.value.filterNot { it.id == id } + entity.copy(id = id)
            return id
        }

        override suspend fun delete(id: Long) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun deleteAll() {
            state.value = emptyList()
        }
    }
}
