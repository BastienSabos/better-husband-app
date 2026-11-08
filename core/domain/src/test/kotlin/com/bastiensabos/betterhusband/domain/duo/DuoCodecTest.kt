package com.bastiensabos.betterhusband.domain.duo

import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.SharedRecord
import com.bastiensabos.betterhusband.domain.model.SyncMeta
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class DuoCodecTest {
    private val meta = SyncMeta("0f8b2c1e-6a4d-4c3b-9e2f-1a2b3c4d5e6f", 1_800_000_000_000, "alex")

    private val wish = SharedRecord.Wish(meta, owner = "alex", text = "Un carnet « à croquis »", link = "https://exemple.fr/c?a=1&b=2")
    private val task =
        SharedRecord.Task(
            meta.copy(deleted = true),
            title = "Sortir les poubelles",
            repeat = TaskRepeat.BIWEEKLY,
            createdOn = LocalDate.of(2027, 2, 15),
            lastDoneOn = LocalDate.of(2027, 3, 1),
            lastDoneBy = "camille",
            assignee = "alex",
        )
    private val date = SharedRecord.Date(meta, "Notre rencontre", DateKind.ANNIVERSARY, LocalDate.of(2019, 6, 1), recurring = true)

    private fun text(json: String) = json.toByteArray(Charsets.UTF_8)

    @Test
    fun `every kind of record round trips`() {
        listOf(wish, task, date).forEach { assertThat(DuoCodec.decode(DuoCodec.encode(it))).isEqualTo(it) }
    }

    @Test
    fun `optional fields round trip as null`() {
        val bare = SharedRecord.Wish(meta, "alex", "Du thé")
        val minimal = task.copy(lastDoneOn = null, lastDoneBy = null, assignee = null)

        assertThat(DuoCodec.decode(DuoCodec.encode(bare))).isEqualTo(bare)
        assertThat(DuoCodec.decode(DuoCodec.encode(minimal))).isEqualTo(minimal)
    }

    @Test
    fun `the clear text carries the version and the type`() {
        val json = String(DuoCodec.encode(date), Charsets.UTF_8)

        assertThat(json).contains("\"v\":1")
        assertThat(json).contains("\"type\":\"date\"")
    }

    @Test
    fun `unknown fields of a future version are ignored`() {
        val json = String(DuoCodec.encode(wish), Charsets.UTF_8).replace("\"v\":1", "\"v\":1,\"extra\":{\"a\":1}")

        assertThat(DuoCodec.decode(text(json))).isEqualTo(wish)
    }

    @Test
    fun `an unknown version, type or enum value is refused`() {
        val good = String(DuoCodec.encode(task), Charsets.UTF_8)

        listOf(
            good.replace("\"v\":1", "\"v\":2"),
            good.replace("\"task\"", "\"idea\""),
            good.replace("BIWEEKLY", "YEARLY"),
        ).forEach { assertThrows(UnreadableBlockException::class.java) { DuoCodec.decode(text(it)) } }
    }

    @Test
    fun `malformed blocks are refused instead of crashing`() {
        listOf(
            "",
            "pas du json",
            "[]",
            "{}",
            """{"v":1,"type":"wish","uid":"u","revision":"12","author":"a","deleted":false,"data":{}}""",
            """{"v":1,"type":"wish","uid":"u","revision":1,"author":"a","deleted":"non","data":{}}""",
            """{"v":1,"type":"wish","uid":"u","revision":1,"author":"a","deleted":false}""",
            """{"v":1,"type":"wish","uid":"u","revision":1,"author":"a","deleted":false,"data":{"owner":"a"}}""",
            """{"v":1,"type":"date","uid":"u","revision":1,"author":"a","deleted":false,"data":{"title":"t","kind":"OTHER","date":"31/12/2027","recurring":true}}""",
            """{"v":1,"type":"wish","uid":"u","revision":1,"author":"a","deleted":false,"data":{"owner":"a","text":5}}""",
        ).forEach { assertThrows(it, UnreadableBlockException::class.java) { DuoCodec.decode(text(it)) } }
    }

    @Test
    fun `invalid utf8 is refused`() {
        assertThrows(UnreadableBlockException::class.java) { DuoCodec.decode(byteArrayOf(0x7B, 0xC3.toByte(), 0x28, 0x7D)) }
    }
}
