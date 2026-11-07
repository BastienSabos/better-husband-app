package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.data.local.dao.IdeaDao
import com.bastiensabos.betterhusband.data.local.entity.IdeaEntity
import com.bastiensabos.betterhusband.domain.model.Idea
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Carnet d'idées. Source de vérité unique, local et chiffré. */
interface IdeaRepository {
    fun observeAll(): Flow<List<Idea>>

    /** Ajoute une idée datée de maintenant et retourne son identifiant. Le texte est nettoyé (espaces). */
    suspend fun add(
        text: String,
        category: IdeaCategory,
    ): Long

    suspend fun updateContent(
        id: Long,
        text: String,
        category: IdeaCategory,
    )

    suspend fun setDone(
        id: Long,
        done: Boolean,
    )

    suspend fun delete(id: Long)

    suspend fun deleteAll()
}

@Singleton
class RoomIdeaRepository
    @Inject
    constructor(
        private val dao: IdeaDao,
        private val clock: Clock,
    ) : IdeaRepository {
        override fun observeAll(): Flow<List<Idea>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun add(
            text: String,
            category: IdeaCategory,
        ): Long =
            dao.insert(
                IdeaEntity(text = text.trim(), category = category.name, createdAt = clock.millis(), done = false),
            )

        override suspend fun updateContent(
            id: Long,
            text: String,
            category: IdeaCategory,
        ) = dao.updateContent(id, text.trim(), category.name)

        override suspend fun setDone(
            id: Long,
            done: Boolean,
        ) = dao.setDone(id, done)

        override suspend fun delete(id: Long) = dao.delete(id)

        override suspend fun deleteAll() = dao.deleteAll()
    }

// Catégorie inconnue (base écrite par une version plus récente) : on garde l'idée, rangée en « attentions ».
internal fun IdeaEntity.toDomain() =
    Idea(
        id = id,
        text = text,
        category = IdeaCategory.entries.firstOrNull { it.name == category } ?: IdeaCategory.ATTENTION,
        createdAt = Instant.ofEpochMilli(createdAt),
        done = done,
    )

@Module
@InstallIn(SingletonComponent::class)
abstract class IdeaRepositoryModule {
    @Binds
    abstract fun bindIdeaRepository(impl: RoomIdeaRepository): IdeaRepository
}
