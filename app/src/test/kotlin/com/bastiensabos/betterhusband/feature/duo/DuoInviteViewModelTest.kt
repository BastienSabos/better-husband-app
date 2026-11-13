package com.bastiensabos.betterhusband.feature.duo

import com.bastiensabos.betterhusband.data.repository.FakeDuoRepository
import com.bastiensabos.betterhusband.domain.duo.DuoInviteLink
import com.bastiensabos.betterhusband.domain.duo.InMemoryDuoInviteServer
import com.bastiensabos.betterhusband.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DuoInviteViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2027-01-20T09:00:00Z"), ZoneOffset.UTC)
    private val server = InMemoryDuoInviteServer()
    private val duo = FakeDuoRepository()

    // Deux « téléphones » sur le même faux serveur ; créés au premier usage, après MainDispatcherRule.
    private val host by lazy { DuoInviteViewModel(server, duo, clock, StandardTestDispatcher(mainRule.dispatcher.scheduler)) }
    private val guest by lazy { DuoInviteViewModel(server, duo, clock, StandardTestDispatcher(mainRule.dispatcher.scheduler)) }

    /**
     * Les deux ViewModels interrogent le serveur en boucle tant qu'ils attendent : sans les arrêter, `runTest` avance le
     * temps virtuel à l'infini une fois le corps du test fini.
     */
    private fun runDuoTest(body: suspend TestScope.() -> Unit) =
        runTest {
            try {
                body()
            } finally {
                host.onCancel()
                guest.onCancel()
            }
        }

    private fun TestScope.settle() {
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
    }

    @Test
    fun `starts on the explanation with nothing to do`() {
        assertThat(host.uiState.value.stage).isEqualTo(DuoStage.Intro)
        assertThat(host.uiState.value.canJoin).isFalse()
    }

    @Test
    fun `creating the link waits for the other phone`() =
        runDuoTest {
            host.onCreateLink("duo.example")
            settle()

            val stage = host.uiState.value.stage as DuoStage.HostWaiting
            assertThat(stage.url).startsWith("https://duo.example/i/")
            assertThat(DuoInviteLink.parse(stage.url)).isNotNull()
        }

    @Test
    fun `both phones show the same verification code once the guest has joined`() =
        runDuoTest {
            host.onCreateLink("duo.example")
            settle()
            guest.onLinkInputChange((host.uiState.value.stage as DuoStage.HostWaiting).url)
            guest.onJoin()
            settle()
            settle()

            val atHost = host.uiState.value.stage as DuoStage.Joined
            val atGuest = guest.uiState.value.stage as DuoStage.Joined
            assertThat(atGuest.code).isEqualTo(atHost.code)
            assertThat(atHost.code).hasSize(4)
            assertThat(duo.joined).isNotNull()
        }

    @Test
    fun `a pasted text that is not an invitation is refused without leaving the screen`() =
        runDuoTest {
            guest.onLinkInputChange("coucou")
            guest.onJoin()
            settle()

            assertThat(guest.uiState.value.notice).isEqualTo(DuoNotice.LINK_INVALID)
            assertThat(guest.uiState.value.stage).isEqualTo(DuoStage.Intro)
        }

    @Test
    fun `an unknown invitation says it expired`() =
        runDuoTest {
            guest.onLinkInputChange("https://duo.example/i/${"a".repeat(32)}#${"A".repeat(22)}")
            guest.onJoin()
            settle()

            assertThat(guest.uiState.value.notice).isEqualTo(DuoNotice.LINK_UNAVAILABLE)
        }

    @Test
    fun `cancelling goes back to the explanation`() =
        runDuoTest {
            host.onCreateLink("duo.example")
            settle()
            host.onCancel()

            assertThat(host.uiState.value).isEqualTo(DuoInviteUiState())
        }

    @Test
    fun `a keystore refusal goes back to the start instead of announcing a duo`() =
        runDuoTest {
            duo.failOnJoin = true
            host.onCreateLink("duo.example")
            settle()
            guest.onLinkInputChange((host.uiState.value.stage as DuoStage.HostWaiting).url)
            guest.onJoin()
            settle()
            settle()

            assertThat(host.uiState.value).isEqualTo(DuoInviteUiState(notice = DuoNotice.SAVE_FAILED))
            assertThat(duo.joined).isNull()
        }
}
