package com.bastiensabos.betterhusband.feature.onboarding

import com.bastiensabos.betterhusband.data.repository.FakePartnerRepository
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = FakePartnerRepository()
    private val viewModel = OnboardingViewModel(repository)

    @Test
    fun `cannot submit without a first name`() = runTest {
        viewModel.onFirstNameChange("   ")

        viewModel.onSubmit()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.canSubmit).isFalse()
        assertThat(repository.state.value).isNull()
    }

    @Test
    fun `submit saves the partner with the chosen address form`() = runTest {
        viewModel.onFirstNameChange("Alex")
        viewModel.onAddressFormSelected(AddressForm.FEMININE)

        viewModel.onSubmit()
        advanceUntilIdle()

        assertThat(repository.state.value).isEqualTo(Partner("Alex", AddressForm.FEMININE))
        assertThat(viewModel.uiState.value.saving).isFalse()
    }

    @Test
    fun `first name is capped at 40 characters`() = runTest {
        viewModel.onFirstNameChange("a".repeat(100))

        assertThat(viewModel.uiState.value.firstName).hasLength(40)
    }
}
