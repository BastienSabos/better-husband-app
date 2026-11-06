package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.domain.model.Partner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePartnerRepository(
    initial: Partner? = null,
) : PartnerRepository {
    val state = MutableStateFlow(initial)

    override fun observePartner(): Flow<Partner?> = state

    override suspend fun savePartner(partner: Partner) {
        state.value = partner
    }

    override suspend fun deletePartner() {
        state.value = null
    }
}
