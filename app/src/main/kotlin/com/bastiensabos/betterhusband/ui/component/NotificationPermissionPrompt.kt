package com.bastiensabos.betterhusband.ui.component

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.ui.theme.Spacing

/**
 * Explique l'intérêt des rappels puis demande la permission (Android 13+). N'affiche rien tant que c'est inutile :
 * versions plus anciennes, permission déjà accordée, ou aperçu Compose.
 */
@Composable
fun NotificationPermissionPrompt(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || LocalInspectionMode.current) return
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    if (granted) return

    val spacing = Spacing()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Text(text = stringResource(R.string.notif_permission_prompt), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(
            onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            modifier = Modifier.heightIn(min = spacing.touchTarget),
        ) {
            Text(stringResource(R.string.notif_permission_action))
        }
    }
}
