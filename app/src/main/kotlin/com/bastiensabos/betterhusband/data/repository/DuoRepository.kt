package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.data.local.dao.DuoDao
import com.bastiensabos.betterhusband.data.local.dao.DuoExitDao
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
import javax.inject.Inject
import javax.inject.Singleton

/** Le duo rejoint : qui je suis dedans, qui est l'autre, et si l'autre l'a quitté. */
data class DuoSession(
    val duoId: String,
    val memberId: String,
    val ended: Boolean,
    /** L'autre membre ; `null` pour un duo gardé avant que les identifiants ne soient tirés de la clé. */
    val partnerMemberId: String? = null,
)

/** Le duo de ce téléphone. Les données sont dans la base chiffrée, la clé de couple dans le Keystore. */
interface DuoRepository {
    /** `null` en solo. */
    fun observeSession(): Flow<DuoSession?>

    /** Garde un duo qui vient d'être rejoint, à la place d'un éventuel duo précédent. */
    suspend fun join(joined: DuoJoined)

    /** La clé de couple, ou `null` en solo (ou si le Keystore a perdu la clé). */
    fun keys(): CoupleKeys?

    /**
     * Quitte le duo sur ce téléphone : les tâches et les dates restent en solo, les envies et le duo disparaissent, la
     * clé de couple est effacée. L'effacement côté serveur viendra avec le vrai client (Cloud Function `leaveDuo`).
     */
    suspend fun leave()

    suspend fun deleteAll()
}

@Singleton
class RoomDuoRepository
    @Inject
    constructor(
        private val dao: DuoDao,
        private val wishes: WishDao,
        private val exit: DuoExitDao,
        private val vault: CoupleKeyVault,
    ) : DuoRepository {
        override fun observeSession(): Flow<DuoSession?> =
            combine(dao.observeMembers(), dao.observeState()) { members, state ->
                val me = members.firstOrNull { it.isMe }
                val partner = members.firstOrNull { !it.isMe }
                if (me == null || state == null) null else DuoSession(state.duoId, me.memberId, state.ended, partner?.memberId)
            }

        override suspend fun join(joined: DuoJoined) {
            // La clé d'abord : si le Keystore refuse, l'ancien duo reste intact.
            vault.save(joined.keys.exportRoot())
            dao.deleteMembers()
            dao.deleteState()
            dao.upsertMember(DuoMemberEntity(memberId = joined.myMemberId, isMe = true))
            dao.upsertMember(DuoMemberEntity(memberId = joined.partnerMemberId, isMe = false))
            dao.upsertState(DuoStateEntity(duoId = joined.duoId, lastSeen = 0, pushedUntil = 0, ended = false))
        }

        override fun keys(): CoupleKeys? = vault.load()?.let { CoupleKeys(it) }

        override suspend fun leave() {
            exit.leave()
            // Après la base : si la transaction échoue, le duo reste entier, clé comprise.
            vault.clear()
        }

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
