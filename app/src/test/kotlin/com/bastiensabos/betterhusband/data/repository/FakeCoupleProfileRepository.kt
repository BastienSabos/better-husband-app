package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCoupleProfileRepository(
    initial: CoupleProfile = CoupleProfile(),
) : CoupleProfileRepository {
    val state = MutableStateFlow(initial)

    override fun observe(): Flow<CoupleProfile> = state

    override suspend fun save(profile: CoupleProfile) {
        state.value = profile
    }

    override suspend fun deleteAll() {
        state.value = CoupleProfile()
    }
}
