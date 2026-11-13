package com.bastiensabos.betterhusband.domain.duo

import com.bastiensabos.betterhusband.domain.model.SharedType
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class DuoCryptoTest {
    private val keys = CoupleKeys(ByteArray(CoupleKeys.KEY_SIZE) { it.toByte() })
    private val duoId = "d1"
    private val uid = "0f8b2c1e-6a4d-4c3b-9e2f-1a2b3c4d5e6f"
    private val blobId = keys.blobId(SharedType.TASK, uid)
    private val json = """{"title":"Sortir les poubelles"}""".toByteArray()

    @Test
    fun `a sealed block opens back to the same text`() {
        val sealed = keys.seal(json, duoId, blobId)

        assertThat(keys.open(sealed, duoId, blobId)).isEqualTo(json)
    }

    @Test
    fun `the text does not appear in the sealed block`() {
        val sealed = keys.seal(json, duoId, blobId)

        assertThat(String(sealed, Charsets.ISO_8859_1)).doesNotContain("poubelles")
    }

    @Test
    fun `sealing twice gives two different blocks`() {
        assertThat(keys.seal(json, duoId, blobId)).isNotEqualTo(keys.seal(json, duoId, blobId))
    }

    @Test
    fun `block sizes hide text length within a padding step`() {
        val short = keys.seal("a".toByteArray(), duoId, blobId)
        val longer = keys.seal("a".repeat(200).toByteArray(), duoId, blobId)
        val beyond = keys.seal("a".repeat(300).toByteArray(), duoId, blobId)

        assertThat(short.size).isEqualTo(longer.size)
        assertThat(beyond.size).isEqualTo(short.size + CoupleKeys.PAD_BLOCK)
    }

    @Test
    fun `an empty text still round trips`() {
        assertThat(keys.open(keys.seal(ByteArray(0), duoId, blobId), duoId, blobId)).isEmpty()
    }

    @Test
    fun `a modified byte makes the block unreadable`() {
        val sealed = keys.seal(json, duoId, blobId)
        sealed[sealed.size / 2] = (sealed[sealed.size / 2] + 1).toByte()

        assertThrows(UnreadableBlockException::class.java) { keys.open(sealed, duoId, blobId) }
    }

    @Test
    fun `a block moved to another duo or record is unreadable`() {
        val sealed = keys.seal(json, duoId, blobId)
        val otherBlob = keys.blobId(SharedType.TASK, "11111111-2222-4333-8444-555555555555")

        assertThrows(UnreadableBlockException::class.java) { keys.open(sealed, "d2", blobId) }
        assertThrows(UnreadableBlockException::class.java) { keys.open(sealed, duoId, otherBlob) }
    }

    @Test
    fun `another couple key cannot read the block`() {
        val sealed = keys.seal(json, duoId, blobId)
        val stranger = CoupleKeys(ByteArray(CoupleKeys.KEY_SIZE) { 7 })

        assertThrows(UnreadableBlockException::class.java) { stranger.open(sealed, duoId, blobId) }
    }

    @Test
    fun `an unknown format version or empty block is refused`() {
        val sealed = keys.seal(json, duoId, blobId)
        sealed[0] = 2

        assertThrows(UnreadableBlockException::class.java) { keys.open(sealed, duoId, blobId) }
        assertThrows(UnreadableBlockException::class.java) { keys.open(ByteArray(0), duoId, blobId) }
    }

    @Test
    fun `a blob id is stable, hides the uid and depends on the type`() {
        assertThat(keys.blobId(SharedType.TASK, uid)).isEqualTo(blobId)
        assertThat(blobId).hasLength(CoupleKeys.BLOB_ID_SIZE * 2)
        assertThat(blobId).doesNotContain(uid.take(8))
        assertThat(keys.blobId(SharedType.DATE, uid)).isNotEqualTo(blobId)
    }

    @Test
    fun `the derivation stays the same from one version to the next`() {
        // Vecteur figé (valeurs de la première version, qui servent de garde contre une régression) : changer la dérivation rendrait illisibles les blocs déjà envoyés par l'autre téléphone.
        assertThat(blobId).isEqualTo(GOLDEN_BLOB_ID)
        assertThat(keys.verificationCode()).containsExactlyElementsIn(GOLDEN_CODE).inOrder()
    }

    @Test
    fun `the verification code has four words between 0 and 255`() {
        assertThat(keys.verificationCode()).hasSize(CoupleKeys.VERIFY_WORDS)
        assertThat(keys.verificationCode().all { it in 0..255 }).isTrue()
    }

    @Test
    fun `a couple key of the wrong size is refused`() {
        assertThrows(IllegalArgumentException::class.java) { CoupleKeys(ByteArray(16)) }
    }

    private companion object {
        const val GOLDEN_BLOB_ID = "02a914efe071916c6ef6875f4640d3c1"
        val GOLDEN_CODE = listOf(45, 44, 117, 119)
    }

    @Test
    fun `a key rebuilt from its exported root opens what the original sealed`() {
        val original = CoupleKeys(ByteArray(32) { it.toByte() })
        val rebuilt = CoupleKeys(original.exportRoot())
        val blob = original.blobId(SharedType.WISH, "w")

        assertThat(rebuilt.open(original.seal("ok".toByteArray(), "d", blob), "d", blob)).isEqualTo("ok".toByteArray())
        assertThat(rebuilt.verificationCode()).isEqualTo(original.verificationCode())
    }

    @Test
    fun `member ids are stable, distinct per role and shaped like a uuid`() {
        val keys = CoupleKeys(ByteArray(32) { it.toByte() })
        val uuid = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

        assertThat(keys.memberId(host = true)).isEqualTo(CoupleKeys(ByteArray(32) { it.toByte() }).memberId(host = true))
        assertThat(keys.memberId(host = true)).isNotEqualTo(keys.memberId(host = false))
        assertThat(keys.memberId(host = true)).matches(uuid.pattern)
        assertThat(keys.memberId(host = false)).isNotEqualTo(CoupleKeys(ByteArray(32) { 1 }).memberId(host = false))
    }
}
