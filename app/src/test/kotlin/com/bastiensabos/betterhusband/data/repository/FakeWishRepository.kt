package com.bastiensabos.betterhusband.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeWishRepository(
    mine: List<Wish> = emptyList(),
    theirs: List<Wish> = emptyList(),
) : WishRepository {
    val mine = MutableStateFlow(mine)
    val theirs = MutableStateFlow(theirs)
    private var nextId = 100L

    override fun observeMine(): Flow<List<Wish>> = mine

    override fun observeTheirs(): Flow<List<Wish>> = theirs

    override suspend fun add(text: String) {
        mine.value = mine.value + Wish(nextId++, text.trim(), 0)
    }

    override suspend fun delete(id: Long) {
        mine.value = mine.value.filterNot { it.id == id }
    }
}
