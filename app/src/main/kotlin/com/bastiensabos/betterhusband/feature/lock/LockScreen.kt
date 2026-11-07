package com.bastiensabos.betterhusband.feature.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Écran de verrouillage : ne montre aucune donnée. La demande système s'ouvre toute seule ; le bouton sert à
 * recommencer si elle a été fermée.
 */
@Composable
fun LockRoute(
    authenticate: suspend () -> Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentAuthenticate by rememberUpdatedState(authenticate)
    val currentOnUnlock by rememberUpdatedState(onUnlock)
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun attempt() {
        if (currentAuthenticate()) currentOnUnlock() else failed = true
    }
    LaunchedEffect(Unit) { attempt() }
    LockScreen(failed = failed, onRetry = { scope.launch { attempt() } }, modifier = modifier)
}

@Composable
fun LockScreen(
    failed: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = Spacing()
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(spacing.screen),
            verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.lock_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (failed) {
                Text(text = stringResource(R.string.lock_failed), style = MaterialTheme.typography.bodyLarge)
            }
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget)) {
                Text(stringResource(R.string.lock_unlock))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LockScreenPreview() {
    BetterHusbandTheme(dynamicColor = false) { LockScreen(failed = false, onRetry = {}) }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun LockScreenFailedPreview() {
    BetterHusbandTheme(dynamicColor = false) { LockScreen(failed = true, onRetry = {}) }
}
