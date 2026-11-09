package com.bastiensabos.betterhusband.domain.duo

import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.SharedRecord
import com.bastiensabos.betterhusband.domain.model.SharedType
import com.bastiensabos.betterhusband.domain.model.SyncMeta
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Texte en clair d'un bloc, avant chiffrement (`docs/duo.md`, « Format d'un bloc ») : un JSON versionné, sans champ
 * superflu. À la lecture, un type inconnu ou un champ mal formé lève [UnreadableBlockException] ; un champ inconnu
 * d'une version future est ignoré.
 */
object DuoCodec {
    const val VERSION = 1

    fun encode(record: SharedRecord): ByteArray {
        val json =
            buildJsonObject {
                put("v", VERSION)
                put("type", record.type.name.lowercase())
                put("uid", record.meta.uid)
                put("revision", record.meta.revision)
                put("author", record.meta.author)
                put("deleted", record.meta.deleted)
                put("data", data(record))
            }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): SharedRecord {
        val root = parse(bytes)
        if (root.long("v") != VERSION.toLong()) fail("Version de bloc inconnue")
        val meta =
            SyncMeta(
                uid = root.string("uid"),
                revision = root.long("revision"),
                author = root.string("author"),
                deleted = root.boolean("deleted"),
            )
        val data = root["data"] as? JsonObject ?: fail("Données absentes")
        return when (root.string("type")) {
            "wish" -> {
                SharedRecord.Wish(meta, owner = data.string("owner"), text = data.string("text"), link = data.stringOrNull("link"))
            }

            "task" -> {
                SharedRecord.Task(
                    meta,
                    title = data.string("title"),
                    repeat = data.enum<TaskRepeat>("repeat"),
                    createdOn = data.date("createdOn"),
                    lastDoneOn = data.stringOrNull("lastDoneOn")?.let(::parseDate),
                    lastDoneBy = data.stringOrNull("lastDoneBy"),
                    assignee = data.stringOrNull("assignee"),
                )
            }

            "date" -> {
                SharedRecord.Date(
                    meta,
                    title = data.string("title"),
                    kind = data.enum<DateKind>("kind"),
                    date = data.date("date"),
                    recurring = data.boolean("recurring"),
                )
            }

            else -> {
                fail("Type de bloc inconnu")
            }
        }
    }

    private fun data(record: SharedRecord): JsonObject =
        buildJsonObject {
            when (record) {
                is SharedRecord.Wish -> {
                    put("owner", record.owner)
                    put("text", record.text)
                    put("link", record.link)
                }

                is SharedRecord.Task -> {
                    put("title", record.title)
                    put("repeat", record.repeat.name)
                    put("createdOn", record.createdOn.toString())
                    put("lastDoneOn", record.lastDoneOn?.toString())
                    put("lastDoneBy", record.lastDoneBy)
                    put("assignee", record.assignee)
                }

                is SharedRecord.Date -> {
                    put("title", record.title)
                    put("kind", record.kind.name)
                    put("date", record.date.toString())
                    put("recurring", record.recurring)
                }
            }
        }
}

private fun parse(bytes: ByteArray): JsonObject =
    try {
        Json.parseToJsonElement(String(bytes, Charsets.UTF_8)) as? JsonObject ?: fail("Bloc sans objet")
    } catch (e: IllegalArgumentException) {
        throw UnreadableBlockException("Bloc illisible", e)
    }

private fun JsonObject.primitive(key: String): JsonPrimitive? = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }

private fun JsonObject.string(key: String): String = primitive(key)?.takeIf { it.isString }?.content ?: fail("Champ manquant : $key")

private fun JsonObject.stringOrNull(key: String): String? {
    val element: JsonElement? = this[key]
    return if (element == null || element is JsonNull) null else string(key)
}

private fun JsonObject.long(key: String): Long = primitive(key)?.takeIf { !it.isString }?.longOrNull ?: fail("Champ manquant : $key")

private fun JsonObject.boolean(key: String): Boolean =
    primitive(key)?.takeIf { !it.isString }?.booleanOrNull ?: fail("Champ manquant : $key")

private fun JsonObject.date(key: String): LocalDate = parseDate(string(key))

private fun parseDate(text: String): LocalDate =
    try {
        LocalDate.parse(text)
    } catch (e: DateTimeParseException) {
        throw UnreadableBlockException("Date illisible", e)
    }

private inline fun <reified E : Enum<E>> JsonObject.enum(key: String): E =
    enumValues<E>().firstOrNull { it.name == string(key) } ?: fail("Valeur inconnue : $key")

private fun fail(message: String): Nothing = throw UnreadableBlockException(message)
