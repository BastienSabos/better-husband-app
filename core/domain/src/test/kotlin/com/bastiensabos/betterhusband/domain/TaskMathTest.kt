package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class TaskMathTest {
    private val today = LocalDate.of(2027, 1, 20)

    private fun task(
        repeat: TaskRepeat,
        lastDoneOn: LocalDate? = null,
        createdOn: LocalDate = LocalDate.of(2027, 1, 4),
        title: String = "Poubelles",
    ) = HouseholdTask(title = title, repeat = repeat, createdOn = createdOn, lastDoneOn = lastDoneOn)

    @Test
    fun `a task never done is due from its creation day`() {
        val task = task(TaskRepeat.WEEKLY)

        assertThat(TaskMath.nextDue(task)).isEqualTo(LocalDate.of(2027, 1, 4))
        assertThat(TaskMath.isDue(task, today)).isTrue()
    }

    @Test
    fun `next due date follows the repeat after the last time it was done`() {
        val done = LocalDate.of(2027, 1, 20)

        assertThat(TaskMath.nextDue(task(TaskRepeat.DAILY, done))).isEqualTo(LocalDate.of(2027, 1, 21))
        assertThat(TaskMath.nextDue(task(TaskRepeat.WEEKLY, done))).isEqualTo(LocalDate.of(2027, 1, 27))
        assertThat(TaskMath.nextDue(task(TaskRepeat.BIWEEKLY, done))).isEqualTo(LocalDate.of(2027, 2, 3))
        assertThat(TaskMath.nextDue(task(TaskRepeat.MONTHLY, done))).isEqualTo(LocalDate.of(2027, 2, 20))
    }

    @Test
    fun `monthly repeat clamps to the end of a shorter month`() {
        val task = task(TaskRepeat.MONTHLY, LocalDate.of(2027, 1, 31))

        assertThat(TaskMath.nextDue(task)).isEqualTo(LocalDate.of(2027, 2, 28))
    }

    @Test
    fun `a task done today is not due again today`() {
        val task = task(TaskRepeat.DAILY, today)

        assertThat(TaskMath.isDue(task, today)).isFalse()
        assertThat(TaskMath.daysOverdue(task, today)).isEqualTo(-1)
    }

    @Test
    fun `a task is due on its due day and overdue after`() {
        val task = task(TaskRepeat.WEEKLY, LocalDate.of(2027, 1, 13))

        assertThat(TaskMath.isDue(task, today)).isTrue()
        assertThat(TaskMath.daysOverdue(task, today)).isEqualTo(0)
        assertThat(TaskMath.daysOverdue(task, today.plusDays(3))).isEqualTo(3)
    }

    @Test
    fun `due tasks come first, most overdue first, then upcoming by due date`() {
        val late = task(TaskRepeat.WEEKLY, LocalDate.of(2027, 1, 1), title = "En retard")
        val justDue = task(TaskRepeat.WEEKLY, LocalDate.of(2027, 1, 13), title = "Aujourd'hui")
        val soon = task(TaskRepeat.WEEKLY, LocalDate.of(2027, 1, 16), title = "Bientôt")
        val later = task(TaskRepeat.MONTHLY, LocalDate.of(2027, 1, 19), title = "Plus tard")

        val sorted = TaskMath.sorted(listOf(later, soon, justDue, late), today)

        assertThat(sorted.map { it.title }).containsExactly("En retard", "Aujourd'hui", "Bientôt", "Plus tard").inOrder()
    }
}
