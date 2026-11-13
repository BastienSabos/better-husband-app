package com.bastiensabos.betterhusband.data.repository

import app.cash.turbine.test
import com.bastiensabos.betterhusband.data.local.dao.WishDao
import com.bastiensabos.betterhusband.data.local.entity.SyncColumns
import com.bastiensabos.betterhusband.data.local.entity.WishEntity
import com.bastiensabos.betterhusband.testutil.MutableClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class RoomWishRepositoryTest {
    private val dao = FakeWishDao()
    private val duo = FakeDuoRepository(DuoSession("duo-1", "me", ended = false))
    private val clock = MutableClock(Instant.parse("2027-03-10T09:00:00Z"))
    private val repository = RoomWishRepository(dao, duo, clock)

    @Test
    fun `a wish is shared, signed by me and stamped with the current time`() =
        runTest {
            repository.add("  Un livre  ")

            val row = dao.rows.value.single()
            assertThat(row.text).isEqualTo("Un livre")
            assertThat(row.owner).isEqualTo("me")
            assertThat(row.sync.shared).isTrue()
            assertThat(row.sync.author).isEqualTo("me")
            assertThat(row.sync.revision).isEqualTo(clock.millis())
            assertThat(row.sync.localChangedAt).isEqualTo(clock.millis())
            assertThat(row.sync.deleted).isFalse()
        }

    @Test
    fun `without a duo nothing is written and nothing is shown`() =
        runTest {
            duo.state.value = null

            repository.add("Un livre")

            assertThat(dao.rows.value).isEmpty()
            repository.observeMine().test { assertThat(awaitItem()).isEmpty() }
        }

    @Test
    fun `a blank wish is ignored and a long one is cut to the shared limit`() =
        runTest {
            repository.add("   ")
            repository.add("a".repeat(900))

            assertThat(
                dao.rows.value
                    .single()
                    .text,
            ).hasLength(500)
        }

    @Test
    fun `mine and theirs are told apart by the owner`() =
        runTest {
            repository.add("Le mien")
            dao.rows.value = dao.rows.value + wish(id = 50, owner = "other", text = "Le sien")

            repository.observeMine().test { assertThat(awaitItem().map { it.text }).containsExactly("Le mien") }
            repository.observeTheirs().test { assertThat(awaitItem().map { it.text }).containsExactly("Le sien") }
        }

    @Test
    fun `deleting keeps a tombstone with a newer revision and hides the wish`() =
        runTest {
            repository.add("Un livre")
            val id =
                dao.rows.value
                    .single()
                    .id
            val before =
                dao.rows.value
                    .single()
                    .sync.revision
            clock.instant = clock.instant.plusMillis(10)

            repository.delete(id)

            val row = dao.rows.value.single()
            assertThat(row.sync.deleted).isTrue()
            assertThat(row.sync.revision).isGreaterThan(before)
            assertThat(row.sync.localChangedAt).isEqualTo(clock.millis())
            repository.observeMine().test { assertThat(awaitItem()).isEmpty() }
        }

    @Test
    fun `the revision never goes back even if the clock does`() =
        runTest {
            repository.add("Un livre")
            val id =
                dao.rows.value
                    .single()
                    .id
            val before =
                dao.rows.value
                    .single()
                    .sync.revision
            clock.instant = clock.instant.minusMillis(5_000)

            repository.delete(id)

            assertThat(
                dao.rows.value
                    .single()
                    .sync.revision,
            ).isEqualTo(before + 1)
        }

    @Test
    fun `I cannot delete the other person's wish`() =
        runTest {
            dao.rows.value = listOf(wish(id = 7, owner = "other", text = "Le sien"))

            repository.delete(7)

            assertThat(
                dao.rows.value
                    .single()
                    .sync.deleted,
            ).isFalse()
        }

    private fun wish(
        id: Long,
        owner: String,
        text: String,
    ) = WishEntity(id = id, owner = owner, text = text, link = null, createdAt = 0, sync = SyncColumns(shared = true, revision = 1))

    private class FakeWishDao : WishDao {
        val rows = MutableStateFlow<List<WishEntity>>(emptyList())
        private var nextId = 1L

        // Comme la requête Room : les marqueurs de suppression ne sont pas listés.
        override fun observeAll(): Flow<List<WishEntity>> = rows.map { list -> list.filterNot { it.sync.deleted } }

        override suspend fun find(id: Long): WishEntity? = rows.value.firstOrNull { it.id == id }

        override suspend fun upsert(entity: WishEntity): Long {
            val id = if (entity.id != 0L) entity.id else nextId++
            rows.value = rows.value.filterNot { it.id == id } + entity.copy(id = id)
            return id
        }

        override suspend fun deleteAll() {
            rows.value = emptyList()
        }
    }
}
