package com.bastiensabos.betterhusband.domain.duo

import com.google.crypto.tink.subtle.Random

/** Ce que le serveur garde d'une invitation : jamais le secret du lien. */
class InviteRecord(
    val inviteId: String,
    val duoId: String,
    val hostPublic: ByteArray,
    val expiresAt: Long,
)

/** La demande de l'invité : sa clé publique et la preuve qu'il a reçu le lien. */
class JoinRequest(
    val guestPublic: ByteArray,
    val proof: ByteArray,
)

/** Le serveur d'invitations (Firestore plus tard ; un faux en mémoire d'ici là). Les appels bloquent. */
interface DuoInviteServer {
    /** Crée le duo avec son seul créateur et rend son identifiant. */
    fun createDuo(): String

    fun publish(invite: InviteRecord)

    /** `null` si l'invitation est inconnue ou expirée. */
    fun fetch(inviteId: String): InviteRecord?

    fun sendJoin(
        inviteId: String,
        request: JoinRequest,
    )

    /** Côté hôte : la demande en attente, s'il y en a une. */
    fun pendingJoin(inviteId: String): JoinRequest?

    /** Côté hôte : efface une demande refusée. */
    fun dropJoin(inviteId: String)

    /** Côté hôte : ajoute l'invité aux membres du duo ; l'invitation ne sert plus. */
    fun accept(inviteId: String)

    /** Côté invité : l'hôte l'a-t-il ajouté ? */
    fun isAccepted(inviteId: String): Boolean
}

/** La clé de couple et le duo, une fois l'échange terminé des deux côtés. */
class DuoJoined(
    val duoId: String,
    val keys: CoupleKeys,
    /** Ce téléphone a créé l'invitation. */
    val asHost: Boolean,
) {
    val myMemberId: String get() = keys.memberId(host = asHost)
    val partnerMemberId: String get() = keys.memberId(host = !asHost)
}

sealed interface HostStep {
    data object Waiting : HostStep

    /** Quelqu'un a répondu sans le secret du lien : sa demande est effacée, l'invitation reste ouverte. */
    data object Refused : HostStep

    data class Joined(
        val joined: DuoJoined,
    ) : HostStep
}

sealed interface GuestStep {
    data object Waiting : GuestStep

    data class Joined(
        val joined: DuoJoined,
    ) : GuestStep
}

/** L'invitation a expiré ou n'existe pas. */
class InviteUnavailableException : Exception("Invitation expirée ou inconnue")

/** Côté de celui qui invite : crée l'invitation, puis vérifie celui qui répond. */
class DuoInviteHost private constructor(
    private val server: DuoInviteServer,
    private val keyPair: DuoInvitation.KeyPair,
    private val secret: ByteArray,
    private val inviteId: String,
    private val duoId: String,
    val link: DuoInviteLink,
) {
    fun poll(): HostStep {
        val join = server.pendingJoin(inviteId) ?: return HostStep.Waiting
        val exchange = DuoInvitation.Exchange(inviteId, keyPair.publicKey, join.guestPublic)
        return try {
            DuoInvitation.verifyProof(join.proof, secret, exchange)
            server.accept(inviteId)
            HostStep.Joined(DuoJoined(duoId, DuoInvitation.coupleKey(keyPair, join.guestPublic, secret, exchange), asHost = true))
        } catch (_: InvalidInvitationProofException) {
            server.dropJoin(inviteId)
            HostStep.Refused
        }
    }

    companion object {
        const val VALIDITY_MILLIS = 24L * 60 * 60 * 1000

        fun start(
            server: DuoInviteServer,
            nowMillis: Long,
        ): DuoInviteHost {
            val keyPair = DuoInvitation.newKeyPair()
            val secret = DuoInvitation.newSecret()
            val inviteId = Random.randBytes(ID_SIZE).toHex()
            val duoId = server.createDuo()
            server.publish(InviteRecord(inviteId, duoId, keyPair.publicKey, nowMillis + VALIDITY_MILLIS))
            return DuoInviteHost(server, keyPair, secret, inviteId, duoId, DuoInviteLink(inviteId, secret))
        }

        private const val ID_SIZE = 16
    }
}

/** Côté de celui qui rejoint : répond à l'invitation, puis attend d'être ajouté. */
class DuoInviteGuest private constructor(
    private val server: DuoInviteServer,
    private val invite: InviteRecord,
    private val keys: CoupleKeys,
) {
    fun poll(): GuestStep =
        if (server.isAccepted(invite.inviteId)) GuestStep.Joined(DuoJoined(invite.duoId, keys, asHost = false)) else GuestStep.Waiting

    companion object {
        /** @throws InviteUnavailableException si l'invitation est inconnue ou expirée. */
        fun join(
            server: DuoInviteServer,
            link: DuoInviteLink,
            nowMillis: Long,
        ): DuoInviteGuest {
            val invite = server.fetch(link.inviteId)?.takeIf { it.expiresAt > nowMillis } ?: throw InviteUnavailableException()
            val keyPair = DuoInvitation.newKeyPair()
            val exchange = DuoInvitation.Exchange(invite.inviteId, invite.hostPublic, keyPair.publicKey)
            server.sendJoin(invite.inviteId, JoinRequest(keyPair.publicKey, DuoInvitation.proof(link.secret, exchange)))
            val keys = DuoInvitation.coupleKey(keyPair, invite.hostPublic, link.secret, exchange)
            return DuoInviteGuest(server, invite, keys)
        }
    }
}
