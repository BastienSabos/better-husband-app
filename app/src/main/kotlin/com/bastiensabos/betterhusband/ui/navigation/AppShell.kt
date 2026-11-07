package com.bastiensabos.betterhusband.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.feature.couple.CoupleProfileRoute
import com.bastiensabos.betterhusband.feature.dates.DatesRoute
import com.bastiensabos.betterhusband.feature.ideas.IdeasRoute
import com.bastiensabos.betterhusband.feature.mydata.MyDataRoute
import com.bastiensabos.betterhusband.feature.mydata.PrivacyPolicyScreen
import com.bastiensabos.betterhusband.feature.partner.PartnerProfileRoute
import com.bastiensabos.betterhusband.feature.settings.SettingsScreen
import com.bastiensabos.betterhusband.feature.tasks.TasksRoute
import com.bastiensabos.betterhusband.feature.today.TodayRoute

/** Routes de l'appli. Chaque entrée de pile a ses propres ViewModels, détruits quand on la quitte. */
object Routes {
    const val TODAY = "today"
    const val DATES = "dates"
    const val IDEAS = "ideas"
    const val TASKS = "tasks"
    const val PARTNER = "partner"
    const val SETTINGS = "settings"
    const val COUPLE = "couple"
    const val MY_DATA = "my-data"
    const val PRIVACY = "privacy"
}

/** Un onglet de premier niveau : icône pleine quand il est sélectionné, en contour sinon (Material 3). */
internal data class TopLevel(
    val route: String,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

internal val topLevels =
    listOf(
        TopLevel(Routes.TODAY, R.string.nav_today, Icons.Filled.WbSunny, Icons.Outlined.WbSunny),
        TopLevel(Routes.DATES, R.string.nav_dates, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        TopLevel(Routes.IDEAS, R.string.nav_ideas, Icons.Filled.Lightbulb, Icons.Outlined.Lightbulb),
        TopLevel(Routes.TASKS, R.string.nav_tasks, Icons.Filled.Home, Icons.Outlined.Home),
    )

/**
 * L'onglet à surligner pour la route affichée, ou `null` sur un écran secondaire (fiche, réglages…), où la barre
 * disparaît pour laisser la place à la flèche de retour.
 */
internal fun selectedTab(shown: String?): String? = shown?.takeIf { route -> topLevels.any { it.route == route } }

/** Rail à gauche plutôt que barre du bas à partir de 600 dp (tablette, paysage), comme le budget planner. */
internal fun usesRail(widthDp: Int): Boolean = widthDp >= RAIL_MIN_WIDTH_DP

private const val RAIL_MIN_WIDTH_DP = 600

/** Ouvre un onglet en gardant une seule copie de chaque onglet dans la pile, avec son état (défilement, saisie). */
internal fun NavHostController.openTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Coquille de l'appli une fois l'onboarding fait : barre du bas (ou rail) à 4 onglets et écrans secondaires. */
@Composable
fun AppShell(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val tab = selectedTab(entry?.destination?.route)
    val widthDp =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.width
                .toDp()
                .value
                .toInt()
        }
    val rail = usesRail(widthDp)
    // Toucher l'onglet affiché ramène à sa racine ; un autre onglet restaure son écran.
    val openTab: (String) -> Unit = { route -> if (route == tab) nav.popBackStack(route, inclusive = false) else nav.openTopLevel(route) }
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (tab != null && !rail) AppNavigationBar(tab, openTab)
        },
    ) { padding ->
        Row(Modifier.padding(padding).consumeWindowInsets(padding)) {
            if (tab != null && rail) {
                NavigationRail {
                    topLevels.forEach { top ->
                        val selected = top.route == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { openTab(top.route) },
                            icon = { Icon(if (selected) top.selectedIcon else top.icon, contentDescription = null) },
                            label = { TabLabel(top.label) },
                        )
                    }
                }
            }
            AppNavHost(nav)
        }
    }
}

/** Barre du bas des quatre onglets, [tab] surligné. */
@Composable
internal fun AppNavigationBar(
    tab: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        topLevels.forEach { top ->
            val selected = top.route == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onOpen(top.route) },
                icon = { Icon(if (selected) top.selectedIcon else top.icon, contentDescription = null) },
                label = { TabLabel(top.label) },
            )
        }
    }
}

/**
 * Libellé d'onglet sur une ligne : en police très agrandie, il rétrécit juste ce qu'il faut pour tenir plutôt que de
 * se couper en « Aujour / d'hui ». TalkBack lit toujours le libellé entier.
 */
@Composable
private fun TabLabel(
    @StringRes label: Int,
) {
    val style = LocalTextStyle.current
    BasicText(
        text = stringResource(label),
        style = style.copy(color = LocalContentColor.current),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = MIN_TAB_LABEL_SP.sp, maxFontSize = style.fontSize),
    )
}

private const val MIN_TAB_LABEL_SP = 7

@Composable
private fun AppNavHost(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }
    NavHost(navController = nav, startDestination = Routes.TODAY) {
        composable(Routes.TODAY) {
            TodayRoute(
                onOpenPartner = { nav.navigate(Routes.PARTNER) },
                onOpenIdeas = { nav.openTopLevel(Routes.IDEAS) },
                onOpenTasks = { nav.openTopLevel(Routes.TASKS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenCoupleProfile = { nav.navigate(Routes.COUPLE) },
            )
        }
        composable(Routes.DATES) { DatesRoute(onOpenIdeas = { nav.openTopLevel(Routes.IDEAS) }) }
        composable(Routes.IDEAS) { IdeasRoute() }
        composable(Routes.TASKS) { TasksRoute() }
        composable(Routes.PARTNER) { PartnerProfileRoute(onFinish = back) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = back,
                onOpenCoupleProfile = { nav.navigate(Routes.COUPLE) },
                onOpenMyData = { nav.navigate(Routes.MY_DATA) },
            )
        }
        composable(Routes.COUPLE) { CoupleProfileRoute(onFinish = back) }
        composable(Routes.MY_DATA) { MyDataRoute(onBack = back, onOpenPrivacy = { nav.navigate(Routes.PRIVACY) }) }
        composable(Routes.PRIVACY) { PrivacyPolicyScreen(onBack = back) }
    }
}
