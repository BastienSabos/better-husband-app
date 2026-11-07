package com.bastiensabos.betterhusband.ui.capture

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.DateLabel
import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.Idea
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import com.bastiensabos.betterhusband.feature.couple.CoupleProfileActions
import com.bastiensabos.betterhusband.feature.couple.CoupleProfileScreen
import com.bastiensabos.betterhusband.feature.couple.CoupleProfileUiState
import com.bastiensabos.betterhusband.feature.dates.DateEditorActions
import com.bastiensabos.betterhusband.feature.dates.DatesListState
import com.bastiensabos.betterhusband.feature.dates.DatesScreen
import com.bastiensabos.betterhusband.feature.dates.DatesUiState
import com.bastiensabos.betterhusband.feature.ideas.IdeasActions
import com.bastiensabos.betterhusband.feature.ideas.IdeasListState
import com.bastiensabos.betterhusband.feature.ideas.IdeasScreen
import com.bastiensabos.betterhusband.feature.ideas.IdeasUiState
import com.bastiensabos.betterhusband.feature.lock.LockScreen
import com.bastiensabos.betterhusband.feature.mydata.MyDataActions
import com.bastiensabos.betterhusband.feature.mydata.MyDataScreen
import com.bastiensabos.betterhusband.feature.mydata.MyDataUiState
import com.bastiensabos.betterhusband.feature.onboarding.OnboardingScreen
import com.bastiensabos.betterhusband.feature.onboarding.OnboardingStep
import com.bastiensabos.betterhusband.feature.onboarding.OnboardingUiState
import com.bastiensabos.betterhusband.feature.onboarding.previewOnboardingActions
import com.bastiensabos.betterhusband.feature.partner.PartnerProfileScreen
import com.bastiensabos.betterhusband.feature.partner.PartnerProfileUiState
import com.bastiensabos.betterhusband.feature.partner.PartnerSection
import com.bastiensabos.betterhusband.feature.partner.previewActions
import com.bastiensabos.betterhusband.feature.partner.previewProfile
import com.bastiensabos.betterhusband.feature.settings.SettingsScreen
import com.bastiensabos.betterhusband.feature.tasks.TaskItem
import com.bastiensabos.betterhusband.feature.tasks.TasksActions
import com.bastiensabos.betterhusband.feature.tasks.TasksListState
import com.bastiensabos.betterhusband.feature.tasks.TasksScreen
import com.bastiensabos.betterhusband.feature.tasks.TasksUiState
import com.bastiensabos.betterhusband.feature.today.TodayActions
import com.bastiensabos.betterhusband.feature.today.TodayScreen
import com.bastiensabos.betterhusband.feature.today.previewReady
import com.bastiensabos.betterhusband.ui.navigation.AppNavigationBar
import com.bastiensabos.betterhusband.ui.navigation.Routes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate

/** Chaque écran dans ses états principaux, avec des données fictives (Camille, Léa). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Application nue : celle de l'appli charge SQLCipher, absent de la JVM.
@Config(application = Application::class)
class ScreensCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val day = LocalDate.of(2026, 11, 7)
    private val todayActions = TodayActions({}, {}, {}, {}, {}, {}, {}, {})

    @Test
    fun today() = compose.captureScreen("aujourdhui") { TodayScreen(previewReady, todayActions) }

    @Test
    fun todayDone() =
        compose.captureScreen("aujourdhui_fait") {
            TodayScreen(
                previewReady.copy(
                    attention = previewReady.attention.copy(done = true),
                    streak = 6,
                    week = listOf(true, true, true, true, true, false, false),
                    nextDateIdeas = emptyList(),
                    suggestCoupleProfile = true,
                ),
                todayActions,
            )
        }

    @Test
    fun navigationBar() = compose.captureScreen("barre_du_bas") { AppNavigationBar(tab = Routes.TODAY, onOpen = {}) }

    @Test
    fun settings() = compose.captureScreen("reglages") { SettingsScreen(onBack = {}, onOpenCoupleProfile = {}, onOpenMyData = {}) }

    @Test
    fun onboarding() = compose.captureScreen("onboarding") { OnboardingScreen(OnboardingUiState(), previewOnboardingActions) }

    @Test
    fun onboardingPartner() =
        compose.captureScreen("onboarding_partenaire") {
            OnboardingScreen(
                OnboardingUiState(step = OnboardingStep.PARTNER, firstName = "Camille", addressForm = AddressForm.FEMININE),
                previewOnboardingActions,
            )
        }

    @Test
    fun onboardingBirthday() =
        compose.captureScreen("onboarding_anniversaire") {
            OnboardingScreen(OnboardingUiState(step = OnboardingStep.BIRTHDAY, firstName = "Camille"), previewOnboardingActions)
        }

    @Test
    fun dates() =
        compose.captureScreen("dates") {
            DatesScreen(
                uiState =
                    DatesUiState(
                        DatesListState.Ready(
                            listOf(
                                UpcomingDate("a", DateLabel.PartnerBirthday("Camille"), DateKind.BIRTHDAY, day.plusDays(14), 14, 34, null),
                                UpcomingDate("b", DateLabel.RelativeBirthday("Léa"), DateKind.BIRTHDAY, day.plusDays(40), 40, 37, null),
                                UpcomingDate("d", DateLabel.Custom("Réveillon à la maison"), DateKind.OTHER, day.plusDays(47), 47, null, 3),
                                UpcomingDate("c", DateLabel.Custom("Mariage"), DateKind.ANNIVERSARY, day.plusDays(102), 102, 7, 7),
                            ),
                            nextIdeas = 2,
                        ),
                    ),
                onAddClick = {},
                onEdit = {},
                editor = DateEditorActions({}, {}, {}, {}, {}, {}, {}),
            )
        }

    @Test
    fun ideas() =
        compose.captureScreen("idees") {
            IdeasScreen(
                IdeasUiState(
                    list =
                        IdeasListState.Ready(
                            listOf(
                                Idea(1, "Pivoines pour son anniversaire", IdeaCategory.GIFT, Instant.EPOCH),
                                Idea(2, "Restaurant italien du marché", IdeaCategory.OUTING, Instant.EPOCH),
                                Idea(3, "Café au lit dimanche", IdeaCategory.ATTENTION, Instant.EPOCH, done = true),
                            ),
                        ),
                ),
                IdeasActions({}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {}),
            )
        }

    @Test
    fun tasks() =
        compose.captureScreen("foyer") {
            TasksScreen(
                TasksUiState(
                    list =
                        TasksListState.Ready(
                            listOf(
                                TaskItem(HouseholdTask(1, "Sortir les poubelles", TaskRepeat.WEEKLY, day), done = false, daysUntilDue = -2),
                                TaskItem(HouseholdTask(2, "Faire une lessive", TaskRepeat.WEEKLY, day), done = false, daysUntilDue = 0),
                                TaskItem(HouseholdTask(3, "Passer l'aspirateur", TaskRepeat.BIWEEKLY, day), done = true, daysUntilDue = 12),
                            ),
                        ),
                ),
                TasksActions({}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {}),
            )
        }

    @Test
    fun partner() = compose.captureScreen("fiche") { PartnerProfileScreen(previewProfile, previewActions) }

    @Test
    fun partnerEmpty() =
        compose.captureScreen("fiche_vide") {
            PartnerProfileScreen(PartnerProfileUiState(loaded = true, hasProfile = true, firstName = "Camille"), previewActions)
        }

    @Test
    fun partnerEditing() =
        compose.captureScreen("fiche_edition") {
            PartnerProfileScreen(previewProfile.copy(editing = PartnerSection.RELATIVES), previewActions)
        }

    @Test
    fun couple() =
        compose.captureScreen("couple") {
            CoupleProfileScreen(CoupleProfileUiState(loading = false), CoupleProfileActions({}, {}, {}, {}, {}, {}, {}))
        }

    @Test
    fun myData() =
        compose.captureScreen("mes_donnees") {
            MyDataScreen(MyDataUiState(), MyDataActions({}, {}, {}, {}, {}, {}, {}))
        }

    @Test
    fun lock() = compose.captureScreen("verrou") { LockScreen(failed = false, onRetry = {}) }
}
