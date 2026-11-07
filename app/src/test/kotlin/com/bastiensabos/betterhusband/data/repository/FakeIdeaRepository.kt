package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.domain.model.Idea
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

class FakeIdeaRepository(
    initial: List<Idea> = emptyList(),
) : IdeaRepository {
    val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeAll(): Flow<List<Idea>> = state

    override suspend fun add(
        text: String,
        category: IdeaCategory,
    ): Long {
        val id = nextId++
        state.value = listOf(Idea(id, text.trim(), category, Instant.EPOCH.plusSeconds(id))) + state.value
        return id
    }

    override suspend fun updateContent(
        id: Long,
        text: String,
        category: IdeaCategory,
    ) {
        state.value = state.value.map { if (it.id == id) it.copy(text = text.trim(), category = category) else it }
    }

    override suspend fun setDone(
        id: Long,
        done: Boolean,
    ) {
        state.value = state.value.map { if (it.id == id) it.copy(done = done) else it }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }
}
