package com.bastiensabos.betterhusband.data.repository

import app.cash.turbine.test
import com.bastiensabos.betterhusband.data.local.dao.ImportantDateDao
import com.bastiensabos.betterhusband.data.local.entity.ImportantDateEntity
import com.bastiensabos.betterhusband.data.local.entity.SyncColumns
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class RoomImportantDateRepositoryTest {
    private val dao = FakeImportantDateDao()
    private val duo = FakeDuoRepository()
    private val clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
    private val repository = RoomImportantDateRepository(dao, duo, clock)
    private val day = LocalDate.of(2027, 6, 1)

    private fun inDuo() {
        duo.state.value = DuoSession("duo-1", "me", ended = false, partnerMemberId = "other")
    }

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
    fun `editing a shared date keeps its uid and signs a new revision`() =
        runTest {
            inDuo()
            val id =
                repository.save(
                    ImportantDate(title = "Rencontre", kind = DateKind.ANNIVERSARY, date = day, recurring = true, shared = true),
                )
            val first = dao.syncOf(id)!!

            repository.save(
                ImportantDate(id = id, title = "Notre rencontre", kind = DateKind.ANNIVERSARY, date = day, recurring = true, shared = true),
            )

            val after = dao.syncOf(id)!!
            assertThat(after.uid).isEqualTo(first.uid)
            assertThat(after.shared).isTrue()
            assertThat(after.author).isEqualTo("me")
            assertThat(after.revision).isGreaterThan(first.revision)
            assertThat(after.localChangedAt).isEqualTo(NOW)
            assertThat(dao.snapshot().single().title).isEqualTo("Notre rencontre")
        }

    @Test
    fun `in solo a date is never shared`() =
        runTest {
            val id =
                repository.save(
                    ImportantDate(title = "Rencontre", kind = DateKind.ANNIVERSARY, date = day, recurring = true, shared = true),
                )

            assertThat(dao.syncOf(id)!!.shared).isFalse()
            assertThat(
                repository
                    .observeAll()
                    .first()
                    .single()
                    .shared,
            ).isFalse()
        }

    @Test
    fun `deleting a shared date leaves a tombstone that is not listed`() =
        runTest {
            inDuo()
            val id =
                repository.save(
                    ImportantDate(title = "Rencontre", kind = DateKind.ANNIVERSARY, date = day, recurring = true, shared = true),
                )

            repository.delete(id)

            assertThat(dao.syncOf(id)!!.deleted).isTrue()
            assertThat(repository.observeAll().first()).isEmpty()
        }

    @Test
    fun `undoing the deletion of a shared date brings it back shared`() =
        runTest {
            inDuo()
            val date = ImportantDate(title = "Rencontre", kind = DateKind.ANNIVERSARY, date = day, recurring = true, shared = true)
            val id = repository.save(date)
            val uid = dao.syncOf(id)!!.uid
            repository.delete(id)

            repository.save(date.copy(id = id))

            val sync = dao.syncOf(id)!!
            assertThat(sync.deleted).isFalse()
            assertThat(sync.shared).isTrue()
            assertThat(sync.uid).isEqualTo(uid)
        }

    @Test
    fun `no longer sharing keeps the date here and sends a tombstone of the old one`() =
        runTest {
            inDuo()
            val date = ImportantDate(title = "Rencontre", kind = DateKind.ANNIVERSARY, date = day, recurring = true, shared = true)
            val id = repository.save(date)
            val oldUid = dao.syncOf(id)!!.uid

            repository.save(date.copy(id = id, shared = false))

            val kept = dao.syncOf(id)!!
            assertThat(kept.shared).isFalse()
            assertThat(kept.uid).isNotEqualTo(oldUid)
            val tombstone = dao.snapshot().single { it.id != id }.sync
            assertThat(tombstone.uid).isEqualTo(oldUid)
            assertThat(tombstone.deleted).isTrue()
            assertThat(tombstone.shared).isTrue()
            assertThat(repository.observeAll().first().map { it.id }).containsExactly(id)
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

        override fun observeAll(): Flow<List<ImportantDateEntity>> = state.map { rows -> rows.filterNot { it.sync.deleted } }

        override suspend fun find(id: Long): ImportantDateEntity? = state.value.firstOrNull { it.id == id }

        fun snapshot() = state.value

        override suspend fun syncOf(id: Long): SyncColumns? = state.value.firstOrNull { it.id == id }?.sync

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

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
