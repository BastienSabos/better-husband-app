package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

class FakeHouseholdTaskRepository(
    initial: List<HouseholdTask> = emptyList(),
) : HouseholdTaskRepository {
    val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeAll(): Flow<List<HouseholdTask>> = state

    override suspend fun save(task: HouseholdTask): Long {
        val id = if (task.id == 0L) nextId++ else task.id
        state.value = state.value.filterNot { it.id == id } + task.copy(id = id, title = task.title.trim())
        return id
    }

    override suspend fun setLastDone(
        id: Long,
        date: LocalDate?,
    ) {
        state.value = state.value.map { if (it.id == id) it.copy(lastDoneOn = date) else it }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }
}
