package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.Budget
import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import com.bastiensabos.betterhusband.domain.model.FamilySituation
import com.bastiensabos.betterhusband.domain.model.Interest
import com.bastiensabos.betterhusband.domain.model.KidsAtHome
import com.bastiensabos.betterhusband.domain.model.LifeRhythm
import com.bastiensabos.betterhusband.domain.model.LivingArrangement
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Month

class AttentionCatalogTest {
    private val sizes = AttentionGroup.entries.associateWith { 2 }

    private fun groups(
        profile: CoupleProfile = CoupleProfile(),
        years: Int? = null,
        month: Month = Month.JANUARY,
        addressForm: AddressForm? = null,
    ) = AttentionCatalog
        .eligibleIds(sizes, AttentionContext(profile, years, month, addressForm))
        .mapNotNull { AttentionGroup.of(it) }
        .toSet()

    @Test
    fun `an empty profile only gets attentions for everyone, in person and the season`() {
        assertThat(groups()).containsExactly(AttentionGroup.EVERYONE, AttentionGroup.IN_PERSON, AttentionGroup.WINTER)
    }

    @Test
    fun `long distance gets no in person attention`() {
        assertThat(groups(CoupleProfile(living = LivingArrangement.LONG_DISTANCE))).doesNotContain(AttentionGroup.IN_PERSON)
        assertThat(groups(CoupleProfile(living = LivingArrangement.APART))).contains(AttentionGroup.IN_PERSON)
    }

    @Test
    fun `each season matches its months`() {
        assertThat(groups(month = Month.APRIL)).contains(AttentionGroup.SPRING)
        assertThat(groups(month = Month.JULY)).contains(AttentionGroup.SUMMER)
        assertThat(groups(month = Month.OCTOBER)).contains(AttentionGroup.AUTUMN)
        assertThat(groups(month = Month.DECEMBER)).contains(AttentionGroup.WINTER)
    }

    @Test
    fun `living together and apart are exclusive`() {
        assertThat(groups(CoupleProfile(living = LivingArrangement.TOGETHER)))
            .containsNoneOf(AttentionGroup.APART, AttentionGroup.LONG_DISTANCE)
        assertThat(groups(CoupleProfile(living = LivingArrangement.TOGETHER))).contains(AttentionGroup.TOGETHER)
        assertThat(groups(CoupleProfile(living = LivingArrangement.APART))).contains(AttentionGroup.APART)
        assertThat(groups(CoupleProfile(living = LivingArrangement.APART))).doesNotContain(AttentionGroup.LONG_DISTANCE)
    }

    @Test
    fun `long distance also gets attentions for living apart`() {
        assertThat(groups(CoupleProfile(living = LivingArrangement.LONG_DISTANCE)))
            .containsAtLeast(AttentionGroup.APART, AttentionGroup.LONG_DISTANCE)
    }

    @Test
    fun `kids groups follow the kids at home`() {
        assertThat(groups(CoupleProfile(kids = KidsAtHome.NONE))).contains(AttentionGroup.NO_KIDS)
        assertThat(groups(CoupleProfile(kids = KidsAtHome.NONE))).doesNotContain(AttentionGroup.KIDS)
        assertThat(groups(CoupleProfile(kids = KidsAtHome.YOUNG)))
            .containsAtLeast(AttentionGroup.KIDS, AttentionGroup.YOUNG_KIDS)
        assertThat(groups(CoupleProfile(kids = KidsAtHome.YOUNG))).doesNotContain(AttentionGroup.TEENS)
        assertThat(groups(CoupleProfile(kids = KidsAtHome.BOTH)))
            .containsAtLeast(AttentionGroup.KIDS, AttentionGroup.YOUNG_KIDS, AttentionGroup.TEENS)
    }

    @Test
    fun `the chosen address form opens the attentions written for it`() {
        assertThat(groups(addressForm = AddressForm.MASCULINE)).contains(AttentionGroup.PARTNER_MASCULINE)
        assertThat(groups(addressForm = AddressForm.MASCULINE)).doesNotContain(AttentionGroup.PARTNER_FEMININE)
        assertThat(groups(addressForm = AddressForm.FEMININE)).contains(AttentionGroup.PARTNER_FEMININE)
        assertThat(groups(addressForm = AddressForm.FEMININE)).doesNotContain(AttentionGroup.PARTNER_MASCULINE)
    }

    @Test
    fun `without gender agreement no gendered attention is offered`() {
        assertThat(groups(addressForm = AddressForm.NEUTRAL))
            .containsNoneOf(AttentionGroup.PARTNER_MASCULINE, AttentionGroup.PARTNER_FEMININE)
    }

    @Test
    fun `stepfamily and shared custody each open their group`() {
        assertThat(groups(CoupleProfile(family = setOf(FamilySituation.STEPFAMILY))))
            .containsAtLeast(AttentionGroup.STEPFAMILY, AttentionGroup.EVERYONE)
        assertThat(groups(CoupleProfile(family = setOf(FamilySituation.STEPFAMILY))))
            .doesNotContain(AttentionGroup.SHARED_CUSTODY)
        assertThat(groups(CoupleProfile(family = setOf(FamilySituation.SHARED_CUSTODY)))).contains(AttentionGroup.SHARED_CUSTODY)
        assertThat(groups()).containsNoneOf(AttentionGroup.STEPFAMILY, AttentionGroup.SHARED_CUSTODY)
    }

    @Test
    fun `a tight budget gets no paid attention`() {
        assertThat(groups(CoupleProfile(budget = Budget.TIGHT)))
            .containsNoneOf(AttentionGroup.TREATS, AttentionGroup.SPLURGE)
        assertThat(groups(CoupleProfile(budget = Budget.MODERATE))).contains(AttentionGroup.TREATS)
        assertThat(groups(CoupleProfile(budget = Budget.MODERATE))).doesNotContain(AttentionGroup.SPLURGE)
        assertThat(groups(CoupleProfile(budget = Budget.COMFORTABLE)))
            .containsAtLeast(AttentionGroup.TREATS, AttentionGroup.SPLURGE)
    }

    @Test
    fun `a very busy week gets no long attention`() {
        assertThat(groups(CoupleProfile(rhythm = LifeRhythm.VERY_BUSY))).doesNotContain(AttentionGroup.TAKES_TIME)
        assertThat(groups(CoupleProfile(rhythm = LifeRhythm.RELAXED))).contains(AttentionGroup.TAKES_TIME)
    }

    @Test
    fun `relationship length picks new or long couple attentions`() {
        assertThat(groups(years = 1)).contains(AttentionGroup.NEW_COUPLE)
        assertThat(groups(years = 2)).containsNoneOf(AttentionGroup.NEW_COUPLE, AttentionGroup.LONG_COUPLE)
        assertThat(groups(years = 10)).contains(AttentionGroup.LONG_COUPLE)
    }

    @Test
    fun `each chosen interest opens its group`() {
        val interests = Interest.entries.toSet()

        val opened = groups(CoupleProfile(interests = interests))

        assertThat(opened).containsAtLeast(
            AttentionGroup.NATURE,
            AttentionGroup.CULTURE,
            AttentionGroup.SPORT,
            AttentionGroup.COOKING,
            AttentionGroup.GAMES,
            AttentionGroup.TRAVEL,
            AttentionGroup.MUSIC,
            AttentionGroup.READING,
            AttentionGroup.DIY,
            AttentionGroup.WELLBEING,
        )
    }

    @Test
    fun `ids are the group offset plus the position`() {
        val ids = AttentionCatalog.eligibleIds(mapOf(AttentionGroup.EVERYONE to 3), AttentionContext(CoupleProfile(), null, Month.MAY))

        assertThat(ids).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun `an id outside every group has no group`() {
        assertThat(AttentionGroup.of(5)).isEqualTo(AttentionGroup.EVERYONE)
        assertThat(AttentionGroup.of(10_001)).isEqualTo(AttentionGroup.TOGETHER)
        assertThat(AttentionGroup.of(-1)).isNull()
        assertThat(AttentionGroup.of(190_000)).isNull()
        assertThat(AttentionGroup.of(300_001)).isEqualTo(AttentionGroup.IN_PERSON)
    }

    @Test
    fun `group offsets never overlap`() {
        val offsets = AttentionGroup.entries.map { it.offset }

        assertThat(offsets.toSet()).hasSize(offsets.size)
        assertThat(offsets.sorted().zipWithNext().all { (a, b) -> b - a >= AttentionGroup.GROUP_CAPACITY }).isTrue()
    }
}
