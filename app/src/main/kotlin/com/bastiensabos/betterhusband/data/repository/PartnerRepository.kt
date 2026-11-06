package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.data.local.dao.PartnerDao
import com.bastiensabos.betterhusband.data.local.entity.PartnerEntity
import com.bastiensabos.betterhusband.data.local.entity.RelativeEntity
import com.bastiensabos.betterhusband.data.local.entity.SizeEntity
import com.bastiensabos.betterhusband.data.local.entity.TasteEntity
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.domain.model.Relative
import com.bastiensabos.betterhusband.domain.model.SizeKind
import com.bastiensabos.betterhusband.domain.model.Taste
import com.bastiensabos.betterhusband.domain.model.TasteCategory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
        override fun observePartner(): Flow<Partner?> =
            combine(
                dao.observePartner(),
                dao.observeTastes(),
                dao.observeSizes(),
                dao.observeRelatives(),
            ) { partner, tastes, sizes, relatives ->
                partner?.toDomain(tastes, sizes, relatives)
            }

        override suspend fun savePartner(partner: Partner) {
            dao.replaceProfile(
                partner = partner.toEntity(),
                tastes = partner.tastes.map { TasteEntity(category = it.category.name, label = it.label) },
                sizes = partner.sizes.map { (kind, value) -> SizeEntity(kind = kind.name, value = value) },
                relatives =
                    partner.relatives.map {
                        RelativeEntity(relation = it.relation, name = it.name, birthday = it.birthday)
                    },
            )
        }

        override suspend fun deletePartner() = dao.deleteAll()
    }

// Valeurs inconnues (base écrite par une version plus récente) : on les ignore ou on retombe sur le neutre
// plutôt que de planter.
internal fun PartnerEntity.toDomain(
    tastes: List<TasteEntity>,
    sizes: List<SizeEntity>,
    relatives: List<RelativeEntity>,
) = Partner(
    firstName = firstName,
    addressForm = AddressForm.entries.firstOrNull { it.name == addressForm } ?: AddressForm.NEUTRAL,
    birthday = birthday,
    relationshipStart = relationshipStart,
    tastes =
        tastes.mapNotNull { entity ->
            TasteCategory.entries.firstOrNull { it.name == entity.category }?.let { Taste(it, entity.label) }
        },
    sizes =
        sizes
            .mapNotNull { entity -> SizeKind.entries.firstOrNull { it.name == entity.kind }?.let { it to entity.value } }
            .toMap(),
    relatives = relatives.map { Relative(name = it.name, relation = it.relation, birthday = it.birthday) },
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
