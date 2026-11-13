package com.bastiensabos.betterhusband.feature.duo

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.ui.component.BackTopBar
import com.bastiensabos.betterhusband.ui.component.Heading
import com.bastiensabos.betterhusband.ui.theme.AppTheme
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme

@Composable
fun DuoInviteRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DuoInviteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val words = stringArrayResource(R.array.duo_verification_words)
    val shareTitle = stringResource(R.string.duo_share_link)
    DuoInviteScreen(
        state = state,
        wordOf = { words.getOrElse(it) { "" } },
        onBack = {
            viewModel.onCancel()
            onBack()
        },
        onLinkInputChange = viewModel::onLinkInputChange,
        onCreateLink = viewModel::onCreateLink,
        onJoin = viewModel::onJoin,
        leave = DuoLeaveActions(viewModel::onLeave, viewModel::onConfirmLeave, viewModel::onDismissLeave),
        onShare = { url ->
            val send =
                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
            context.startActivity(Intent.createChooser(send, shareTitle))
        },
        modifier = modifier,
    )
}

/** Sortir du duo : demander, confirmer, renoncer. */
data class DuoLeaveActions(
    val onLeave: () -> Unit,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)

private val noLeave = DuoLeaveActions({}, {}, {})

/** Inviter son ou sa partenaire, ou rejoindre avec un lien : ce qui sera partagé est dit avant tout. */
@Composable
fun DuoInviteScreen(
    state: DuoInviteUiState,
    wordOf: (Int) -> String,
    onBack: () -> Unit,
    onLinkInputChange: (String) -> Unit,
    onCreateLink: () -> Unit,
    onJoin: () -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
    leave: DuoLeaveActions = noLeave,
) {
    Scaffold(modifier = modifier, topBar = { BackTopBar(title = stringResource(R.string.duo_title), onBack = onBack) }) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(AppTheme.spacing.screen),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            when (val stage = state.stage) {
                DuoStage.Intro -> if (state.inDuo) InDuo(leave.onLeave) else Intro(state, onLinkInputChange, onCreateLink, onJoin)
                is DuoStage.HostWaiting -> HostWaiting(stage.url, onShare)
                DuoStage.GuestWaiting -> Waiting(R.string.duo_guest_waiting)
                is DuoStage.Joined -> Joined(stage.code.map(wordOf), onBack)
            }
            state.notice?.let { Text(stringResource(it.message()), color = MaterialTheme.colorScheme.error) }
        }
    }
    if (state.confirmLeave) LeaveDialog(leave)
}

@StringRes
private fun DuoNotice.message(): Int =
    when (this) {
        DuoNotice.LINK_INVALID -> R.string.duo_notice_link_invalid
        DuoNotice.LINK_UNAVAILABLE -> R.string.duo_notice_link_unavailable
        DuoNotice.ANSWER_REFUSED -> R.string.duo_notice_refused
        DuoNotice.SAVE_FAILED -> R.string.duo_notice_save_failed
        DuoNotice.LEAVE_FAILED -> R.string.duo_notice_leave_failed
    }

@Composable
private fun InDuo(onLeave: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        Heading(stringResource(R.string.duo_in_duo_title), MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.duo_in_duo_body))
        OutlinedButton(onClick = onLeave, modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.spacing.touchTarget)) {
            Text(stringResource(R.string.duo_leave))
        }
    }
}

@Composable
private fun LeaveDialog(actions: DuoLeaveActions) {
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(stringResource(R.string.duo_leave_confirm_title)) },
        text = { Text(stringResource(R.string.duo_leave_confirm_body)) },
        confirmButton = { TextButton(onClick = actions.onConfirm) { Text(stringResource(R.string.duo_leave_confirm)) } },
        dismissButton = { TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.duo_leave_cancel)) } },
    )
}

@Composable
private fun Intro(
    state: DuoInviteUiState,
    onLinkInputChange: (String) -> Unit,
    onCreateLink: () -> Unit,
    onJoin: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        Heading(stringResource(R.string.duo_shared_title), MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.duo_shared_body))
        Heading(stringResource(R.string.duo_never_title), MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.duo_never_body))
        Button(onClick = onCreateLink, modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.spacing.touchTarget)) {
            Text(stringResource(R.string.duo_create_link))
        }
        Heading(stringResource(R.string.duo_join_title), MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.linkInput,
            onValueChange = onLinkInputChange,
            label = { Text(stringResource(R.string.duo_join_field)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = onJoin,
            enabled = state.canJoin,
            modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.spacing.touchTarget),
        ) {
            Text(stringResource(R.string.duo_join))
        }
    }
}

@Composable
private fun HostWaiting(
    url: String,
    onShare: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        Text(stringResource(R.string.duo_host_body))
        SelectionContainer { Text(url, style = MaterialTheme.typography.bodyMedium) }
        Button(onClick = { onShare(url) }, modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.spacing.touchTarget)) {
            Text(stringResource(R.string.duo_share_link))
        }
        Waiting(R.string.duo_host_waiting)
    }
}

@Composable
private fun Waiting(
    @StringRes label: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        CircularProgressIndicator()
        Text(stringResource(label))
    }
}

@Composable
private fun Joined(
    words: List<String>,
    onDone: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        Heading(stringResource(R.string.duo_joined_title), MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.duo_joined_body))
        Text(words.joinToString(" · "), style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.spacing.touchTarget)) {
            Text(stringResource(R.string.duo_done))
        }
    }
}

private val previewWords = listOf("abeille", "lune", "pomme", "phare")

@Preview(showBackground = true)
@Composable
private fun IntroPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        DuoInviteScreen(DuoInviteUiState(), { "" }, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun HostWaitingLargeFontPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        DuoInviteScreen(DuoInviteUiState(stage = DuoStage.HostWaiting("https://duo.invalid/i/abc#def")), { "" }, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinedPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        DuoInviteScreen(DuoInviteUiState(stage = DuoStage.Joined(listOf(0, 1, 2, 3))), { previewWords[it] }, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LeaveConfirmPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        DuoInviteScreen(DuoInviteUiState(inDuo = true, confirmLeave = true), { "" }, {}, {}, {}, {}, {})
    }
}
