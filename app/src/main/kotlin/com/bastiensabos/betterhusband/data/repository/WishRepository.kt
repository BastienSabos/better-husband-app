package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.data.local.dao.WishDao
import com.bastiensabos.betterhusband.data.local.entity.SyncColumns
import com.bastiensabos.betterhusband.data.local.entity.WishEntity
import com.bastiensabos.betterhusband.domain.duo.DuoLimits
import com.bastiensabos.betterhusband.domain.duo.Revision
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Une envie telle que l'écran l'affiche. */
data class Wish(
    val id: Long,
    val text: String,
    val createdAt: Long,
)

/** Les envies du duo : les miennes, que l'autre voit, et les siennes, que je vois en lecture seule. */
interface WishRepository {
    /** Vide en solo. */
    fun observeMine(): Flow<List<Wish>>

    /** Vide en solo, et tant que rien n'est arrivé de l'autre téléphone. */
    fun observeTheirs(): Flow<List<Wish>>

    /** Note une envie. Sans duo rejoint, ou texte vide, ne fait rien. */
    suspend fun add(text: String)

    /** Retire une de mes envies : elle reste en marqueur de suppression jusqu'à ce que l'autre l'ait reçue. */
    suspend fun delete(id: Long)
}

@Singleton
class RoomWishRepository
    @Inject
    constructor(
        private val dao: WishDao,
        private val duo: DuoRepository,
        private val clock: Clock,
    ) : WishRepository {
        override fun observeMine(): Flow<List<Wish>> = observe { owner, me -> owner == me }

        override fun observeTheirs(): Flow<List<Wish>> = observe { owner, me -> owner != me }

        private fun observe(keep: (owner: String, me: String) -> Boolean): Flow<List<Wish>> =
            combine(duo.observeSession(), dao.observeAll()) { session, rows ->
                if (session == null) {
                    emptyList()
                } else {
                    rows.filter { keep(it.owner, session.memberId) }.map { Wish(it.id, it.text, it.createdAt) }
                }
            }

        override suspend fun add(text: String) {
            val me = duo.observeSession().first()?.memberId ?: return
            val clean = text.trim().take(DuoLimits.MAX_TEXT)
            if (clean.isEmpty()) return
            val now = clock.millis()
            dao.upsert(
                WishEntity(
                    owner = me,
                    text = clean,
                    link = null,
                    createdAt = now,
                    sync = SyncColumns(shared = true, revision = Revision.next(now, null), author = me, localChangedAt = now),
                ),
            )
        }

        override suspend fun delete(id: Long) {
            val me = duo.observeSession().first()?.memberId ?: return
            val wish = dao.find(id)?.takeIf { it.owner == me } ?: return
            val now = clock.millis()
            dao.upsert(
                wish.copy(
                    sync =
                        wish.sync.copy(
                            deleted = true,
                            revision = Revision.next(now, wish.sync.revision),
                            author = me,
                            localChangedAt = now,
                        ),
                ),
            )
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class WishRepositoryModule {
    @Binds
    abstract fun bindWishRepository(impl: RoomWishRepository): WishRepository
}
