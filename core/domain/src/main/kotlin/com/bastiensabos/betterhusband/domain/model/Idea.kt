package com.bastiensabos.betterhusband.domain.model

import java.time.Instant

enum class IdeaCategory {
    GIFT,
    OUTING,
    ATTENTION,
}

/** Une idée notée au fil de l'eau (« a dit aimer… »). [done] : offerte, faite ou écartée, pour ne pas se répéter. */
data class Idea(
    val id: Long = 0,
    val text: String,
    val category: IdeaCategory,
    val createdAt: Instant,
    val done: Boolean = false,
    /** L'envie de l'autre dont cette idée s'occupe (« Je m'en occupe »), ou `null`. Jamais partagée. */
    val wishUid: String? = null,
)
