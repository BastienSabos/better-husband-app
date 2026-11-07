package com.bastiensabos.betterhusband.data.repository

class FakeAttentionBank(
    override val size: Int = 150,
) : AttentionBank {
    override fun text(id: Int): String = "Attention $id"
}
