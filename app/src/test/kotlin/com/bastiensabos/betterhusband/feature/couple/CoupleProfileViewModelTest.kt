package com.bastiensabos.betterhusband.feature.couple

import com.bastiensabos.betterhusband.data.repository.FakeCoupleProfileRepository
import com.bastiensabos.betterhusband.domain.model.Budget
import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import com.bastiensabos.betterhusband.domain.model.Interest
import com.bastiensabos.betterhusband.domain.model.KidsAtHome
import com.bastiensabos.betterhusband.domain.model.LifeRhythm
import com.bastiensabos.betterhusband.domain.model.LivingArrangement
import com.bastiensabos.betterhusband.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CoupleProfileViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = FakeCoupleProfileRepository(CoupleProfile(budget = Budget.TIGHT))

    @Test
    fun `loads the stored profile`() =
        runTest {
            val viewModel = CoupleProfileViewModel(repository)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.loading).isFalse()
            assertThat(viewModel.uiState.value.profile.budget).isEqualTo(Budget.TIGHT)
        }

    @Test
    fun `answers are saved only when asked`() =
        runTest {
            val viewModel = CoupleProfileViewModel(repository)
            advanceUntilIdle()

            viewModel.onKidsChange(KidsAtHome.TEENS)
            viewModel.onLivingChange(LivingArrangement.TOGETHER)
            viewModel.onRhythmChange(LifeRhythm.BALANCED)
            viewModel.onInterestToggle(Interest.GAMES)
            advanceUntilIdle()
            assertThat(repository.state.value.kids).isNull()

            viewModel.onSave()
            advanceUntilIdle()

            assertThat(repository.state.value)
                .isEqualTo(
                    CoupleProfile(
                        kids = KidsAtHome.TEENS,
                        living = LivingArrangement.TOGETHER,
                        budget = Budget.TIGHT,
                        rhythm = LifeRhythm.BALANCED,
                        interests = setOf(Interest.GAMES),
                    ),
                )
            assertThat(viewModel.uiState.value.saved).isTrue()
        }

    @Test
    fun `touching a chosen answer clears it`() =
        runTest {
            val viewModel = CoupleProfileViewModel(repository)
            advanceUntilIdle()

            viewModel.onBudgetChange(Budget.TIGHT)
            viewModel.onInterestToggle(Interest.MUSIC)
            viewModel.onInterestToggle(Interest.MUSIC)

            assertThat(viewModel.uiState.value.profile).isEqualTo(CoupleProfile())
        }
}
