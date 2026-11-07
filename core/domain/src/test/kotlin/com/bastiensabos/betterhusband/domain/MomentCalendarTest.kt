package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.DateLabel
import com.bastiensabos.betterhusband.domain.model.KidsAtHome
import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.Month

class MomentCalendarTest {
    private val ordinaryDay = LocalDate.of(2027, 6, 10)

    private fun upcoming(
        label: DateLabel,
        kind: DateKind,
        daysUntil: Long,
        recurring: Boolean = true,
    ) = UpcomingDate(
        key = "k",
        label = label,
        kind = kind,
        date = ordinaryDay.plusDays(daysUntil),
        daysUntil = daysUntil,
        yearsSince = if (recurring) 3 else null,
        editableId = null,
    )

    private fun groups(
        dates: List<UpcomingDate> = emptyList(),
        kids: KidsAtHome? = null,
        today: LocalDate = ordinaryDay,
    ) = MomentCalendar.priorityGroups(dates, kids, today)

    @Test
    fun `an ordinary day has no priority`() {
        assertThat(groups()).isEmpty()
    }

    @Test
    fun `partner birthday is prepared on chosen days then celebrated`() {
        val birthday = { days: Long -> listOf(upcoming(DateLabel.PartnerBirthday("Alex"), DateKind.BIRTHDAY, days)) }
        assertThat(groups(birthday(7))).containsExactly(AttentionGroup.PARTNER_BIRTHDAY_PREPARE)
        assertThat(groups(birthday(6))).isEmpty()
        assertThat(groups(birthday(0))).containsExactly(AttentionGroup.PARTNER_BIRTHDAY_DAY)
    }

    @Test
    fun `relationship start and a wedding both count as anniversary`() {
        assertThat(groups(listOf(upcoming(DateLabel.RelationshipStart, DateKind.ANNIVERSARY, 0))))
            .containsExactly(AttentionGroup.ANNIVERSARY_DAY)
        assertThat(groups(listOf(upcoming(DateLabel.Custom("Mariage"), DateKind.ANNIVERSARY, 14))))
            .containsExactly(AttentionGroup.ANNIVERSARY_PREPARE)
    }

    @Test
    fun `relatives and custom birthdays count as relative birthday`() {
        assertThat(groups(listOf(upcoming(DateLabel.RelativeBirthday("Sam"), DateKind.BIRTHDAY, 3))))
            .containsExactly(AttentionGroup.RELATIVE_BIRTHDAY_PREPARE)
        assertThat(groups(listOf(upcoming(DateLabel.Custom("Sam"), DateKind.BIRTHDAY, 0))))
            .containsExactly(AttentionGroup.RELATIVE_BIRTHDAY_DAY)
    }

    @Test
    fun `one-off events and other dates are not moments`() {
        assertThat(groups(listOf(upcoming(DateLabel.Custom("Concert"), DateKind.OTHER, 0)))).isEmpty()
        assertThat(groups(listOf(upcoming(DateLabel.Custom("Fête"), DateKind.ANNIVERSARY, 0, recurring = false)))).isEmpty()
    }

    @Test
    fun `calendar holidays come every year`() {
        assertThat(groups(today = LocalDate.of(2027, 2, 14))).containsExactly(AttentionGroup.VALENTINES_DAY)
        assertThat(groups(today = LocalDate.of(2027, 2, 4))).containsExactly(AttentionGroup.VALENTINES_PREPARE)
        assertThat(groups(today = LocalDate.of(2027, 12, 25))).containsExactly(AttentionGroup.CHRISTMAS_DAY)
        assertThat(groups(today = LocalDate.of(2027, 11, 20))).containsExactly(AttentionGroup.CHRISTMAS_PREPARE)
    }

    @Test
    fun `christmas and new year can overlap`() {
        assertThat(groups(today = LocalDate.of(2027, 12, 27)))
            .containsExactly(AttentionGroup.NEW_YEAR_PREPARE)
        assertThat(groups(today = LocalDate.of(2027, 12, 24)))
            .containsExactly(AttentionGroup.CHRISTMAS_PREPARE)
        assertThat(groups(today = LocalDate.of(2027, 12, 31))).containsExactly(AttentionGroup.NEW_YEAR_DAY)
    }

    @Test
    fun `back to school only counts with kids at home`() {
        val first = LocalDate.of(2027, 9, 1)
        assertThat(groups(today = first)).isEmpty()
        assertThat(groups(kids = KidsAtHome.NONE, today = first)).isEmpty()
        assertThat(groups(kids = KidsAtHome.TEENS, today = first)).containsExactly(AttentionGroup.BACK_TO_SCHOOL_DAY)
        assertThat(groups(kids = KidsAtHome.YOUNG, today = first.minusDays(10)))
            .containsExactly(AttentionGroup.BACK_TO_SCHOOL_PREPARE)
    }

    @Test
    fun `several moments the same day add up`() {
        val christmasEve = LocalDate.of(2027, 12, 24)
        val birthday = UpcomingDate("k", DateLabel.PartnerBirthday("Alex"), DateKind.BIRTHDAY, christmasEve, 0, 30, null)
        assertThat(groups(listOf(birthday), today = christmasEve))
            .containsExactly(AttentionGroup.PARTNER_BIRTHDAY_DAY, AttentionGroup.CHRISTMAS_PREPARE)
    }

    @Test
    fun `priority ids only cover written texts of the chosen groups`() {
        val sizes = mapOf(AttentionGroup.EVERYONE to 3, AttentionGroup.VALENTINES_DAY to 2)
        assertThat(MomentCalendar.priorityIds(sizes, setOf(AttentionGroup.VALENTINES_DAY, AttentionGroup.CHRISTMAS_DAY)))
            .containsExactly(470_000, 470_001)
            .inOrder()
    }

    @Test
    fun `moment groups never enter the daily draw`() {
        val sizes = AttentionGroup.entries.associateWith { 1 }
        val context = AttentionContext(CoupleProfile(), 20, Month.DECEMBER)
        val drawn = AttentionCatalog.eligibleIds(sizes, context).mapNotNull { AttentionGroup.of(it) }
        val momentGroups = Moment.entries.flatMap { listOf(it.prepare, it.day) }
        assertThat(drawn).containsNoneIn(momentGroups)
    }
}
