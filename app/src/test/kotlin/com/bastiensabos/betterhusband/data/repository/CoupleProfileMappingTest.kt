package com.bastiensabos.betterhusband.data.repository

import com.bastiensabos.betterhusband.data.local.entity.CoupleProfileEntity
import com.bastiensabos.betterhusband.domain.model.Budget
import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import com.bastiensabos.betterhusband.domain.model.FamilySituation
import com.bastiensabos.betterhusband.domain.model.Interest
import com.bastiensabos.betterhusband.domain.model.KidsAtHome
import com.bastiensabos.betterhusband.domain.model.LifeRhythm
import com.bastiensabos.betterhusband.domain.model.LivingArrangement
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CoupleProfileMappingTest {
    @Test
    fun `a full profile survives the round trip`() {
        val profile =
            CoupleProfile(
                kids = KidsAtHome.BOTH,
                living = LivingArrangement.LONG_DISTANCE,
                budget = Budget.MODERATE,
                rhythm = LifeRhythm.VERY_BUSY,
                interests = setOf(Interest.MUSIC, Interest.NATURE),
                family = setOf(FamilySituation.STEPFAMILY, FamilySituation.SHARED_CUSTODY),
            )

        assertThat(profile.toEntity().toDomain()).isEqualTo(profile)
    }

    @Test
    fun `an empty profile survives the round trip`() {
        assertThat(CoupleProfile().toEntity().toDomain()).isEqualTo(CoupleProfile())
    }

    @Test
    fun `unknown values read as unanswered`() {
        val entity = CoupleProfileEntity(kids = "TWINS", living = null, budget = "", rhythm = "RELAXED", interests = "SPORT,KNITTING")

        assertThat(entity.toDomain())
            .isEqualTo(CoupleProfile(rhythm = LifeRhythm.RELAXED, interests = setOf(Interest.SPORT)))
    }
}
