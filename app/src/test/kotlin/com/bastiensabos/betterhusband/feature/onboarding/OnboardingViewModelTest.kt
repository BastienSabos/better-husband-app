package com.bastiensabos.betterhusband.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.bastiensabos.betterhusband.data.repository.FakePartnerRepository
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class OnboardingViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = FakePartnerRepository()
    private val savedState = SavedStateHandle()
    private val viewModel = OnboardingViewModel(repository, savedState)

    @Test
    fun `starts on the promise, then walks through the three steps and back`() {
        assertThat(viewModel.uiState.value.step).isEqualTo(OnboardingStep.WELCOME)

        viewModel.onStart()
        viewModel.onFirstNameChange("Alex")
        viewModel.onContinue()
        assertThat(viewModel.uiState.value.step).isEqualTo(OnboardingStep.BIRTHDAY)

        viewModel.onBack()
        assertThat(viewModel.uiState.value.step).isEqualTo(OnboardingStep.PARTNER)
        assertThat(viewModel.uiState.value.firstName).isEqualTo("Alex")
    }

    @Test
    fun `cannot go past the partner step without a first name`() {
        viewModel.onStart()
        viewModel.onFirstNameChange("   ")

        viewModel.onContinue()

        assertThat(viewModel.uiState.value.step).isEqualTo(OnboardingStep.PARTNER)
        assertThat(viewModel.uiState.value.canSubmit).isFalse()
    }

    @Test
    fun `finishing saves the partner with the address form and the birthday`() =
        runTest {
            viewModel.onFirstNameChange(" Alex ")
            viewModel.onAddressFormSelect(AddressForm.FEMININE)
            viewModel.onBirthdayChange(LocalDate.of(1992, 11, 21))

            viewModel.onSubmit()
            advanceUntilIdle()

            assertThat(repository.state.value).isEqualTo(Partner("Alex", AddressForm.FEMININE, birthday = LocalDate.of(1992, 11, 21)))
            assertThat(viewModel.uiState.value.saving).isFalse()
        }

    @Test
    fun `later saves the partner without the birthday`() =
        runTest {
            viewModel.onFirstNameChange("Alex")
            viewModel.onBirthdayChange(LocalDate.of(1992, 11, 21))

            viewModel.onSubmit(withBirthday = false)
            advanceUntilIdle()

            assertThat(repository.state.value?.birthday).isNull()
        }

    @Test
    fun `the draft survives the process being killed`() {
        viewModel.onStart()
        viewModel.onFirstNameChange("Alex")
        viewModel.onAddressFormSelect(AddressForm.MASCULINE)
        viewModel.onContinue()
        viewModel.onBirthdayChange(LocalDate.of(1992, 11, 21))

        val restored = OnboardingViewModel(repository, savedState).uiState.value

        assertThat(restored.step).isEqualTo(OnboardingStep.BIRTHDAY)
        assertThat(restored.firstName).isEqualTo("Alex")
        assertThat(restored.addressForm).isEqualTo(AddressForm.MASCULINE)
        assertThat(restored.birthday).isEqualTo(LocalDate.of(1992, 11, 21))
    }

    @Test
    fun `first name is capped at 40 characters`() {
        viewModel.onFirstNameChange("a".repeat(100))

        assertThat(viewModel.uiState.value.firstName).hasLength(40)
    }
}
