package com.bastiensabos.betterhusband.feature.mydata

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.ui.component.BackTopBar
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing

data class MyDataActions(
    val onBack: () -> Unit,
    val onLockChange: (Boolean) -> Unit,
    val onExport: () -> Unit,
    val onDeleteRequest: () -> Unit,
    val onDeleteConfirm: () -> Unit,
    val onDeleteDismiss: () -> Unit,
    val onOpenPrivacy: () -> Unit,
)

@Composable
fun MyDataRoute(
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyDataViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Le sélecteur de fichiers laisse l'utilisateur choisir où va l'export : aucune permission de stockage.
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) viewModel.onExportCancel() else viewModel.onExportTo(uri.toString())
        }
    MyDataScreen(
        uiState = uiState,
        actions =
            MyDataActions(
                onBack = onBack,
                onLockChange = viewModel::onLockChange,
                onExport = { picker.launch(viewModel.suggestedFileName()) },
                onDeleteRequest = viewModel::onDeleteRequest,
                onDeleteConfirm = viewModel::onDeleteConfirm,
                onDeleteDismiss = viewModel::onDeleteDismiss,
                onOpenPrivacy = onOpenPrivacy,
            ),
        modifier = modifier,
    )
}

@Composable
fun MyDataScreen(
    uiState: MyDataUiState,
    actions: MyDataActions,
    modifier: Modifier = Modifier,
) {
    val spacing = Spacing()
    Scaffold(
        modifier = modifier,
        topBar = { BackTopBar(title = stringResource(R.string.my_data_title), onBack = actions.onBack) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screen),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(text = stringResource(R.string.my_data_intro), style = MaterialTheme.typography.bodyLarge)

            LockSetting(uiState, actions.onLockChange)

            Text(
                text = stringResource(R.string.my_data_export_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(R.string.my_data_export_body), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick = actions.onExport,
                enabled = uiState.exportStatus != ExportStatus.RUNNING,
                modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget),
            ) {
                Text(stringResource(R.string.my_data_export_action))
            }
            ExportFeedback(uiState.exportStatus)

            Text(
                text = stringResource(R.string.my_data_delete_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(R.string.my_data_delete_body), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = actions.onDeleteRequest,
                enabled = !uiState.deleting,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget),
            ) {
                Text(stringResource(R.string.my_data_delete_action))
            }

            TextButton(onClick = actions.onOpenPrivacy, modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget)) {
                Text(stringResource(R.string.my_data_privacy_action))
            }
        }
    }
    if (uiState.confirmingDelete) {
        AlertDialog(
            onDismissRequest = actions.onDeleteDismiss,
            title = { Text(stringResource(R.string.my_data_delete_confirm_title)) },
            text = { Text(stringResource(R.string.my_data_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = actions.onDeleteConfirm, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                    Text(stringResource(R.string.my_data_delete_confirm_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onDeleteDismiss, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                    Text(stringResource(R.string.my_data_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun LockSetting(
    uiState: MyDataUiState,
    onLockChange: (Boolean) -> Unit,
) {
    val spacing = Spacing()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.touchTarget)
                .toggleable(
                    value = uiState.lockEnabled,
                    enabled = uiState.lockAvailable || uiState.lockEnabled,
                    role = Role.Switch,
                    onValueChange = onLockChange,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.my_data_lock_title), style = MaterialTheme.typography.titleMedium)
            val hint = if (uiState.lockAvailable) R.string.my_data_lock_body else R.string.my_data_lock_unavailable
            Text(text = stringResource(hint), style = MaterialTheme.typography.bodyMedium)
        }
        // Le clic est porté par la ligne entière (toggleable) : le Switch seul ne doit pas être un second point d'accès.
        Switch(checked = uiState.lockEnabled, onCheckedChange = null, enabled = uiState.lockAvailable || uiState.lockEnabled)
    }
}

@Composable
private fun ExportFeedback(status: ExportStatus) {
    val message =
        when (status) {
            ExportStatus.DONE -> R.string.my_data_export_done
            ExportStatus.FAILED -> R.string.my_data_export_failed
            ExportStatus.IDLE, ExportStatus.RUNNING -> null
        } ?: return
    val color = if (status == ExportStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Text(text = stringResource(message), style = MaterialTheme.typography.bodyMedium, color = color)
}

private val previewActions = MyDataActions({}, {}, {}, {}, {}, {}, {})

@Preview(showBackground = true)
@Composable
private fun MyDataScreenIdlePreview() {
    BetterHusbandTheme(dynamicColor = false) { MyDataScreen(MyDataUiState(), previewActions) }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun MyDataScreenExportFailedPreview() {
    BetterHusbandTheme(dynamicColor = false) { MyDataScreen(MyDataUiState(exportStatus = ExportStatus.FAILED), previewActions) }
}

@Preview(showBackground = true)
@Composable
private fun MyDataScreenConfirmPreview() {
    BetterHusbandTheme(dynamicColor = false) { MyDataScreen(MyDataUiState(confirmingDelete = true), previewActions) }
}
