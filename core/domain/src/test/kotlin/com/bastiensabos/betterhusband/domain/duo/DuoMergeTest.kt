package com.bastiensabos.betterhusband.domain.duo

import com.bastiensabos.betterhusband.domain.model.SharedRecord
import com.bastiensabos.betterhusband.domain.model.SyncMeta
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class DuoMergeTest {
    private val uid = "0f8b2c1e-6a4d-4c3b-9e2f-1a2b3c4d5e6f"

    private fun task(
        revision: Long,
        author: String = "alex",
        title: String = "Sortir les poubelles",
        deleted: Boolean = false,
    ) = SharedRecord.Task(
        meta = SyncMeta(uid, revision, author, deleted),
        title = title,
        repeat = TaskRepeat.WEEKLY,
        createdOn = LocalDate.of(2027, 2, 15),
    )

    @Test
    fun `a revision moves forward with the clock`() {
        assertThat(Revision.next(nowMillis = 1_000, previous = null)).isEqualTo(1_000)
        assertThat(Revision.next(nowMillis = 2_000, previous = 1_000)).isEqualTo(2_000)
    }

    @Test
    fun `a revision never goes back when the clock does`() {
        assertThat(Revision.next(nowMillis = 500, previous = 1_000)).isEqualTo(1_001)
        assertThat(Revision.next(nowMillis = 1_000, previous = 1_000)).isEqualTo(1_001)
    }

    @Test
    fun `an unknown record is taken as is`() {
        assertThat(DuoMerge.incomingWins(null, task(1))).isTrue()
        assertThat(DuoMerge.merge(null, task(1))).isEqualTo(task(1))
    }

    @Test
    fun `the latest change wins`() {
        val older = task(1, title = "Sortir les poubelles")
        val newer = task(2, author = "camille", title = "Sortir le tri")

        assertThat(DuoMerge.merge(older, newer)).isEqualTo(newer)
        assertThat(DuoMerge.merge(newer, older)).isEqualTo(newer)
    }

    @Test
    fun `on equal revisions the author breaks the tie the same way on both phones`() {
        val fromAlex = task(5, author = "alex", title = "A")
        val fromCamille = task(5, author = "camille", title = "C")

        assertThat(DuoMerge.merge(fromAlex, fromCamille)).isEqualTo(fromCamille)
        assertThat(DuoMerge.merge(fromCamille, fromAlex)).isEqualTo(fromCamille)
    }

    @Test
    fun `receiving the same block twice changes nothing`() {
        val record = task(3)

        assertThat(DuoMerge.incomingWins(record, record)).isFalse()
        assertThat(DuoMerge.merge(DuoMerge.merge(null, record), record)).isEqualTo(record)
    }

    @Test
    fun `a deletion with the same revision and author wins so both phones converge`() {
        val kept = task(4)
        val deleted = task(4, deleted = true)

        assertThat(DuoMerge.merge(kept, deleted)).isEqualTo(deleted)
        assertThat(DuoMerge.merge(deleted, kept)).isEqualTo(deleted)
    }

    @Test
    fun `a newer edit brings back a record deleted earlier`() {
        val deleted = task(4, deleted = true)
        val edited = task(6, author = "camille")

        assertThat(DuoMerge.merge(deleted, edited)).isEqualTo(edited)
    }

    @Test
    fun `merging two different records is a bug`() {
        val other = task(1).copy(meta = SyncMeta("11111111-2222-4333-8444-555555555555", 1, "alex"))

        assertThrows(IllegalArgumentException::class.java) { DuoMerge.merge(task(1), other) }
    }
}
