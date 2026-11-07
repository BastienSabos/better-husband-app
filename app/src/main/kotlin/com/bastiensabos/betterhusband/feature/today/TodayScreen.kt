package com.bastiensabos.betterhusband.feature.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.DateLabel
import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import com.bastiensabos.betterhusband.feature.dates.daysUntilText
import com.bastiensabos.betterhusband.feature.dates.text
import com.bastiensabos.betterhusband.ui.component.Heading
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing
import java.time.LocalDate

data class TodayActions(
    val onAttentionDone: (Boolean) -> Unit,
    val onOtherAttention: () -> Unit,
    val onTaskDone: (Long) -> Unit,
    val onOpenPartner: () -> Unit,
    val onOpenDates: () -> Unit,
    val onOpenIdeas: () -> Unit,
    val onOpenTasks: () -> Unit,
    val onOpenMyData: () -> Unit,
)

/** Point d'entrée « stateful » : branche le ViewModel. */
@Composable
fun TodayRoute(
    onOpenPartner: () -> Unit,
    onOpenDates: () -> Unit,
    onOpenIdeas: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenMyData: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        uiState = uiState,
        actions =
            TodayActions(
                onAttentionDone = viewModel::onAttentionDone,
                onOtherAttention = viewModel::onOtherAttention,
                onTaskDone = viewModel::onTaskDone,
                onOpenPartner = onOpenPartner,
                onOpenDates = onOpenDates,
                onOpenIdeas = onOpenIdeas,
                onOpenTasks = onOpenTasks,
                onOpenMyData = onOpenMyData,
            ),
        modifier = modifier,
    )
}

/** Écran « stateless » : ne dépend que de son état et de ses callbacks, donc prévisualisable et testable. */
@Composable
fun TodayScreen(
    uiState: TodayUiState,
    actions: TodayActions,
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
                    .padding(spacing.screen),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Heading(text = stringResource(R.string.today_title), style = MaterialTheme.typography.headlineMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                TextButton(onClick = actions.onOpenPartner, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                    Text(stringResource(R.string.today_open_partner))
                }
                TextButton(onClick = actions.onOpenDates, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                    Text(stringResource(R.string.today_open_dates))
                }
                TextButton(onClick = actions.onOpenIdeas, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                    Text(stringResource(R.string.today_open_ideas))
                }
                TextButton(onClick = actions.onOpenTasks, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                    Text(stringResource(R.string.today_open_tasks))
                }
                TextButton(onClick = actions.onOpenMyData, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                    Text(stringResource(R.string.today_open_my_data))
                }
            }
            when (uiState) {
                TodayUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                is TodayUiState.Ready -> {
                    if (uiState.streak > 0) {
                        Text(
                            text = pluralStringResource(R.plurals.today_streak, uiState.streak, uiState.streak),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    AttentionSection(uiState.attention, actions)
                    TasksSection(uiState, actions)
                    uiState.nextDate?.let { NextDateSection(it, actions.onOpenDates) }
                }
            }
        }
    }
}

@Composable
private fun AttentionSection(
    attention: AttentionCard,
    actions: TodayActions,
) {
    val spacing = Spacing()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(text = stringResource(R.string.today_attention_title), style = MaterialTheme.typography.labelLarge)
            Text(text = attention.text, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                if (attention.done) {
                    OutlinedButton(
                        onClick = { actions.onAttentionDone(false) },
                        modifier = Modifier.heightIn(min = spacing.touchTarget),
                    ) {
                        Text(stringResource(R.string.today_attention_done_state))
                    }
                } else {
                    Button(
                        onClick = { actions.onAttentionDone(true) },
                        modifier = Modifier.heightIn(min = spacing.touchTarget),
                    ) {
                        Text(stringResource(R.string.today_attention_done))
                    }
                    TextButton(onClick = actions.onOtherAttention, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                        Text(stringResource(R.string.today_attention_other))
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksSection(
    state: TodayUiState.Ready,
    actions: TodayActions,
) {
    val spacing = Spacing()
    val doneDescription = stringResource(R.string.tasks_done_description)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        Heading(text = stringResource(R.string.today_tasks_title), style = MaterialTheme.typography.titleMedium)
        if (state.tasks.isEmpty()) {
            Text(text = stringResource(R.string.today_tasks_none), style = MaterialTheme.typography.bodyLarge)
        }
        state.tasks.forEach { task ->
            ListItem(
                modifier = Modifier.heightIn(min = spacing.touchTarget),
                leadingContent = {
                    Checkbox(
                        checked = false,
                        onCheckedChange = { actions.onTaskDone(task.id) },
                        modifier = Modifier.semantics { contentDescription = doneDescription },
                    )
                },
                headlineContent = { Text(task.title) },
            )
        }
        if (state.moreTasks > 0) {
            TextButton(onClick = actions.onOpenTasks, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                Text(pluralStringResource(R.plurals.today_tasks_more, state.moreTasks, state.moreTasks))
            }
        }
    }
}

@Composable
private fun NextDateSection(
    date: UpcomingDate,
    onOpenDates: () -> Unit,
) {
    val spacing = Spacing()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        Heading(text = stringResource(R.string.today_next_date_title), style = MaterialTheme.typography.titleMedium)
        ListItem(
            modifier = Modifier.heightIn(min = spacing.touchTarget).clickable(onClick = onOpenDates),
            headlineContent = { Text(date.label.text()) },
            trailingContent = { Text(daysUntilText(date.daysUntil), style = MaterialTheme.typography.labelLarge) },
        )
    }
}

private val previewActions = TodayActions({}, {}, {}, {}, {}, {}, {}, {})

private val previewDate =
    UpcomingDate("a", DateLabel.RelativeBirthday("Léa"), DateKind.BIRTHDAY, LocalDate.of(2027, 3, 11), 6, 8, null)

@Preview(showBackground = true)
@Composable
private fun TodayScreenReadyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        TodayScreen(
            uiState =
                TodayUiState.Ready(
                    attention = AttentionCard("Écris-lui un mot doux et glisse-le dans une poche ou un sac.", done = false),
                    tasks = listOf(TodayTask(1, "Sortir les poubelles", 2), TodayTask(2, "Faire une lessive", 0)),
                    moreTasks = 2,
                    nextDate = previewDate,
                    streak = 4,
                ),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun TodayScreenDonePreview() {
    BetterHusbandTheme(dynamicColor = false) {
        TodayScreen(
            uiState =
                TodayUiState.Ready(
                    attention = AttentionCard("Prépare son petit-déjeuner préféré, sans occasion particulière.", done = true),
                    tasks = emptyList(),
                    moreTasks = 0,
                    nextDate = null,
                    streak = 0,
                ),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenLoadingPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        TodayScreen(uiState = TodayUiState.Loading, actions = previewActions)
    }
}
