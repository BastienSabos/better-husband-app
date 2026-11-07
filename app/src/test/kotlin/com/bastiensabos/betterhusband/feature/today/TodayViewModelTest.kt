package com.bastiensabos.betterhusband.feature.today

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TodayViewModelTest {
    @Test
    fun `initial state is empty`() =
        runTest {
            val viewModel = TodayViewModel()

            assertThat(viewModel.uiState.value).isEqualTo(TodayUiState.Empty)
        }

    @Test
    fun `marking a card done only updates that card`() =
        runTest {
            val viewModel = TodayViewModel()
            viewModel.setCards(listOf(TodayCard("a", "Mot doux"), TodayCard("b", "Courses")))

            viewModel.uiState.test {
                assertThat(awaitItem()).isInstanceOf(TodayUiState.Ready::class.java)
                viewModel.onCardComplete("a")
                val cards = (awaitItem() as TodayUiState.Ready).cards
                assertThat(cards.single { it.id == "a" }.done).isTrue()
                assertThat(cards.single { it.id == "b" }.done).isFalse()
            }
        }
}
