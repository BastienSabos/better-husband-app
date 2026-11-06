package com.bastiensabos.betterhusband

import app.cash.turbine.test
import com.bastiensabos.betterhusband.data.repository.FakePartnerRepository
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    @Test
    fun `starts loading then asks for onboarding when there is no partner`() = runTest {
        val viewModel = AppViewModel(FakePartnerRepository())

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(AppUiState.Loading)
            assertThat(awaitItem()).isEqualTo(AppUiState.NeedsOnboarding)
        }
    }

    @Test
    fun `goes to the app once the partner exists`() = runTest {
        val repository = FakePartnerRepository()
        val viewModel = AppViewModel(repository)

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(AppUiState.Loading)
            assertThat(awaitItem()).isEqualTo(AppUiState.NeedsOnboarding)
            repository.savePartner(Partner("Alex", AddressForm.NEUTRAL))
            assertThat(awaitItem()).isEqualTo(AppUiState.Ready)
        }
    }
}
