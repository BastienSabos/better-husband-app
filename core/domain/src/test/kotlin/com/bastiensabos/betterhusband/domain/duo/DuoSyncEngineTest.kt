package com.bastiensabos.betterhusband.domain.duo

import com.bastiensabos.betterhusband.domain.model.SharedRecord
import com.bastiensabos.betterhusband.domain.model.SharedType
import com.bastiensabos.betterhusband.domain.model.SyncMeta
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DuoSyncEngineTest {
    private val keys = CoupleKeys(ByteArray(CoupleKeys.KEY_SIZE) { (it * 3).toByte() })
    private val server = FakeMailbox()
    private var clock = 1_800_000_000_000L
    private val alex = Phone("alex")
    private val camille = Phone("camille")

    /** Un téléphone : sa base locale simulée et son moteur de synchro, branchés sur le même serveur. */
    private inner class Phone(
        val me: String,
        val phoneKeys: CoupleKeys = keys,
    ) {
        val store = FakeStore(me, setOf("alex", "camille"))
        val engine = DuoSyncEngine(phoneKeys, server, store) { clock }

        fun write(record: SharedRecord) = store.writeLocal(record, clock)

        fun sync() = engine.sync()
    }

    private val uid1 = "11111111-1111-4111-8111-111111111111"
    private val uid2 = "22222222-2222-4222-8222-222222222222"

    private fun task(
        uid: String = uid1,
        revision: Long,
        author: String,
        title: String = "Sortir les poubelles",
        deleted: Boolean = false,
    ) = SharedRecord.Task(SyncMeta(uid, revision, author, deleted), title, TaskRepeat.WEEKLY, LocalDate.of(2027, 2, 15))

    private fun tick() {
        clock += 1_000
    }

    @Test
    fun `a record written on one phone reaches the other`() {
        alex.write(task(revision = clock, author = "alex"))

        assertThat(alex.sync()).isEqualTo(SyncResult.Done(applied = 0, pushed = 1, unreadable = 0))
        tick()
        assertThat(camille.sync()).isEqualTo(SyncResult.Done(applied = 1, pushed = 0, unreadable = 0))

        assertThat(camille.store.records()).containsExactlyElementsIn(alex.store.records())
    }

    @Test
    fun `the server only ever holds sealed blocks`() {
        alex.write(task(revision = clock, author = "alex", title = "Sortir les poubelles"))
        alex.sync()

        val stored = server.blocks.values.single()
        assertThat(String(stored.sealed, Charsets.ISO_8859_1)).doesNotContain("poubelles")
        assertThat(stored.blobId).doesNotContain(uid1.take(8))
    }

    @Test
    fun `an edit on the other phone comes back and the latest wins`() {
        alex.write(task(revision = clock, author = "alex"))
        alex.sync()
        tick()
        camille.sync()
        tick()
        camille.write(task(revision = clock, author = "camille", title = "Sortir le tri"))
        camille.sync()
        tick()
        alex.sync()

        assertThat((alex.store.find(SharedType.TASK, uid1) as SharedRecord.Task).title).isEqualTo("Sortir le tri")
        assertThat(alex.store.records()).containsExactlyElementsIn(camille.store.records())
    }

    @Test
    fun `two edits made at the same time end up identical on both phones`() {
        val at = clock
        alex.write(task(revision = at, author = "alex", title = "A"))
        camille.write(task(revision = at, author = "camille", title = "C"))
        alex.sync()
        camille.sync()
        tick()
        alex.sync()
        camille.sync()
        tick()
        alex.sync()

        assertThat(alex.store.records()).containsExactlyElementsIn(camille.store.records())
        assertThat((alex.store.find(SharedType.TASK, uid1) as SharedRecord.Task).title).isEqualTo("C")
    }

    @Test
    fun `a deletion marker travels like any other change`() {
        alex.write(task(revision = clock, author = "alex"))
        alex.sync()
        tick()
        camille.sync()
        tick()
        alex.write(task(revision = clock, author = "alex", deleted = true))
        alex.sync()
        tick()
        camille.sync()

        assertThat(
            camille.store
                .find(SharedType.TASK, uid1)!!
                .meta.deleted,
        ).isTrue()
    }

    @Test
    fun `syncing again with nothing new does nothing`() {
        alex.write(task(revision = clock, author = "alex"))
        tick()
        alex.sync()
        tick()
        camille.sync()
        tick()

        assertThat(alex.sync()).isEqualTo(SyncResult.Done(0, 0, 0))
        assertThat(camille.sync()).isEqualTo(SyncResult.Done(0, 0, 0))
    }

    @Test
    fun `what was received is not sent back`() {
        alex.write(task(revision = clock, author = "alex"))
        alex.sync()
        tick()
        camille.sync()

        assertThat(server.pushes).isEqualTo(1)
    }

    @Test
    fun `a block that arrives late inside the margin is still read`() {
        alex.write(task(uid = uid1, revision = clock, author = "alex"))
        alex.sync()
        tick()
        camille.sync()
        // Un bloc écrit avant la dernière lecture mais devenu visible après (validations dans le désordre).
        server.insertLate(sealed = alex.write2(task(uid = uid2, revision = clock, author = "alex")), serverMillis = clock - 60_000)
        tick()

        assertThat(camille.sync()).isEqualTo(SyncResult.Done(applied = 1, pushed = 0, unreadable = 0))
        assertThat(camille.store.find(SharedType.TASK, uid2)).isNotNull()
    }

    @Test
    fun `a block sealed with another key is ignored and the rest still applies`() {
        val stranger = Phone("alex", CoupleKeys(ByteArray(CoupleKeys.KEY_SIZE) { 9 }))
        stranger.write(task(uid = uid2, revision = clock, author = "alex"))
        stranger.sync()
        alex.write(task(uid = uid1, revision = clock, author = "alex"))
        alex.sync()
        tick()

        assertThat(camille.sync()).isEqualTo(SyncResult.Done(applied = 1, pushed = 0, unreadable = 1))
        assertThat(camille.store.find(SharedType.TASK, uid1)).isNotNull()
        assertThat(camille.store.find(SharedType.TASK, uid2)).isNull()
    }

    @Test
    fun `a block stored under the wrong blob id is ignored`() {
        alex.write(task(uid = uid1, revision = clock, author = "alex"))
        alex.sync()
        val genuine = server.blocks.values.single()
        server.blocks.clear()
        val moved = keys.blobId(SharedType.TASK, uid2)
        server.blocks[moved] = RemoteBlock(moved, keys.seal(DuoCodec.encode(task(uid1, clock, "alex")), "d1", moved), clock)
        tick()

        assertThat(genuine.blobId).isNotEqualTo(moved)
        assertThat(camille.sync()).isEqualTo(SyncResult.Done(applied = 0, pushed = 0, unreadable = 1))
    }

    @Test
    fun `an out of bounds block rejects the whole batch and writes nothing`() {
        alex.write(task(uid = uid1, revision = clock, author = "alex"))
        alex.sync()
        alex.write(task(uid = uid2, revision = clock + 3 * DuoLimits.MAX_CLOCK_AHEAD_MILLIS, author = "alex"))
        alex.sync()
        tick()

        assertThat(camille.sync()).isEqualTo(SyncResult.Rejected(DuoRejection.REVISION_OUT_OF_RANGE))
        assertThat(camille.store.records()).isEmpty()
        assertThat(camille.store.state().lastSeen).isEqualTo(0)
    }

    @Test
    fun `a block signed by someone outside the duo is rejected`() {
        alex.write(task(revision = clock, author = "intrus"))
        alex.sync()
        tick()

        assertThat(camille.sync()).isEqualTo(SyncResult.Rejected(DuoRejection.UNKNOWN_MEMBER))
    }

    @Test
    fun `when the other phone left, the duo ends`() {
        server.ended = true

        assertThat(alex.sync()).isEqualTo(SyncResult.Ended)
        assertThat(alex.store.ended).isTrue()
    }

    // --- faux serveur et fausse base ---

    private inner class FakeMailbox : DuoMailbox {
        val blocks = linkedMapOf<String, RemoteBlock>()
        var ended = false
        var pushes = 0

        override fun pull(afterMillis: Long): List<RemoteBlock> {
            if (ended) throw DuoEndedException()
            return blocks.values.filter { it.serverMillis > afterMillis }
        }

        override fun push(blocks: List<OutgoingBlock>) {
            pushes++
            blocks.forEach { this.blocks[it.blobId] = RemoteBlock(it.blobId, it.sealed, clock) }
        }

        fun insertLate(
            sealed: OutgoingBlock,
            serverMillis: Long,
        ) {
            blocks[sealed.blobId] = RemoteBlock(sealed.blobId, sealed.sealed, serverMillis)
        }
    }

    /** Scelle [record] comme le ferait alex, sans l'envoyer. */
    private fun Phone.write2(record: SharedRecord): OutgoingBlock {
        val blobId = keys.blobId(record.type, record.meta.uid)
        return OutgoingBlock(blobId, keys.seal(DuoCodec.encode(record), "d1", blobId))
    }

    private inner class FakeStore(
        me: String,
        members: Set<String>,
    ) : DuoStore {
        private var current = DuoState("d1", me, members, lastSeen = 0, pushedUntil = 0)
        private val rows = linkedMapOf<Pair<SharedType, String>, SharedRecord>()
        private val changedAt = mutableMapOf<Pair<SharedType, String>, Long>()
        var ended = false

        fun writeLocal(
            record: SharedRecord,
            at: Long,
        ) {
            rows[record.type to record.meta.uid] = record
            changedAt[record.type to record.meta.uid] = at
        }

        fun records() = rows.values.toList()

        override fun state() = current

        override fun find(
            type: SharedType,
            uid: String,
        ) = rows[type to uid]

        override fun apply(
            records: List<SharedRecord>,
            lastSeen: Long,
        ) {
            records.forEach { rows[it.type to it.meta.uid] = it }
            current = current.copy(lastSeen = lastSeen)
        }

        override fun pending(since: Long) =
            rows.entries
                .filter { (changedAt[it.key] ?: -1) >= since && changedAt.containsKey(it.key) }
                .map { LocalRecord(it.value, changedAt.getValue(it.key)) }

        override fun markPushed(until: Long) {
            current = current.copy(pushedUntil = until)
        }

        override fun markEnded() {
            ended = true
        }
    }
}
