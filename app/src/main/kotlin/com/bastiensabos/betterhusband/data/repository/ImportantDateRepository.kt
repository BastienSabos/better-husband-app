package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.data.local.dao.ImportantDateDao
import com.bastiensabos.betterhusband.data.local.entity.ImportantDateEntity
import com.bastiensabos.betterhusband.data.local.entity.SyncColumns
import com.bastiensabos.betterhusband.domain.duo.Revision
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Dates saisies par l'utilisateur. Les dates de la fiche partenaire restent dans [PartnerRepository]. */
interface ImportantDateRepository {
    fun observeAll(): Flow<List<ImportantDate>>

    /**
     * Crée (id = 0) ou met à jour une date, et retourne son identifiant. [ImportantDate.shared] ne compte qu'en duo ;
     * ne plus partager une date la garde ici et la retire chez l'autre.
     */
    suspend fun save(date: ImportantDate): Long

    /** Supprime ; une date partagée garde un marqueur pour disparaître aussi chez l'autre. */
    suspend fun delete(id: Long)

    suspend fun deleteAll()
}

@Singleton
class RoomImportantDateRepository
    @Inject
    constructor(
        private val dao: ImportantDateDao,
        private val duo: DuoRepository,
        private val clock: Clock,
    ) : ImportantDateRepository {
        override fun observeAll(): Flow<List<ImportantDate>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun save(date: ImportantDate): Long {
            val before = if (date.id == 0L) null else dao.syncOf(date.id)
            val me = duo.observeSession().first()?.memberId
            val share = date.shared && me != null
            val wasShared = before?.shared == true && !before.deleted
            val now = clock.millis()
            val sync =
                when {
                    // Partagée (ou qui le reste) : nouvelle révision signée, à envoyer.
                    share -> {
                        (before ?: SyncColumns()).copy(
                            shared = true,
                            deleted = false,
                            revision = Revision.next(now, before?.revision),
                            author = me,
                            localChangedAt = now,
                        )
                    }

                    // Plus partagée : la date reprend un nouvel uid ici, l'ancien part comme marqueur de suppression.
                    wasShared -> {
                        SyncColumns()
                    }

                    else -> {
                        before?.copy(deleted = false, shared = false) ?: SyncColumns()
                    }
                }
            val id = dao.upsert(date.toEntity(sync))
            if (!share && wasShared && before != null) {
                dao.upsert(date.toEntity(before.tombstone(now, me)).copy(id = 0))
            }
            return id
        }

        override suspend fun delete(id: Long) {
            val entity = dao.find(id)
            if (entity == null || !entity.sync.shared) {
                dao.delete(id)
                return
            }
            val me = duo.observeSession().first()?.memberId
            dao.upsert(entity.copy(sync = entity.sync.tombstone(clock.millis(), me)))
        }

        override suspend fun deleteAll() = dao.deleteAll()

        private fun SyncColumns.tombstone(
            now: Long,
            me: String?,
        ) = copy(deleted = true, revision = Revision.next(now, revision), author = me ?: author, localChangedAt = now)
    }

internal fun ImportantDateEntity.toDomain() =
    ImportantDate(
        id = id,
        title = title,
        // Genre inconnu (base plus récente) : on garde la date plutôt que de la perdre.
        kind = DateKind.entries.firstOrNull { it.name == kind } ?: DateKind.OTHER,
        date = date,
        recurring = recurring,
        shared = sync.shared,
    )

internal fun ImportantDate.toEntity(sync: SyncColumns? = null) =
    ImportantDateEntity(
        id = id,
        title = title.trim(),
        kind = kind.name,
        date = date,
        recurring = recurring,
        sync = sync ?: SyncColumns(),
    )

@Module
@InstallIn(SingletonComponent::class)
abstract class ImportantDateRepositoryModule {
    @Binds
    abstract fun bindImportantDateRepository(impl: RoomImportantDateRepository): ImportantDateRepository
}
