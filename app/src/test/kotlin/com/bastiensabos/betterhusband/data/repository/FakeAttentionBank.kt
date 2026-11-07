package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.domain.AttentionGroup

class FakeAttentionBank(
    override val groupSizes: Map<AttentionGroup, Int> = mapOf(AttentionGroup.EVERYONE to 150),
) : AttentionBank {
    override fun text(id: Int): String = "Attention $id"
}
