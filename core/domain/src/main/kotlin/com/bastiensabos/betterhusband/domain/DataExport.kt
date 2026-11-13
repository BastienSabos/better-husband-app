package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.AttentionLogEntry
import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.Idea
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import com.bastiensabos.betterhusband.domain.model.Partner
import java.time.Instant

/** Tout ce que l'application sait de l'utilisateur, au moment de l'export (droit d'accès et de portabilité, RGPD). */
data class UserDataSnapshot(
    val partner: Partner?,
    val dates: List<ImportantDate>,
    val ideas: List<Idea>,
    val tasks: List<HouseholdTask>,
    val attentions: List<AttentionLogEntry>,
    val exportedAt: Instant,
    val coupleProfile: CoupleProfile = CoupleProfile(),
    /** Envies du duo, les miennes et celles de l'autre (vides en solo). */
    val wishes: List<ExportedWish> = emptyList(),
    /** Mon identifiant de membre en duo : sert à écrire « me » ou « partner » plutôt qu'un identifiant opaque. */
    val myMemberId: String? = null,
)

/** Une envie du duo telle qu'exportée : [mine] dit si c'est la mienne ou celle de l'autre. */
data class ExportedWish(
    val text: String,
    val createdAt: Instant,
    val mine: Boolean,
)

/**
 * Écrit un [UserDataSnapshot] en JSON lisible. Le format est écrit à la main : pas de bibliothèque, donc pas de
 * dépendance de plus à auditer pour des données intimes. Les énumérations sortent par leur nom, les dates en ISO 8601.
 */
object DataExport {
    const val FORMAT_VERSION = 1

    fun toJson(snapshot: UserDataSnapshot): String = buildString { render(snapshot.toTree(), 0) }.plus("\n")

    private fun UserDataSnapshot.toTree(): Map<String, Any?> =
        linkedMapOf(
            "formatVersion" to FORMAT_VERSION,
            "exportedAt" to exportedAt.toString(),
            "partner" to partner?.toTree(),
            "coupleProfile" to coupleProfile.toTree(),
            "importantDates" to
                dates.map {
                    linkedMapOf(
                        "title" to it.title,
                        "kind" to it.kind.name,
                        "date" to it.date.toString(),
                        "recurring" to it.recurring,
                        "shared" to it.shared,
                    )
                },
            "ideas" to
                ideas.map {
                    linkedMapOf(
                        "text" to it.text,
                        "category" to it.category.name,
                        "createdAt" to it.createdAt.toString(),
                        "done" to it.done,
                        "fromPartnerWish" to (it.wishUid != null),
                    )
                },
            "householdTasks" to
                tasks.map {
                    linkedMapOf(
                        "title" to it.title,
                        "repeat" to it.repeat.name,
                        "createdOn" to it.createdOn.toString(),
                        "lastDoneOn" to it.lastDoneOn?.toString(),
                        "assignee" to who(it.assignee),
                        "lastDoneBy" to who(it.lastDoneBy),
                    )
                },
            "attentions" to
                attentions.map {
                    linkedMapOf(
                        "date" to it.date.toString(),
                        "attentionId" to it.attentionId,
                        "done" to it.done,
                    )
                },
            "wishes" to
                wishes.map {
                    linkedMapOf(
                        "text" to it.text,
                        "createdAt" to it.createdAt.toString(),
                        "author" to if (it.mine) ME else PARTNER,
                    )
                },
        )

    /** Un membre du duo, sans son identifiant : « me », « partner » ou `null` (personne, ou solo). */
    private fun UserDataSnapshot.who(memberId: String?): String? =
        when (memberId) {
            null -> null
            myMemberId -> ME
            else -> PARTNER
        }

    private fun CoupleProfile.toTree(): Map<String, Any?> =
        linkedMapOf(
            "kids" to kids?.name,
            "living" to living?.name,
            "budget" to budget?.name,
            "rhythm" to rhythm?.name,
            "interests" to interests.map { it.name }.sorted(),
            "family" to family.map { it.name }.sorted(),
        )

    private fun Partner.toTree(): Map<String, Any?> =
        linkedMapOf(
            "firstName" to firstName,
            "addressForm" to addressForm.name,
            "birthday" to birthday?.toString(),
            "relationshipStart" to relationshipStart?.toString(),
            "tastes" to tastes.map { linkedMapOf("category" to it.category.name, "label" to it.label) },
            "sizes" to sizes.entries.associate { it.key.name to it.value },
            "relatives" to
                relatives.map {
                    linkedMapOf("name" to it.name, "relation" to it.relation, "birthday" to it.birthday?.toString())
                },
        )

    private fun StringBuilder.render(
        value: Any?,
        depth: Int,
    ) {
        when (value) {
            null -> {
                append("null")
            }

            is String -> {
                appendQuoted(value)
            }

            is Map<*, *> -> {
                renderContainer(value.entries, depth, '{', '}') { (key, item) ->
                    appendQuoted(key.toString())
                    append(": ")
                    render(item, depth + 1)
                }
            }

            is List<*> -> {
                renderContainer(value, depth, '[', ']') { render(it, depth + 1) }
            }

            else -> {
                append(value.toString())
            }
        }
    }

    private fun <T> StringBuilder.renderContainer(
        items: Collection<T>,
        depth: Int,
        open: Char,
        close: Char,
        renderItem: (T) -> Unit,
    ) {
        append(open)
        if (items.isNotEmpty()) {
            items.forEachIndexed { index, item ->
                append(if (index == 0) "\n" else ",\n")
                append(INDENT.repeat(depth + 1))
                renderItem(item)
            }
            append("\n").append(INDENT.repeat(depth))
        }
        append(close)
    }

    private fun StringBuilder.appendQuoted(text: String) {
        append('"')
        text.forEach { char ->
            when {
                char == '"' -> append("\\\"")
                char == '\\' -> append("\\\\")
                char == '\n' -> append("\\n")
                char == '\r' -> append("\\r")
                char == '\t' -> append("\\t")
                char < ' ' -> append("\\u").append(char.code.toString(HEX_RADIX).padStart(UNICODE_DIGITS, '0'))
                else -> append(char)
            }
        }
        append('"')
    }

    private const val ME = "me"
    private const val PARTNER = "partner"
    private const val INDENT = "  "
    private const val HEX_RADIX = 16
    private const val UNICODE_DIGITS = 4
}
