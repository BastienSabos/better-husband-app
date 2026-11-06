package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.data.local.dao.PartnerDao
import com.bastiensabos.betterhusband.data.local.entity.PartnerEntity
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.Partner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Source de vérité de la fiche partenaire. `null` tant que l'onboarding n'est pas terminé. */
interface PartnerRepository {
    fun observePartner(): Flow<Partner?>

    suspend fun savePartner(partner: Partner)

    suspend fun deletePartner()
}

@Singleton
class RoomPartnerRepository
    @Inject
    constructor(
        private val dao: PartnerDao,
    ) : PartnerRepository {
        override fun observePartner(): Flow<Partner?> = dao.observe().map { it?.toDomain() }

        override suspend fun savePartner(partner: Partner) = dao.upsert(partner.toEntity())

        override suspend fun deletePartner() = dao.delete()
    }

internal fun PartnerEntity.toDomain() =
    Partner(
        firstName = firstName,
        // Valeur inconnue (base plus récente) : on retombe sur le neutre plutôt que de planter.
        addressForm = AddressForm.entries.firstOrNull { it.name == addressForm } ?: AddressForm.NEUTRAL,
        birthday = birthday,
        relationshipStart = relationshipStart,
    )

internal fun Partner.toEntity() =
    PartnerEntity(
        firstName = firstName.trim(),
        addressForm = addressForm.name,
        birthday = birthday,
        relationshipStart = relationshipStart,
    )

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindPartnerRepository(impl: RoomPartnerRepository): PartnerRepository
}
