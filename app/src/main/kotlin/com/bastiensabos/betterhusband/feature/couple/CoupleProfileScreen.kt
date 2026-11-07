package com.bastiensabos.betterhusband.feature.couple

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.model.Budget
import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import com.bastiensabos.betterhusband.domain.model.FamilySituation
import com.bastiensabos.betterhusband.domain.model.Interest
import com.bastiensabos.betterhusband.domain.model.KidsAtHome
import com.bastiensabos.betterhusband.domain.model.LifeRhythm
import com.bastiensabos.betterhusband.domain.model.LivingArrangement
import com.bastiensabos.betterhusband.ui.component.BackTopBar
import com.bastiensabos.betterhusband.ui.component.Heading
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.bastiensabos.betterhusband.ui.theme.Spacing

data class CoupleProfileActions(
    val onBack: () -> Unit,
    val onKidsChange: (KidsAtHome) -> Unit,
    val onFamilyToggle: (FamilySituation) -> Unit,
    val onLivingChange: (LivingArrangement) -> Unit,
    val onBudgetChange: (Budget) -> Unit,
    val onRhythmChange: (LifeRhythm) -> Unit,
    val onInterestToggle: (Interest) -> Unit,
    val onSave: () -> Unit,
)

@Composable
fun CoupleProfileRoute(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CoupleProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnFinish by rememberUpdatedState(onFinish)
    LaunchedEffect(uiState.saved) { if (uiState.saved) currentOnFinish() }
    CoupleProfileScreen(
        uiState = uiState,
        actions =
            CoupleProfileActions(
                onBack = onFinish,
                onKidsChange = viewModel::onKidsChange,
                onFamilyToggle = viewModel::onFamilyToggle,
                onLivingChange = viewModel::onLivingChange,
                onBudgetChange = viewModel::onBudgetChange,
                onRhythmChange = viewModel::onRhythmChange,
                onInterestToggle = viewModel::onInterestToggle,
                onSave = viewModel::onSave,
            ),
        modifier = modifier,
    )
}

@Composable
fun CoupleProfileScreen(
    uiState: CoupleProfileUiState,
    actions: CoupleProfileActions,
    modifier: Modifier = Modifier,
) {
    val spacing = Spacing()
    Scaffold(
        modifier = modifier,
        topBar = { BackTopBar(title = stringResource(R.string.couple_title), onBack = actions.onBack) },
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
            Text(text = stringResource(R.string.couple_intro), style = MaterialTheme.typography.bodyLarge)
            if (uiState.loading) {
                CircularProgressIndicator()
            } else {
                ProfileQuestions(uiState.profile, actions)
                Button(
                    onClick = actions.onSave,
                    modifier = Modifier.fillMaxWidth().heightIn(min = spacing.touchTarget),
                ) {
                    Text(stringResource(R.string.partner_profile_save))
                }
            }
        }
    }
}

@Composable
private fun ProfileQuestions(
    profile: CoupleProfile,
    actions: CoupleProfileActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing().md)) {
        Question(R.string.couple_kids_title, KidsAtHome.entries, { it == profile.kids }, actions.onKidsChange) { it.labelRes() }
        Question(R.string.couple_family_title, FamilySituation.entries, { it in profile.family }, actions.onFamilyToggle) {
            it.labelRes()
        }
        Question(R.string.couple_living_title, LivingArrangement.entries, { it == profile.living }, actions.onLivingChange) {
            it.labelRes()
        }
        Question(R.string.couple_budget_title, Budget.entries, { it == profile.budget }, actions.onBudgetChange) { it.labelRes() }
        Question(R.string.couple_rhythm_title, LifeRhythm.entries, { it == profile.rhythm }, actions.onRhythmChange) {
            it.labelRes()
        }
        Question(R.string.couple_interests_title, Interest.entries, { it in profile.interests }, actions.onInterestToggle) {
            it.labelRes()
        }
    }
}

/** Une question, ses réponses en puces. Toucher une puce choisie la retire. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Question(
    @StringRes title: Int,
    options: List<T>,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    label: (T) -> Int,
) {
    val spacing = Spacing()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Heading(text = stringResource(title), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            options.forEach { option ->
                FilterChip(
                    selected = isSelected(option),
                    onClick = { onSelect(option) },
                    label = { Text(stringResource(label(option))) },
                    modifier = Modifier.heightIn(min = spacing.touchTarget),
                )
            }
        }
    }
}

@StringRes
private fun KidsAtHome.labelRes(): Int =
    when (this) {
        KidsAtHome.NONE -> R.string.couple_kids_none
        KidsAtHome.YOUNG -> R.string.couple_kids_young
        KidsAtHome.TEENS -> R.string.couple_kids_teens
        KidsAtHome.BOTH -> R.string.couple_kids_both
    }

@StringRes
private fun FamilySituation.labelRes(): Int =
    when (this) {
        FamilySituation.STEPFAMILY -> R.string.couple_family_stepfamily
        FamilySituation.SHARED_CUSTODY -> R.string.couple_family_shared_custody
    }

@StringRes
private fun LivingArrangement.labelRes(): Int =
    when (this) {
        LivingArrangement.TOGETHER -> R.string.couple_living_together
        LivingArrangement.APART -> R.string.couple_living_apart
        LivingArrangement.LONG_DISTANCE -> R.string.couple_living_long_distance
    }

@StringRes
private fun Budget.labelRes(): Int =
    when (this) {
        Budget.TIGHT -> R.string.couple_budget_tight
        Budget.MODERATE -> R.string.couple_budget_moderate
        Budget.COMFORTABLE -> R.string.couple_budget_comfortable
    }

@StringRes
private fun LifeRhythm.labelRes(): Int =
    when (this) {
        LifeRhythm.VERY_BUSY -> R.string.couple_rhythm_very_busy
        LifeRhythm.BALANCED -> R.string.couple_rhythm_balanced
        LifeRhythm.RELAXED -> R.string.couple_rhythm_relaxed
    }

@StringRes
private fun Interest.labelRes(): Int =
    when (this) {
        Interest.NATURE -> R.string.couple_interest_nature
        Interest.CULTURE -> R.string.couple_interest_culture
        Interest.SPORT -> R.string.couple_interest_sport
        Interest.COOKING -> R.string.couple_interest_cooking
        Interest.GAMES -> R.string.couple_interest_games
        Interest.TRAVEL -> R.string.couple_interest_travel
        Interest.MUSIC -> R.string.couple_interest_music
        Interest.READING -> R.string.couple_interest_reading
        Interest.DIY -> R.string.couple_interest_diy
        Interest.WELLBEING -> R.string.couple_interest_wellbeing
    }

private val previewActions = CoupleProfileActions({}, {}, {}, {}, {}, {}, {}, {})

@Preview(showBackground = true)
@Composable
private fun CoupleProfileEmptyPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        CoupleProfileScreen(CoupleProfileUiState(loading = false), previewActions)
    }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun CoupleProfileFilledPreview() {
    BetterHusbandTheme(dynamicColor = false) {
        val profile =
            CoupleProfile(
                kids = KidsAtHome.YOUNG,
                family = setOf(FamilySituation.STEPFAMILY),
                living = LivingArrangement.TOGETHER,
                budget = Budget.MODERATE,
                interests = setOf(Interest.NATURE, Interest.COOKING),
            )
        CoupleProfileScreen(CoupleProfileUiState(loading = false, profile = profile), previewActions)
    }
}

@Preview(showBackground = true)
@Composable
private fun CoupleProfileLoadingPreview() {
    BetterHusbandTheme(dynamicColor = false) { CoupleProfileScreen(CoupleProfileUiState(), previewActions) }
}
