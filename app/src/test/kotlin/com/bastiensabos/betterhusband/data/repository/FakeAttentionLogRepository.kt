package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.domain.model.AttentionLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

class FakeAttentionLogRepository(
    initial: List<AttentionLogEntry> = emptyList(),
) : AttentionLogRepository {
    val state = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<AttentionLogEntry>> = state

    override suspend fun save(entry: AttentionLogEntry) {
        state.value = (state.value.filterNot { it.date == entry.date } + entry).sortedBy { it.date }
    }

    override suspend fun setDone(
        date: LocalDate,
        done: Boolean,
    ) {
        state.value = state.value.map { if (it.date == date) it.copy(done = done) else it }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }
}
