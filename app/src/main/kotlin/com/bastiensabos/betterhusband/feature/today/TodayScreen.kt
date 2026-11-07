package com.bastiensabos.betterhusband.feature.today

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme

/** Point d'entrée « stateful » : branche le ViewModel. */
@Composable
fun TodayRoute(
    onOpenPartner: () -> Unit,
    onOpenDates: () -> Unit,
    onOpenIdeas: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        uiState = uiState,
        onCardDone = viewModel::onCardDone,
        onOpenPartner = onOpenPartner,
        onOpenDates = onOpenDates,
        onOpenIdeas = onOpenIdeas,
        modifier = modifier,
    )
}

/** Écran « stateless » : ne dépend que de son état et de ses callbacks, donc prévisualisable et testable. */
@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onCardDone: (String) -> Unit,
    onOpenPartner: () -> Unit,
    onOpenDates: () -> Unit,
    onOpenIdeas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(R.string.today_title), style = MaterialTheme.typography.headlineMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onOpenPartner, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.today_open_partner))
                }
                TextButton(onClick = onOpenDates, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.today_open_dates))
                }
                TextButton(onClick = onOpenIdeas, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.today_open_ideas))
                }
            }
            when (uiState) {
                TodayUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                TodayUiState.Empty -> {
                    Text(text = stringResource(R.string.today_empty))
                }

                is TodayUiState.Ready -> {
                    LazyColumn {
                        items(uiState.cards, key = { it.id }) { card ->
                            ListItem(
                                headlineContent = { Text(card.title) },
                                trailingContent = {
                                    Checkbox(checked = card.done, onCheckedChange = { onCardDone(card.id) })
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenReadyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        TodayScreen(
            uiState = TodayUiState.Ready(
                listOf(
                    TodayCard("1", "Lui écrire un mot doux"),
                    TodayCard("2", "Sortir les poubelles", done = true),
                ),
            ),
            onCardDone = {},
            onOpenPartner = {},
            onOpenDates = {},
            onOpenIdeas = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenEmptyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        TodayScreen(uiState = TodayUiState.Empty, onCardDone = {}, onOpenPartner = {}, onOpenDates = {}, onOpenIdeas = {})
    }
}
