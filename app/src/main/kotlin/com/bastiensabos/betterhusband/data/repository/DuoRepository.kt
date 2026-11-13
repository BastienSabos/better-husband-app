package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.data.local.dao.DuoDao
import com.bastiensabos.betterhusband.data.local.dao.WishDao
import com.bastiensabos.betterhusband.data.local.entity.DuoMemberEntity
import com.bastiensabos.betterhusband.data.local.entity.DuoStateEntity
import com.bastiensabos.betterhusband.data.security.CoupleKeyVault
import com.bastiensabos.betterhusband.domain.duo.CoupleKeys
import com.bastiensabos.betterhusband.domain.duo.DuoJoined
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Le duo rejoint : qui je suis dedans, et si l'autre l'a quitté. L'autre membre n'est connu qu'à son premier bloc. */
data class DuoSession(
    val duoId: String,
    val memberId: String,
    val ended: Boolean,
)

/** Le duo de ce téléphone. Les données sont dans la base chiffrée, la clé de couple dans le Keystore. */
interface DuoRepository {
    /** `null` en solo. */
    fun observeSession(): Flow<DuoSession?>

    /** Garde un duo qui vient d'être rejoint, à la place d'un éventuel duo précédent. */
    suspend fun join(joined: DuoJoined)

    /** La clé de couple, ou `null` en solo (ou si le Keystore a perdu la clé). */
    fun keys(): CoupleKeys?

    suspend fun deleteAll()
}

@Singleton
class RoomDuoRepository
    @Inject
    constructor(
        private val dao: DuoDao,
        private val wishes: WishDao,
        private val vault: CoupleKeyVault,
    ) : DuoRepository {
        override fun observeSession(): Flow<DuoSession?> =
            combine(dao.observeMembers(), dao.observeState()) { members, state ->
                val me = members.firstOrNull { it.isMe }
                if (me == null || state == null) null else DuoSession(state.duoId, me.memberId, state.ended)
            }

        override suspend fun join(joined: DuoJoined) {
            // La clé d'abord : si le Keystore refuse, l'ancien duo reste intact.
            vault.save(joined.keys.exportRoot())
            dao.deleteMembers()
            dao.deleteState()
            dao.upsertMember(DuoMemberEntity(memberId = UUID.randomUUID().toString(), isMe = true))
            dao.upsertState(DuoStateEntity(duoId = joined.duoId, lastSeen = 0, pushedUntil = 0, ended = false))
        }

        override fun keys(): CoupleKeys? = vault.load()?.let { CoupleKeys(it) }

        override suspend fun deleteAll() {
            wishes.deleteAll()
            dao.deleteMembers()
            dao.deleteState()
            vault.clear()
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class DuoRepositoryModule {
    @Binds
    abstract fun bindDuoRepository(impl: RoomDuoRepository): DuoRepository
}
