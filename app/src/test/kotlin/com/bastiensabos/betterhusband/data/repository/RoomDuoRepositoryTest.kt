package com.bastiensabos.betterhusband.data.repository

import app.cash.turbine.test
import com.bastiensabos.betterhusband.data.local.dao.DuoDao
import com.bastiensabos.betterhusband.data.local.dao.WishDao
import com.bastiensabos.betterhusband.data.local.entity.DuoMemberEntity
import com.bastiensabos.betterhusband.data.local.entity.DuoStateEntity
import com.bastiensabos.betterhusband.data.local.entity.WishEntity
import com.bastiensabos.betterhusband.data.security.FakeCoupleKeyVault
import com.bastiensabos.betterhusband.domain.duo.CoupleKeys
import com.bastiensabos.betterhusband.domain.duo.DuoJoined
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RoomDuoRepositoryTest {
    private val dao = FakeDuoDao()
    private val wishDao = FakeWishDao()
    private val vault = FakeCoupleKeyVault()
    private val repository = RoomDuoRepository(dao, wishDao, vault)
    private val keys = CoupleKeys(ByteArray(32) { it.toByte() })

    @Test
    fun `solo has no session and no key`() =
        runTest {
            repository.observeSession().test { assertThat(awaitItem()).isNull() }
            assertThat(repository.keys()).isNull()
        }

    @Test
    fun `joining keeps the duo, a member id for me and the key in the vault`() =
        runTest {
            repository.join(DuoJoined("duo-1", keys))

            repository.observeSession().test {
                val session = awaitItem()!!
                assertThat(session.duoId).isEqualTo("duo-1")
                assertThat(session.memberId).isNotEmpty()
                assertThat(session.ended).isFalse()
            }
            assertThat(repository.keys()!!.verificationCode()).isEqualTo(keys.verificationCode())
        }

    @Test
    fun `the key never lands in the database rows`() =
        runTest {
            repository.join(DuoJoined("duo-1", keys))

            assertThat(dao.members.value.toString() + dao.state.value.toString()).doesNotContain(keys.exportRoot().toList().toString())
            assertThat(vault.stored).isEqualTo(keys.exportRoot())
        }

    @Test
    fun `joining again replaces the previous duo`() =
        runTest {
            repository.join(DuoJoined("duo-1", keys))
            val first =
                dao.members.value
                    .single()
                    .memberId

            repository.join(DuoJoined("duo-2", CoupleKeys(ByteArray(32) { 7 })))

            repository.observeSession().test {
                val session = awaitItem()!!
                assertThat(session.duoId).isEqualTo("duo-2")
                assertThat(session.memberId).isNotEqualTo(first)
            }
            assertThat(dao.members.value).hasSize(1)
        }

    @Test
    fun `delete all forgets the duo and the key`() =
        runTest {
            repository.join(DuoJoined("duo-1", keys))

            repository.deleteAll()

            repository.observeSession().test { assertThat(awaitItem()).isNull() }
            assertThat(vault.stored).isNull()
            assertThat(repository.keys()).isNull()
        }

    @Test
    fun `a lost key makes keys null without failing`() =
        runTest {
            repository.join(DuoJoined("duo-1", keys))
            vault.stored = null

            assertThat(repository.keys()).isNull()
        }

    @Test
    fun `a keystore refusal keeps the previous duo untouched`() =
        runTest {
            repository.join(DuoJoined("duo-1", keys))
            vault.failOnSave = true

            runCatching { repository.join(DuoJoined("duo-2", CoupleKeys(ByteArray(32) { 7 }))) }

            repository.observeSession().test { assertThat(awaitItem()!!.duoId).isEqualTo("duo-1") }
        }

    @Test
    fun `delete all also empties the wishes`() =
        runTest {
            wishDao.rows = 3

            repository.deleteAll()

            assertThat(wishDao.rows).isEqualTo(0)
        }

    private class FakeWishDao : WishDao {
        var rows = 0

        override fun observeAll(): Flow<List<WishEntity>> = MutableStateFlow(emptyList())

        override suspend fun find(id: Long): WishEntity? = null

        override suspend fun upsert(entity: WishEntity): Long = 0

        override suspend fun deleteAll() {
            rows = 0
        }
    }

    private class FakeDuoDao : DuoDao {
        val members = MutableStateFlow<List<DuoMemberEntity>>(emptyList())
        val state = MutableStateFlow<DuoStateEntity?>(null)

        override fun observeMembers(): Flow<List<DuoMemberEntity>> = members

        override fun observeState(): Flow<DuoStateEntity?> = state

        override suspend fun upsertMember(entity: DuoMemberEntity) {
            members.value = members.value.filterNot { it.memberId == entity.memberId } + entity
        }

        override suspend fun upsertState(entity: DuoStateEntity) {
            state.value = entity
        }

        override suspend fun deleteMembers() {
            members.value = emptyList()
        }

        override suspend fun deleteState() {
            state.value = null
        }
    }
}
