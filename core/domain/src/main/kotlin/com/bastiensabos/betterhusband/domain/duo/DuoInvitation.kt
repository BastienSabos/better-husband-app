package com.bastiensabos.betterhusband.domain.duo

import com.google.crypto.tink.subtle.Random
import com.google.crypto.tink.subtle.X25519
import java.security.MessageDigest

/** La preuve envoyée par l'invité ne correspond pas au secret du lien : quelqu'un d'autre tente de rejoindre. */
class InvalidInvitationProofException : Exception("Preuve d'invitation invalide")

/**
 * Invitation au duo (ADR 0004, § 3). Le [secret] voyage seulement dans le lien ou le QR code, après le `#` : le
 * serveur ne le voit jamais. La clé de couple combine l'échange X25519 et ce secret.
 */
object DuoInvitation {
    const val SECRET_SIZE = 16

    /** Paire de clés X25519 d'un téléphone pour une invitation ; la clé privée n'est gardée que le temps de l'échange. */
    class KeyPair(
        val privateKey: ByteArray,
    ) {
        val publicKey: ByteArray = X25519.publicFromPrivate(privateKey)
    }

    fun newKeyPair(): KeyPair = KeyPair(X25519.generatePrivateKey())

    fun newSecret(): ByteArray = Random.randBytes(SECRET_SIZE)

    /** Ce qui identifie un échange : l'invitation et les deux clés publiques, dans l'ordre hôte puis invité. */
    class Exchange(
        val inviteId: String,
        val hostPublic: ByteArray,
        val guestPublic: ByteArray,
    ) {
        internal fun transcript() = "duo-invite-v1|$inviteId|".toByteArray() + hostPublic + guestPublic
    }

    /** Preuve que l'invité détient le secret du lien, liée à l'invitation et aux deux clés publiques. */
    fun proof(
        secret: ByteArray,
        exchange: Exchange,
    ): ByteArray = hmac(secret, exchange.transcript())

    /** Côté hôte : refuse un invité dont la preuve ne correspond pas (comparaison en temps constant). */
    fun verifyProof(
        proof: ByteArray,
        secret: ByteArray,
        exchange: Exchange,
    ) {
        if (!MessageDigest.isEqual(proof, proof(secret, exchange))) throw InvalidInvitationProofException()
    }

    /**
     * Clé de couple, la même des deux côtés : `HKDF(X25519(moi, l'autre) ‖ secret)`, salée par l'échange (dans
     * l'ordre hôte, invité, quel que soit le téléphone qui calcule).
     */
    fun coupleKey(
        mine: KeyPair,
        theirPublic: ByteArray,
        secret: ByteArray,
        exchange: Exchange,
    ): CoupleKeys {
        val shared = X25519.computeSharedSecret(mine.privateKey, theirPublic)
        return CoupleKeys(hkdf(shared + secret, "duo-v1 root", salt = exchange.transcript()))
    }
}
