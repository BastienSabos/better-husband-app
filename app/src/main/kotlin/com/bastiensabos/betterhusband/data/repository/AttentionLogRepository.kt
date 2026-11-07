package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.data.local.dao.AttentionLogDao
import com.bastiensabos.betterhusband.data.local.entity.AttentionLogEntity
import com.bastiensabos.betterhusband.domain.model.AttentionLogEntry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Historique des attentions proposées jour par jour : sert à ne pas se répéter et à compter la série. */
interface AttentionLogRepository {
    /** Du plus ancien au plus récent. */
    fun observeAll(): Flow<List<AttentionLogEntry>>

    /** Enregistre l'attention d'un jour ; si le jour en a déjà une, elle est remplacée. */
    suspend fun save(entry: AttentionLogEntry)

    /** Marque l'attention du jour faite (ou plus faite). Sans effet si le jour n'a pas d'attention. */
    suspend fun setDone(
        date: LocalDate,
        done: Boolean,
    )

    suspend fun deleteAll()
}

@Singleton
class RoomAttentionLogRepository
    @Inject
    constructor(
        private val dao: AttentionLogDao,
    ) : AttentionLogRepository {
        override fun observeAll(): Flow<List<AttentionLogEntry>> =
            dao.observeAll().map { list -> list.map { AttentionLogEntry(it.date, it.attentionId, it.done) } }

        override suspend fun save(entry: AttentionLogEntry) = dao.upsert(AttentionLogEntity(entry.date, entry.attentionId, entry.done))

        override suspend fun setDone(
            date: LocalDate,
            done: Boolean,
        ) = dao.setDone(date, done)

        override suspend fun deleteAll() = dao.deleteAll()
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class AttentionLogRepositoryModule {
    @Binds
    abstract fun bindAttentionLogRepository(impl: RoomAttentionLogRepository): AttentionLogRepository
}
