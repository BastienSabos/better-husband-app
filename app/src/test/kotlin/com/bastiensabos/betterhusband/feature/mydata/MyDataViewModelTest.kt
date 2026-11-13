package com.bastiensabos.betterhusband.feature.mydata

import com.bastiensabos.betterhusband.data.export.FakeDocumentWriter
import com.bastiensabos.betterhusband.data.repository.FakeAttentionLogRepository
import com.bastiensabos.betterhusband.data.repository.FakeCoupleProfileRepository
import com.bastiensabos.betterhusband.data.repository.FakeDuoRepository
import com.bastiensabos.betterhusband.data.repository.FakeHouseholdTaskRepository
import com.bastiensabos.betterhusband.data.repository.FakeIdeaRepository
import com.bastiensabos.betterhusband.data.repository.FakeImportantDateRepository
import com.bastiensabos.betterhusband.data.repository.FakePartnerRepository
import com.bastiensabos.betterhusband.data.security.FakeBiometricAuthenticator
import com.bastiensabos.betterhusband.data.security.FakeLockSettings
import com.bastiensabos.betterhusband.data.work.FakeReminderScheduler
import com.bastiensabos.betterhusband.domain.DeleteAllUserDataUseCase
import com.bastiensabos.betterhusband.domain.ExportUserDataUseCase
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MyDataViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2027-01-20T09:00:00Z"), ZoneOffset.UTC)
    private val partners = FakePartnerRepository(Partner("Camille", AddressForm.NEUTRAL))
    private val dates = FakeImportantDateRepository()
    private val ideas = FakeIdeaRepository()
    private val tasks = FakeHouseholdTaskRepository()
    private val attentions = FakeAttentionLogRepository()
    private val coupleProfile = FakeCoupleProfileRepository()
    private val writer = FakeDocumentWriter()
    private val lockSettings = FakeLockSettings()
    private val authenticator = FakeBiometricAuthenticator()

    private fun newViewModel() =
        MyDataViewModel(
            ExportUserDataUseCase(partners, dates, ideas, tasks, attentions, coupleProfile, clock),
            DeleteAllUserDataUseCase(
                partners,
                dates,
                ideas,
                tasks,
                attentions,
                coupleProfile,
                FakeReminderScheduler(),
                FakeDuoRepository(),
            ),
            writer,
            lockSettings,
            authenticator,
            clock,
        )

    private val viewModel = newViewModel()

    @Test
    fun `suggested file name is dated and holds no personal data`() {
        assertThat(viewModel.suggestedFileName()).isEqualTo("mes-donnees-2027-01-20.json")
    }

    @Test
    fun `export writes the json to the chosen document`() =
        runTest {
            viewModel.onExportTo("content://docs/1")
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.exportStatus).isEqualTo(ExportStatus.DONE)
            assertThat(writer.written["content://docs/1"]).contains("\"firstName\": \"Camille\"")
        }

    @Test
    fun `export reports a failure when the document cannot be written`() =
        runTest {
            writer.failing = true

            viewModel.onExportTo("content://docs/1")
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.exportStatus).isEqualTo(ExportStatus.FAILED)
        }

    @Test
    fun `cancelling the picker returns to rest`() {
        viewModel.onExportCancel()

        assertThat(viewModel.uiState.value.exportStatus).isEqualTo(ExportStatus.IDLE)
    }

    @Test
    fun `delete asks for confirmation and deletes nothing before it`() =
        runTest {
            viewModel.onDeleteRequest()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.confirmingDelete).isTrue()
            assertThat(partners.state.value).isNotNull()
        }

    @Test
    fun `dismissing the confirmation keeps the data`() =
        runTest {
            viewModel.onDeleteRequest()
            viewModel.onDeleteDismiss()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.confirmingDelete).isFalse()
            assertThat(partners.state.value).isNotNull()
        }

    @Test
    fun `confirming deletes everything and closes the dialog`() =
        runTest {
            viewModel.onDeleteRequest()
            viewModel.onDeleteConfirm()
            advanceUntilIdle()

            assertThat(partners.state.value).isNull()
            assertThat(viewModel.uiState.value).isEqualTo(MyDataUiState())
        }

    @Test
    fun `lock can be turned on and off`() {
        viewModel.onLockChange(true)

        assertThat(lockSettings.enabled.value).isTrue()
        assertThat(viewModel.uiState.value.lockEnabled).isTrue()

        viewModel.onLockChange(false)

        assertThat(lockSettings.enabled.value).isFalse()
        assertThat(viewModel.uiState.value.lockEnabled).isFalse()
    }

    @Test
    fun `lock cannot be turned on when the phone has no screen lock`() {
        authenticator.available = false
        val vm = newViewModel()

        vm.onLockChange(true)

        assertThat(vm.uiState.value.lockAvailable).isFalse()
        assertThat(lockSettings.enabled.value).isFalse()
    }

    @Test
    fun `an already enabled lock can still be turned off when unavailable`() {
        lockSettings.setEnabled(true)
        authenticator.available = false
        val vm = newViewModel()

        vm.onLockChange(false)

        assertThat(lockSettings.enabled.value).isFalse()
    }
}
