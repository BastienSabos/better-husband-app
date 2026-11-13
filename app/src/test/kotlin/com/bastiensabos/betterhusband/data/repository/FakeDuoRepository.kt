package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.domain.duo.CoupleKeys
import com.bastiensabos.betterhusband.domain.duo.DuoJoined
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDuoRepository(
    initial: DuoSession? = null,
) : DuoRepository {
    val state = MutableStateFlow(initial)
    var joined: DuoJoined? = null
    var failOnJoin = false

    override fun observeSession(): Flow<DuoSession?> = state

    override suspend fun join(joined: DuoJoined) {
        check(!failOnJoin) { "Keystore indisponible" }
        this.joined = joined
        state.value = DuoSession(joined.duoId, joined.myMemberId, ended = false, partnerMemberId = joined.partnerMemberId)
    }

    override fun keys(): CoupleKeys? = joined?.keys

    override suspend fun deleteAll() {
        joined = null
        state.value = null
    }
}
