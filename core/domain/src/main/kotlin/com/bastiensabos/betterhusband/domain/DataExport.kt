package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.AttentionLogEntry
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
            "importantDates" to
                dates.map {
                    linkedMapOf(
                        "title" to it.title,
                        "kind" to it.kind.name,
                        "date" to it.date.toString(),
                        "recurring" to it.recurring,
                    )
                },
            "ideas" to
                ideas.map {
                    linkedMapOf(
                        "text" to it.text,
                        "category" to it.category.name,
                        "createdAt" to it.createdAt.toString(),
                        "done" to it.done,
                    )
                },
            "householdTasks" to
                tasks.map {
                    linkedMapOf(
                        "title" to it.title,
                        "repeat" to it.repeat.name,
                        "createdOn" to it.createdOn.toString(),
                        "lastDoneOn" to it.lastDoneOn?.toString(),
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

    private const val INDENT = "  "
    private const val HEX_RADIX = 16
    private const val UNICODE_DIGITS = 4
}
