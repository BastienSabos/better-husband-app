package com.bastiensabos.betterhusband.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.ui.component.BackTopBar
import com.bastiensabos.betterhusband.ui.theme.AppTheme
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme

/** Réglages : ce qui ne sert pas tous les jours (profil du couple, données et verrouillage). */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCoupleProfile: () -> Unit,
    onOpenMyData: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenDuo: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = { BackTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingsRow(Icons.Outlined.FavoriteBorder, R.string.today_open_couple, R.string.settings_couple_body, onOpenCoupleProfile)
            onOpenDuo?.let { SettingsRow(Icons.Outlined.Group, R.string.settings_duo, R.string.settings_duo_body, it) }
            SettingsRow(Icons.Outlined.Lock, R.string.today_open_my_data, R.string.settings_my_data_body, onOpenMyData)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    @StringRes title: Int,
    @StringRes body: Int,
    onClick: () -> Unit,
) {
    ListItem(
        modifier =
            Modifier
                .heightIn(min = AppTheme.spacing.touchTarget)
                .clickable(role = Role.Button, onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(stringResource(title), style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(stringResource(body)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    BetterHusbandTheme(dynamicColor = false) { SettingsScreen(onBack = {}, onOpenCoupleProfile = {}, onOpenMyData = {}) }
}

@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun SettingsScreenLargeFontPreview() {
    BetterHusbandTheme(dynamicColor = false) { SettingsScreen(onBack = {}, onOpenCoupleProfile = {}, onOpenMyData = {}) }
}
