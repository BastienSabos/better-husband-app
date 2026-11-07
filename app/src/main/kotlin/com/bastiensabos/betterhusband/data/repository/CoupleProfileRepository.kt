package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.data.local.dao.CoupleProfileDao
import com.bastiensabos.betterhusband.data.local.entity.CoupleProfileEntity
import com.bastiensabos.betterhusband.domain.model.Budget
import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import com.bastiensabos.betterhusband.domain.model.Interest
import com.bastiensabos.betterhusband.domain.model.KidsAtHome
import com.bastiensabos.betterhusband.domain.model.LifeRhythm
import com.bastiensabos.betterhusband.domain.model.LivingArrangement
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Profil du couple ; un profil jamais enregistré vaut un profil vide (rien de répondu). */
interface CoupleProfileRepository {
    fun observe(): Flow<CoupleProfile>

    suspend fun save(profile: CoupleProfile)

    suspend fun deleteAll()
}

@Singleton
class RoomCoupleProfileRepository
    @Inject
    constructor(
        private val dao: CoupleProfileDao,
    ) : CoupleProfileRepository {
        override fun observe(): Flow<CoupleProfile> = dao.observe().map { it?.toDomain() ?: CoupleProfile() }

        override suspend fun save(profile: CoupleProfile) = dao.upsert(profile.toEntity())

        override suspend fun deleteAll() = dao.deleteAll()
    }

// Valeur inconnue (base écrite par une version plus récente) : traitée comme « pas répondu ».
private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? = enumValues<T>().firstOrNull { it.name == this }

internal fun CoupleProfileEntity.toDomain() =
    CoupleProfile(
        kids = kids.toEnumOrNull<KidsAtHome>(),
        living = living.toEnumOrNull<LivingArrangement>(),
        budget = budget.toEnumOrNull<Budget>(),
        rhythm = rhythm.toEnumOrNull<LifeRhythm>(),
        interests = interests.split(',').mapNotNull { it.toEnumOrNull<Interest>() }.toSet(),
    )

internal fun CoupleProfile.toEntity() =
    CoupleProfileEntity(
        kids = kids?.name,
        living = living?.name,
        budget = budget?.name,
        rhythm = rhythm?.name,
        interests = interests.map { it.name }.sorted().joinToString(","),
    )

@Module
@InstallIn(SingletonComponent::class)
abstract class CoupleProfileRepositoryModule {
    @Binds
    abstract fun bindCoupleProfileRepository(impl: RoomCoupleProfileRepository): CoupleProfileRepository
}
