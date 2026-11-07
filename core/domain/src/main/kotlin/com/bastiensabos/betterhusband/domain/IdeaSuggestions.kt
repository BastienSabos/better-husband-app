package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.Idea
import com.bastiensabos.betterhusband.domain.model.IdeaCategory

/** Les idées du carnet à rappeler pour préparer une date : ce qui a été noté sert au bon moment. */
object IdeaSuggestions {
    /**
     * Au plus [limit] idées pas encore faites. Pour un anniversaire, les cadeaux d'abord, puis les sorties ; pour un
     * autre événement, les sorties d'abord. À catégorie égale, la plus récente d'abord.
     */
    fun forDate(
        ideas: List<Idea>,
        kind: DateKind,
        limit: Int = 2,
    ): List<Idea> {
        val order =
            when (kind) {
                DateKind.BIRTHDAY, DateKind.ANNIVERSARY -> listOf(IdeaCategory.GIFT, IdeaCategory.OUTING, IdeaCategory.ATTENTION)
                DateKind.OTHER -> listOf(IdeaCategory.OUTING, IdeaCategory.GIFT, IdeaCategory.ATTENTION)
            }
        return ideas
            .filterNot { it.done }
            .sortedWith(compareBy<Idea> { order.indexOf(it.category) }.thenByDescending { it.createdAt })
            .take(limit)
    }
}
