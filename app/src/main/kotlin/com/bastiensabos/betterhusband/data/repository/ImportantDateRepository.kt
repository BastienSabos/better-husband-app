package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.data.local.dao.ImportantDateDao
import com.bastiensabos.betterhusband.data.local.entity.ImportantDateEntity
import com.bastiensabos.betterhusband.data.local.entity.SyncColumns
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Dates saisies par l'utilisateur. Les dates de la fiche partenaire restent dans [PartnerRepository]. */
interface ImportantDateRepository {
    fun observeAll(): Flow<List<ImportantDate>>

    /** Crée (id = 0) ou met à jour une date, et retourne son identifiant. */
    suspend fun save(date: ImportantDate): Long

    suspend fun delete(id: Long)

    suspend fun deleteAll()
}

@Singleton
class RoomImportantDateRepository
    @Inject
    constructor(
        private val dao: ImportantDateDao,
    ) : ImportantDateRepository {
        override fun observeAll(): Flow<List<ImportantDate>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun save(date: ImportantDate): Long = dao.upsert(date.toEntity(if (date.id == 0L) null else dao.syncOf(date.id)))

        override suspend fun delete(id: Long) = dao.delete(id)

        override suspend fun deleteAll() = dao.deleteAll()
    }

internal fun ImportantDateEntity.toDomain() =
    ImportantDate(
        id = id,
        title = title,
        // Genre inconnu (base plus récente) : on garde la date plutôt que de la perdre.
        kind = DateKind.entries.firstOrNull { it.name == kind } ?: DateKind.OTHER,
        date = date,
        recurring = recurring,
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
