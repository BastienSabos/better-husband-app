package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.data.local.dao.HouseholdTaskDao
import com.bastiensabos.betterhusband.data.local.entity.HouseholdTaskEntity
import com.bastiensabos.betterhusband.data.local.entity.SyncColumns
import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Tâches récurrentes du foyer. L'ordre d'affichage (par échéance) est calculé par `TaskMath`. */
interface HouseholdTaskRepository {
    fun observeAll(): Flow<List<HouseholdTask>>

    /** Crée (id = 0) ou met à jour une tâche, et retourne son identifiant. */
    suspend fun save(task: HouseholdTask): Long

    /** Marque la tâche faite à [date] (par moi, en duo), ou « pas faite » avec `null` (annuler : rétablir l'ancienne date). */
    suspend fun setLastDone(
        id: Long,
        date: LocalDate?,
    )

    suspend fun delete(id: Long)

    suspend fun deleteAll()
}

@Singleton
class RoomHouseholdTaskRepository
    @Inject
    constructor(
        private val dao: HouseholdTaskDao,
        private val duo: DuoRepository,
    ) : HouseholdTaskRepository {
        override fun observeAll(): Flow<List<HouseholdTask>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun save(task: HouseholdTask): Long = dao.upsert(task.toEntity(if (task.id == 0L) null else dao.syncOf(task.id)))

        override suspend fun setLastDone(
            id: Long,
            date: LocalDate?,
        ) {
            // En duo, on retient qui l'a faite ; décocher efface aussi l'auteur.
            val by = if (date == null) null else duo.observeSession().first()?.memberId
            dao.setLastDone(id, date, by)
        }

        override suspend fun delete(id: Long) = dao.delete(id)

        override suspend fun deleteAll() = dao.deleteAll()
    }

internal fun HouseholdTaskEntity.toDomain() =
    HouseholdTask(
        id = id,
        title = title,
        // Rythme inconnu (base plus récente) : on garde la tâche en hebdomadaire plutôt que de la perdre.
        repeat = TaskRepeat.entries.firstOrNull { it.name == repeat } ?: TaskRepeat.WEEKLY,
        createdOn = createdOn,
        lastDoneOn = lastDoneOn,
        assignee = assignee,
        lastDoneBy = lastDoneBy,
    )

internal fun HouseholdTask.toEntity(sync: SyncColumns? = null) =
    HouseholdTaskEntity(
        id = id,
        title = title.trim(),
        repeat = repeat.name,
        createdOn = createdOn,
        lastDoneOn = lastDoneOn,
        assignee = assignee,
        lastDoneBy = lastDoneBy,
        sync = sync ?: SyncColumns(),
    )

@Module
@InstallIn(SingletonComponent::class)
abstract class HouseholdTaskRepositoryModule {
    @Binds
    abstract fun bindHouseholdTaskRepository(impl: RoomHouseholdTaskRepository): HouseholdTaskRepository
}
