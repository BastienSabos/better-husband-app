package com.bastiensabos.betterhusband.domain.duo

import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.SharedRecord
import com.bastiensabos.betterhusband.domain.model.SyncMeta
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DuoRulesTest {
    private val now = 1_800_000_000_000L
    private val members = setOf("alex", "camille")
    private var next = 0

    private fun meta(
        author: String = "camille",
        revision: Long = now,
        deleted: Boolean = false,
    ) = SyncMeta("00000000-0000-4000-8000-%012d".format(next++), revision, author, deleted)

    private fun wish(
        owner: String = "camille",
        text: String = "Un carnet de croquis",
        link: String? = null,
        author: String = owner,
        deleted: Boolean = false,
    ) = SharedRecord.Wish(meta(author, deleted = deleted), owner, text, link)

    private fun task(
        title: String = "Arroser les plantes",
        assignee: String? = null,
        lastDoneBy: String? = null,
        createdOn: LocalDate = LocalDate.of(2027, 2, 15),
    ) = SharedRecord.Task(meta(), title, TaskRepeat.WEEKLY, createdOn, assignee = assignee, lastDoneBy = lastDoneBy)

    private fun date(day: LocalDate = LocalDate.of(2019, 6, 1)) =
        SharedRecord.Date(meta(), "Notre rencontre", DateKind.ANNIVERSARY, day, recurring = true)

    private fun check(record: SharedRecord) = DuoLimits.check(record, members, now)

    @Test
    fun `well formed records pass`() {
        val batch = listOf(wish(link = "https://exemple.fr/carnet"), task(assignee = "alex"), date())

        assertThat(DuoLimits.check(batch, members, now)).isNull()
    }

    @Test
    fun `a uid that is not a lowercase uuid is refused`() {
        val record = wish().copy(meta = SyncMeta("../../idea/1", now, "camille"))

        assertThat(check(record)).isEqualTo(DuoRejection.INVALID_UID)
    }

    @Test
    fun `an author outside the duo is refused`() {
        assertThat(check(task().copy(meta = meta(author = "inconnu")))).isEqualTo(DuoRejection.UNKNOWN_MEMBER)
        assertThat(check(task(assignee = "inconnu"))).isEqualTo(DuoRejection.UNKNOWN_MEMBER)
        assertThat(check(task(lastDoneBy = "inconnu"))).isEqualTo(DuoRejection.UNKNOWN_MEMBER)
    }

    @Test
    fun `a revision more than a day ahead of our clock is refused`() {
        val day = DuoLimits.MAX_CLOCK_AHEAD_MILLIS

        assertThat(check(task().copy(meta = meta(revision = now + day)))).isNull()
        assertThat(check(task().copy(meta = meta(revision = now + day + 1))))
            .isEqualTo(DuoRejection.REVISION_OUT_OF_RANGE)
        assertThat(check(task().copy(meta = meta(revision = -1)))).isEqualTo(DuoRejection.REVISION_OUT_OF_RANGE)
    }

    @Test
    fun `texts longer than the limit are refused`() {
        val long = "a".repeat(DuoLimits.MAX_TEXT + 1)

        assertThat(check(wish(text = long))).isEqualTo(DuoRejection.TEXT_TOO_LONG)
        assertThat(check(task(title = long))).isEqualTo(DuoRejection.TEXT_TOO_LONG)
        assertThat(check(date().copy(title = long))).isEqualTo(DuoRejection.TEXT_TOO_LONG)
    }

    @Test
    fun `years outside the bounds are refused`() {
        assertThat(check(date(LocalDate.of(1899, 12, 31)))).isEqualTo(DuoRejection.YEAR_OUT_OF_RANGE)
        assertThat(check(task(createdOn = LocalDate.of(2201, 1, 1)))).isEqualTo(DuoRejection.YEAR_OUT_OF_RANGE)
    }

    @Test
    fun `only https links without spaces are kept on a wish`() {
        assertThat(check(wish(link = "http://exemple.fr"))).isEqualTo(DuoRejection.INVALID_LINK)
        assertThat(check(wish(link = "javascript:alert(1)"))).isEqualTo(DuoRejection.INVALID_LINK)
        assertThat(check(wish(link = "https://exemple.fr/a b"))).isEqualTo(DuoRejection.INVALID_LINK)
        assertThat(check(wish(link = "https://" + "a".repeat(DuoLimits.MAX_LINK)))).isEqualTo(DuoRejection.INVALID_LINK)
    }

    @Test
    fun `only the owner of a wish can change it`() {
        assertThat(check(wish(owner = "alex", author = "camille"))).isEqualTo(DuoRejection.WISH_EDITED_BY_OTHER)
    }

    @Test
    fun `a batch with the same record twice is refused`() {
        val record = task()

        assertThat(DuoLimits.check(listOf(record, record), members, now)).isEqualTo(DuoRejection.DUPLICATE_UID)
    }

    @Test
    fun `one bad record rejects the whole batch`() {
        val batch = listOf(task(), wish(text = "a".repeat(DuoLimits.MAX_TEXT + 1)))

        assertThat(DuoLimits.check(batch, members, now)).isEqualTo(DuoRejection.TEXT_TOO_LONG)
    }

    @Test
    fun `a batch above the record limit is refused`() {
        val record = task()
        val batch = List(DuoLimits.MAX_RECORDS + 1) { record }

        assertThat(DuoLimits.check(batch, members, now)).isEqualTo(DuoRejection.TOO_MANY_RECORDS)
    }

    @Test
    fun `the outbox resends writes from the last push included`() {
        val changedAt = mapOf("a" to 90L, "b" to 100L, "c" to 120L)

        assertThat(DuoOutbox.pending(changedAt.keys.toList(), pushedUntil = 100) { changedAt.getValue(it) })
            .containsExactly("b", "c")
    }

    @Test
    fun `leaving keeps my wishes, tasks and dates and drops the other's wishes`() {
        val mine = wish(owner = "alex")
        val theirs = wish(owner = "camille")
        val chore = task(assignee = "camille", lastDoneBy = "alex")
        val anniversary = date()

        val plan = DuoExit.plan(listOf(mine, theirs, chore, anniversary), me = "alex")

        assertThat(plan.keep).containsExactly(mine, chore.copy(assignee = null, lastDoneBy = null), anniversary)
        assertThat(plan.drop).containsExactly(theirs)
    }

    @Test
    fun `leaving drops deletion markers`() {
        val deleted = wish(owner = "alex", deleted = true)

        assertThat(DuoExit.plan(listOf(deleted), me = "alex")).isEqualTo(ExitPlan(keep = emptyList(), drop = listOf(deleted)))
    }
}
