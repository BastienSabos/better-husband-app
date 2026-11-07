package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.DateLabel
import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class ReminderPlannerTest {
    private val clock = Clock.fixed(Instant.parse("2027-03-10T09:00:00Z"), ZoneOffset.UTC)
    private val planner = ReminderPlanner(clock)

    private fun upcoming(
        key: String,
        date: LocalDate,
    ) = UpcomingDate(key, DateLabel.Custom("x"), DateKind.OTHER, date, daysUntil = 0, yearsSince = null, editableId = null)

    @Test
    fun `plans J-14, J-3 and day-of at 9am for a date far enough away`() {
        val plan = planner.plan(listOf(upcoming("a", LocalDate.of(2027, 4, 12))))

        assertThat(plan.map { it.workName })
            .containsExactly("reminder-a-j14", "reminder-a-j3", "reminder-a-j0")
            .inOrder()
        assertThat(plan.map { it.triggerAt.toLocalDate() })
            .containsExactly(LocalDate.of(2027, 3, 29), LocalDate.of(2027, 4, 9), LocalDate.of(2027, 4, 12))
            .inOrder()
        assertThat(plan.map { it.triggerAt.hour }.distinct()).containsExactly(9)
    }

    @Test
    fun `skips reminders already in the past`() {
        val plan = planner.plan(listOf(upcoming("a", LocalDate.of(2027, 3, 11))))

        assertThat(plan.map { it.offsetDays }).containsExactly(0)
    }

    @Test
    fun `a reminder due exactly now is skipped`() {
        assertThat(planner.plan(listOf(upcoming("a", LocalDate.of(2027, 3, 10))))).isEmpty()
    }

    @Test
    fun `dates beyond the horizon are not planned yet`() {
        assertThat(planner.plan(listOf(upcoming("a", LocalDate.of(2027, 9, 1))))).isEmpty()
    }

    @Test
    fun `local time stays 9am across the spring daylight saving change`() {
        val paris = ZoneId.of("Europe/Paris")
        val parisPlanner = ReminderPlanner(Clock.fixed(Instant.parse("2027-03-20T09:00:00Z"), paris))

        val plan = parisPlanner.plan(listOf(upcoming("a", LocalDate.of(2027, 3, 28))))

        assertThat(plan.map { it.triggerAt.hour }.distinct()).containsExactly(9)
        assertThat(plan.single { it.offsetDays == 0 }.triggerAt.toInstant()).isEqualTo(Instant.parse("2027-03-28T07:00:00Z"))
    }

    @Test
    fun `results are sorted by trigger time across dates`() {
        val plan = planner.plan(listOf(upcoming("late", LocalDate.of(2027, 4, 12)), upcoming("soon", LocalDate.of(2027, 3, 20))))

        assertThat(plan.map { it.triggerAt }).isInOrder()
    }

    @Test
    fun `selectDue matches only the right key and offset`() {
        val item = upcoming("a", LocalDate.of(2027, 3, 13)).copy(daysUntil = 3)

        assertThat(ReminderPlanner.selectDue(listOf(item), "a", 3)).isEqualTo(item)
        assertThat(ReminderPlanner.selectDue(listOf(item), "a", 14)).isNull()
        assertThat(ReminderPlanner.selectDue(listOf(item), "b", 3)).isNull()
    }
}
