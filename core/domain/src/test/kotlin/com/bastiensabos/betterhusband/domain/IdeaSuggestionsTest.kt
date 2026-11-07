package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.Idea
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class IdeaSuggestionsTest {
    private fun idea(
        text: String,
        category: IdeaCategory,
        day: Long,
        done: Boolean = false,
    ) = Idea(text = text, category = category, createdAt = Instant.EPOCH.plusSeconds(day * 86_400), done = done)

    private val ideas =
        listOf(
            idea("balade", IdeaCategory.OUTING, 3),
            idea("coffret de thés", IdeaCategory.GIFT, 1),
            idea("mot doux", IdeaCategory.ATTENTION, 5),
            idea("livre", IdeaCategory.GIFT, 2),
            idea("parfum déjà offert", IdeaCategory.GIFT, 9, done = true),
        )

    @Test
    fun `a birthday brings gifts first, newest first, and skips what is done`() {
        val texts = IdeaSuggestions.forDate(ideas, DateKind.BIRTHDAY).map { it.text }

        assertThat(texts).containsExactly("livre", "coffret de thés").inOrder()
    }

    @Test
    fun `another event brings outings first`() {
        val texts = IdeaSuggestions.forDate(ideas, DateKind.OTHER, limit = 3).map { it.text }

        assertThat(texts).containsExactly("balade", "livre", "coffret de thés").inOrder()
    }

    @Test
    fun `an empty notebook gives nothing`() {
        assertThat(IdeaSuggestions.forDate(emptyList(), DateKind.ANNIVERSARY)).isEmpty()
    }
}
