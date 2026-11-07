package com.bastiensabos.betterhusband.ui.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppShellTest {
    @Test
    fun `the four tabs are today, dates, ideas and household, in that order`() {
        assertThat(topLevels.map { it.route }).containsExactly(Routes.TODAY, Routes.DATES, Routes.IDEAS, Routes.TASKS).inOrder()
    }

    @Test
    fun `a tab screen highlights its tab`() {
        assertThat(selectedTab(Routes.IDEAS)).isEqualTo(Routes.IDEAS)
    }

    @Test
    fun `a secondary screen hides the bar`() {
        listOf(Routes.PARTNER, Routes.SETTINGS, Routes.COUPLE, Routes.MY_DATA, Routes.PRIVACY, null).forEach {
            assertThat(selectedTab(it)).isNull()
        }
    }

    @Test
    fun `the rail replaces the bar from 600 dp`() {
        assertThat(usesRail(599)).isFalse()
        assertThat(usesRail(600)).isTrue()
    }
}
