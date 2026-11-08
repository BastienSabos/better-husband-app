package com.bastiensabos.betterhusband.domain.duo

import com.bastiensabos.betterhusband.domain.model.SharedType
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class DuoInvitationTest {
    private val inviteId = "inv-1"
    private val host = DuoInvitation.newKeyPair()
    private val guest = DuoInvitation.newKeyPair()
    private val secret = DuoInvitation.newSecret()

    private val exchange = DuoInvitation.Exchange(inviteId, host.publicKey, guest.publicKey)

    private fun hostKey(
        guestPublic: ByteArray = guest.publicKey,
        withSecret: ByteArray = secret,
    ) = DuoInvitation.coupleKey(host, guestPublic, withSecret, DuoInvitation.Exchange(inviteId, host.publicKey, guestPublic))

    private fun guestKey() = DuoInvitation.coupleKey(guest, host.publicKey, secret, exchange)

    @Test
    fun `both phones derive the same couple key`() {
        val atHost = hostKey()
        val atGuest = guestKey()

        assertThat(atHost.verificationCode()).isEqualTo(atGuest.verificationCode())
        val blob = atHost.blobId(SharedType.WISH, "w")
        assertThat(atGuest.open(atHost.seal("ok".toByteArray(), "d", blob), "d", blob)).isEqualTo("ok".toByteArray())
    }

    @Test
    fun `the host accepts the proof of a guest who has the link`() {
        val proof = DuoInvitation.proof(secret, exchange)

        DuoInvitation.verifyProof(proof, secret, exchange)
    }

    @Test
    fun `the host refuses someone who does not have the link secret`() {
        val proof = DuoInvitation.proof(DuoInvitation.newSecret(), exchange)

        assertThrows(InvalidInvitationProofException::class.java) {
            DuoInvitation.verifyProof(proof, secret, exchange)
        }
    }

    @Test
    fun `a proof cannot be replayed with another public key`() {
        val intruder = DuoInvitation.newKeyPair()
        val proof = DuoInvitation.proof(secret, exchange)

        assertThrows(InvalidInvitationProofException::class.java) {
            DuoInvitation.verifyProof(proof, secret, DuoInvitation.Exchange(inviteId, host.publicKey, intruder.publicKey))
        }
    }

    @Test
    fun `a server that swaps the guest key ends up with a different verification code`() {
        // Le serveur seul, sans le secret du lien, ne peut pas calculer la même clé que l'invité.
        val attacker = DuoInvitation.newKeyPair()

        assertThat(hostKey(guestPublic = attacker.publicKey).verificationCode())
            .isNotEqualTo(guestKey().verificationCode())
    }

    @Test
    fun `the link secret is part of the couple key`() {
        assertThat(hostKey(withSecret = DuoInvitation.newSecret()).verificationCode())
            .isNotEqualTo(guestKey().verificationCode())
    }

    @Test
    fun `secrets and key pairs are fresh each time`() {
        assertThat(secret).hasLength(DuoInvitation.SECRET_SIZE)
        assertThat(DuoInvitation.newSecret()).isNotEqualTo(secret)
        assertThat(DuoInvitation.newKeyPair().publicKey).isNotEqualTo(host.publicKey)
    }
}
