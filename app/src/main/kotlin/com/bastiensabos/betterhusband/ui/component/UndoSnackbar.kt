package com.bastiensabos.betterhusband.ui.component

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.bastiensabos.betterhusband.R

/**
 * Une action faite tout de suite mais encore réversible (suppression, tâche cochée) : le bandeau la rappelle quelques
 * secondes avec « Annuler », comme Gmail ou Keep. [key] distingue deux actions identiques qui se suivent.
 */
data class UndoNotice(
    @StringRes val message: Int,
    val key: Long,
)

/**
 * Affiche [notice] dans [host]. « Annuler » appelle [onUndo] ; à la fin du délai (TalkBack l'allonge) ou si une autre
 * action le remplace, [onDismiss] libère l'état.
 */
@Composable
fun UndoSnackbarEffect(
    notice: UndoNotice?,
    host: SnackbarHostState,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = notice?.let { stringResource(it.message) }
    val undoLabel = stringResource(R.string.undo)
    val undo by rememberUpdatedState(onUndo)
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(notice) {
        if (message == null) return@LaunchedEffect
        val result = host.showSnackbar(message = message, actionLabel = undoLabel, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) undo() else dismiss()
    }
}
