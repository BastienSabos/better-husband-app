package com.bastiensabos.betterhusband.feature.duo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.di.IoDispatcher
import com.bastiensabos.betterhusband.domain.duo.DuoInviteGuest
import com.bastiensabos.betterhusband.domain.duo.DuoInviteHost
import com.bastiensabos.betterhusband.domain.duo.DuoInviteLink
import com.bastiensabos.betterhusband.domain.duo.DuoInviteServer
import com.bastiensabos.betterhusband.domain.duo.DuoJoined
import com.bastiensabos.betterhusband.domain.duo.GuestStep
import com.bastiensabos.betterhusband.domain.duo.HostStep
import com.bastiensabos.betterhusband.domain.duo.InviteUnavailableException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject

/** Inviter ou rejoindre : un seul écran, deux rôles. Le serveur est un faux en mémoire tant que Firebase manque. */
@HiltViewModel
class DuoInviteViewModel
    @Inject
    constructor(
        private val server: DuoInviteServer,
        private val clock: Clock,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) : ViewModel() {
        private val state = MutableStateFlow(DuoInviteUiState())
        val uiState: StateFlow<DuoInviteUiState> = state.asStateFlow()

        private var polling: Job? = null

        fun onLinkInputChange(value: String) = state.update { it.copy(linkInput = value.take(MAX_LINK_LENGTH), notice = null) }

        fun onCreateLink(host: String = LINK_HOST) {
            if (state.value.stage != DuoStage.Intro) return
            polling =
                viewModelScope.launch {
                    val invite = withContext(io) { DuoInviteHost.start(server, clock.millis()) }
                    state.update { it.copy(stage = DuoStage.HostWaiting(invite.link.toUrl(host)), notice = null) }
                    while (isActive) {
                        when (val step = withContext(io) { invite.poll() }) {
                            HostStep.Waiting -> {
                                delay(POLL_MILLIS)
                            }

                            HostStep.Refused -> {
                                state.update { it.copy(notice = DuoNotice.ANSWER_REFUSED) }
                                delay(POLL_MILLIS)
                            }

                            is HostStep.Joined -> {
                                return@launch finish(step.joined)
                            }
                        }
                    }
                }
        }

        fun onJoin() {
            val current = state.value
            if (!current.canJoin) return
            val link = DuoInviteLink.parse(current.linkInput)
            if (link == null) {
                state.update { it.copy(notice = DuoNotice.LINK_INVALID) }
                return
            }
            polling =
                viewModelScope.launch {
                    val guest =
                        try {
                            withContext(io) { DuoInviteGuest.join(server, link, clock.millis()) }
                        } catch (_: InviteUnavailableException) {
                            state.update { it.copy(notice = DuoNotice.LINK_UNAVAILABLE) }
                            return@launch
                        }
                    state.update { it.copy(stage = DuoStage.GuestWaiting, notice = null) }
                    while (isActive) {
                        when (val step = withContext(io) { guest.poll() }) {
                            GuestStep.Waiting -> delay(POLL_MILLIS)
                            is GuestStep.Joined -> return@launch finish(step.joined)
                        }
                    }
                }
        }

        /** Annule l'attente (retour) : le lien reste valable côté serveur jusqu'à son expiration. */
        fun onCancel() {
            polling?.cancel()
            state.update { DuoInviteUiState() }
        }

        private fun finish(joined: DuoJoined) {
            state.update { it.copy(stage = DuoStage.Joined(joined.keys.verificationCode()), notice = null) }
        }

        private companion object {
            const val POLL_MILLIS = 2_000L
            const val MAX_LINK_LENGTH = 300

            // Domaine provisoire : à remplacer par celui des App Links quand Bastien l'aura choisi.
            const val LINK_HOST = "duo.invalid"
        }
    }
