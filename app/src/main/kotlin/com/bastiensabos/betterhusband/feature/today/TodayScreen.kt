package com.bastiensabos.betterhusband.feature.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.DateLabel
import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import com.bastiensabos.betterhusband.feature.dates.daysUntilText
import com.bastiensabos.betterhusband.feature.dates.text
import com.bastiensabos.betterhusband.ui.component.Heading
import com.bastiensabos.betterhusband.ui.theme.AppTheme
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

data class TodayActions(
    val onAttentionDone: (Boolean) -> Unit,
    val onOtherAttention: () -> Unit,
    val onTaskDone: (Long) -> Unit,
    val onOpenPartner: () -> Unit,
    val onOpenIdeas: () -> Unit,
    val onOpenTasks: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenCoupleProfile: () -> Unit,
)

/** Point d'entrée « stateful » : branche le ViewModel. */
@Composable
fun TodayRoute(
    onOpenPartner: () -> Unit,
    onOpenIdeas: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCoupleProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Retour au premier plan ou minuit passé écran ouvert : l'accueil passe au nouveau jour.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDay()
        onPauseOrDispose {}
    }
    LaunchedEffect(viewModel) {
        while (true) {
            delay(viewModel.millisUntilNextDay())
            viewModel.refreshDay()
        }
    }
    TodayScreen(
        uiState = uiState,
        actions =
            TodayActions(
                onAttentionDone = viewModel::onAttentionDone,
                onOtherAttention = viewModel::onOtherAttention,
                onTaskDone = viewModel::onTaskDone,
                onOpenPartner = onOpenPartner,
                onOpenIdeas = onOpenIdeas,
                onOpenTasks = onOpenTasks,
                onOpenSettings = onOpenSettings,
                onOpenCoupleProfile = onOpenCoupleProfile,
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
            val ready = uiState as? TodayUiState.Ready
            TodayHeader(
                date = ready?.date,
                partnerName = ready?.partnerName,
                onOpenPartner = actions.onOpenPartner,
                onOpenSettings = actions.onOpenSettings,
            )
            when (uiState) {
                TodayUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                is TodayUiState.Ready -> {
                    if (uiState.attention.done) {
                        AttentionDoneCard(uiState.streak, onUndo = { actions.onAttentionDone(false) })
                    } else {
                        AttentionCardView(uiState.attention, actions)
                    }
                    StreakCard(uiState.streak, uiState.week, uiState.date)
                    if (uiState.suggestCoupleProfile) CoupleProfilePrompt(actions.onOpenCoupleProfile)
                    uiState.nextDate?.let { PrepareCard(it, uiState.nextDateIdeas, actions.onOpenIdeas) }
                    TasksSection(uiState, actions)
                }
            }
        }
    }
}

/** Date du jour, salutation, réglages et avatar du ou de la partenaire (vers sa fiche). */
@Composable
private fun TodayHeader(
    date: LocalDate?,
    partnerName: String?,
    onOpenPartner: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val spacing = AppTheme.spacing
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        Column(modifier = Modifier.weight(1f)) {
            if (date != null) {
                Text(
                    text = longDate(date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Heading(text = stringResource(R.string.today_greeting), style = MaterialTheme.typography.headlineMedium)
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
        }
        if (partnerName != null) {
            val description = stringResource(R.string.today_open_partner_of, partnerName)
            FilledTonalIconButton(
                onClick = onOpenPartner,
                modifier = Modifier.size(spacing.touchTarget).semantics { contentDescription = description },
            ) {
                Text(
                    text = partnerName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}

/** « Samedi 7 novembre », dans la langue de l'appareil. */
@Composable
private fun longDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) {
        date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)).replaceFirstChar { it.titlecase(locale) }
    }
}

/** La carte du jour : la seule carte en bordeaux plein de l'appli. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttentionCardView(
    attention: AttentionCard,
    actions: TodayActions,
) {
    val spacing = AppTheme.spacing
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.primary, contentColor = colors.onPrimary),
    ) {
        Column(modifier = Modifier.padding(spacing.screen), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                Heading(text = stringResource(R.string.today_attention_title), style = MaterialTheme.typography.labelLarge)
            }
            Text(text = attention.text, style = MaterialTheme.typography.headlineSmall)
            // FlowRow : en police agrandie, « Autre idée » passe sous « C'est fait » au lieu de l'écraser.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                modifier = Modifier.padding(top = spacing.xxs),
            ) {
                Button(
                    onClick = { actions.onAttentionDone(true) },
                    modifier = Modifier.heightIn(min = spacing.touchTarget),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.onPrimary, contentColor = colors.primary),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text(stringResource(R.string.today_attention_done), modifier = Modifier.padding(start = spacing.xs))
                }
                OutlinedButton(
                    onClick = actions.onOtherAttention,
                    modifier = Modifier.heightIn(min = spacing.touchTarget),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onPrimary),
                    border = BorderStroke(1.5.dp, colors.primaryContainer),
                ) {
                    Text(stringResource(R.string.today_attention_other))
                }
            }
        }
    }
}

/** Le geste est fait : on le salue, sans en faire trop, et on laisse revenir en arrière. */
@Composable
private fun AttentionDoneCard(
    streak: Int,
    onUndo: () -> Unit,
) {
    val spacing = AppTheme.spacing
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.tertiaryContainer, contentColor = colors.onTertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Box(
                modifier = Modifier.size(44.dp).background(colors.tertiary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onTertiary)
            }
            Heading(text = stringResource(R.string.today_attention_done_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = pluralStringResource(R.plurals.today_attention_done_body, streak, streak),
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(
                onClick = onUndo,
                modifier = Modifier.heightIn(min = spacing.touchTarget),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onTertiaryContainer),
            ) {
                Text(stringResource(R.string.today_attention_undo), textDecoration = TextDecoration.Underline)
            }
        }
    }
}

/** La série et les sept jours de la semaine : valoriser la régularité, sans points ni classement. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StreakCard(
    streak: Int,
    week: List<Boolean>,
    today: LocalDate,
) {
    val spacing = AppTheme.spacing
    val weekDescription = pluralStringResource(R.plurals.today_week_description, week.count { it }, week.count { it })
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).widthIn(min = 140.dp)) {
                Text(
                    text =
                        if (streak > 0) {
                            pluralStringResource(R.plurals.today_streak, streak, streak)
                        } else {
                            stringResource(R.string.today_streak_none)
                        },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.today_streak_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WeekDots(week, today, Modifier.clearAndSetSemantics { contentDescription = weekDescription })
        }
    }
}

@Composable
private fun WeekDots(
    week: List<Boolean>,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val colors = MaterialTheme.colorScheme
    val todayIndex = today.dayOfWeek.value - 1
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        week.forEachIndexed { index, done ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val dot = Modifier.size(12.dp)
                Box(
                    modifier =
                        when {
                            done -> dot.background(colors.primary, CircleShape)
                            index == todayIndex -> dot.border(2.dp, colors.primary, CircleShape)
                            else -> dot.background(colors.surfaceContainerHighest, CircleShape)
                        },
                )
                Text(
                    text = DayOfWeek.of(index + 1).getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CoupleProfilePrompt(onOpen: () -> Unit) {
    val spacing = AppTheme.spacing
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Heading(text = stringResource(R.string.today_couple_prompt_title), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.today_couple_prompt_body), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onOpen, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                Text(stringResource(R.string.today_couple_prompt_action))
            }
        }
    }
}

/** La prochaine date, avec ce que le carnet propose pour la préparer, et une action pour s'y mettre. */
@Composable
private fun PrepareCard(
    date: UpcomingDate,
    ideas: List<String>,
    onOpenIdeas: () -> Unit,
) {
    val spacing = AppTheme.spacing
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val dateText = remember(date.date, locale) { date.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)) }
    val gift = date.kind != DateKind.OTHER
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Box(
                    modifier = Modifier.size(spacing.touchTarget).background(colors.secondaryContainer, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = prepareIcon(gift),
                        contentDescription = null,
                        tint = colors.onSecondaryContainer,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Heading(
                        text = stringResource(R.string.today_next_date_title),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(text = date.label.text(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.today_prepare_when, daysUntilText(date.daysUntil), dateText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Text(
                text =
                    if (ideas.isEmpty()) {
                        stringResource(R.string.today_prepare_no_idea)
                    } else {
                        stringResource(R.string.today_prepare_ideas, ideas.joinToString(", "))
                    },
                style = MaterialTheme.typography.bodyMedium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainer, MaterialTheme.shapes.medium)
                        .padding(horizontal = spacing.sm, vertical = spacing.sm),
            )
            FilledTonalButton(onClick = onOpenIdeas, modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget)) {
                Text(
                    stringResource(
                        when {
                            ideas.isEmpty() -> R.string.today_prepare_add_idea
                            gift -> R.string.today_prepare_choose_gift
                            else -> R.string.today_prepare_open_ideas
                        },
                    ),
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

private fun prepareIcon(gift: Boolean): ImageVector = if (gift) Icons.Outlined.CardGiftcard else Icons.Outlined.Event

@Composable
private fun TasksSection(
    state: TodayUiState.Ready,
    actions: TodayActions,
) {
    val spacing = AppTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Heading(
                text = stringResource(R.string.today_tasks_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = actions.onOpenTasks, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                Text(
                    if (state.moreTasks > 0) {
                        pluralStringResource(R.plurals.today_tasks_more, state.moreTasks, state.moreTasks)
                    } else {
                        stringResource(R.string.today_tasks_see_all)
                    },
                )
            }
        }
        if (state.tasks.isEmpty()) {
            Text(
                text = stringResource(R.string.today_tasks_none),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.tasks.forEach { task -> TaskRow(task, onDone = { actions.onTaskDone(task.id) }) }
    }
}

/** Toute la ligne coche la tâche : une grande cible, et TalkBack lit son nom avec l'état de la case. */
@Composable
private fun TaskRow(
    task: TodayTask,
    onDone: () -> Unit,
) {
    val spacing = AppTheme.spacing
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(value = false, role = Role.Checkbox, onValueChange = { onDone() })
                    .padding(horizontal = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = false, onCheckedChange = null, modifier = Modifier.padding(spacing.sm))
            Text(text = task.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(end = spacing.sm))
        }
    }
}

private val previewActions = TodayActions({}, {}, {}, {}, {}, {}, {}, {})

private val previewDay = LocalDate.of(2026, 11, 6)

private val previewDate =
    UpcomingDate("a", DateLabel.PartnerBirthday("Camille"), DateKind.BIRTHDAY, LocalDate.of(2026, 11, 21), 15, 34, null)

internal val previewReady =
    TodayUiState.Ready(
        date = previewDay,
        attention = AttentionCard("Glisse un mot doux dans la poche de son manteau.", done = false),
        tasks = listOf(TodayTask(1, "Sortir le verre et les cartons", 0), TodayTask(2, "Réserver la baby-sitter", 1)),
        moreTasks = 0,
        nextDate = previewDate,
        nextDateIdeas = listOf("coffret de thés fumés", "atelier poterie à deux"),
        streak = 5,
        week = listOf(true, true, true, true, false, false, false),
        partnerName = "Camille",
    )

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun TodayScreenReadyPreview() {
    BetterHusbandTheme {
        TodayScreen(uiState = previewReady, actions = previewActions)
    }
}

@Preview(showBackground = true, fontScale = 2f, heightDp = 1600)
@Composable
private fun TodayScreenDonePreview() {
    BetterHusbandTheme {
        TodayScreen(
            uiState =
                previewReady.copy(
                    attention = previewReady.attention.copy(done = true),
                    streak = 6,
                    week = listOf(true, true, true, true, true, false, false),
                    nextDateIdeas = emptyList(),
                    tasks = emptyList(),
                ),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenLoadingPreview() {
    BetterHusbandTheme {
        TodayScreen(uiState = TodayUiState.Loading, actions = previewActions)
    }
}
