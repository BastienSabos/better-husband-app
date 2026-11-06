package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.domain.model.ImportantDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeImportantDateRepository(
    initial: List<ImportantDate> = emptyList(),
) : ImportantDateRepository {
    val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeAll(): Flow<List<ImportantDate>> = state

    override suspend fun save(date: ImportantDate): Long {
        val id = if (date.id == 0L) nextId++ else date.id
        state.value = state.value.filterNot { it.id == id } + date.copy(id = id)
        return id
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }
}
