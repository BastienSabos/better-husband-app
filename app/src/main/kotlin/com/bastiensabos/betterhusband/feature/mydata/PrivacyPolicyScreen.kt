package com.bastiensabos.betterhusband.feature.mydata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.ui.component.Heading
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing

/** Titre et corps de chaque rubrique de la politique, dans l'ordre d'affichage. */
private val sections =
    listOf(
        R.string.privacy_collect_title to R.string.privacy_collect_body,
        R.string.privacy_local_title to R.string.privacy_local_body,
        R.string.privacy_export_title to R.string.privacy_export_body,
        R.string.privacy_lock_title to R.string.privacy_lock_body,
        R.string.privacy_erase_title to R.string.privacy_erase_body,
        R.string.privacy_third_title to R.string.privacy_third_body,
    )

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = Spacing()
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screen),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                Text(stringResource(R.string.partner_profile_back))
            }
            Heading(stringResource(R.string.privacy_title), MaterialTheme.typography.headlineMedium)
            Text(text = stringResource(R.string.privacy_updated), style = MaterialTheme.typography.bodyMedium)
            Text(text = stringResource(R.string.privacy_intro), style = MaterialTheme.typography.bodyLarge)
            sections.forEach { (title, body) ->
                Heading(stringResource(title), MaterialTheme.typography.titleMedium)
                Text(text = stringResource(body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrivacyPolicyScreenPreview() {
    BetterHusbandTheme(dynamicColor = false) { PrivacyPolicyScreen(onBack = {}) }
}

@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun PrivacyPolicyScreenLargeFontPreview() {
    BetterHusbandTheme(dynamicColor = false) { PrivacyPolicyScreen(onBack = {}) }
}
