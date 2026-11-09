package com.bastiensabos.betterhusband.domain.duo

import java.util.UUID

/**
 * Faux serveur d'invitations, en mémoire : sert aux tests et aux écrans tant que le projet Firebase n'existe pas. Il
 * ne relie que ce qui tourne dans le même processus ; il disparaîtra quand le client Firestore arrivera.
 */
class InMemoryDuoInviteServer : DuoInviteServer {
    private val invites = mutableMapOf<String, InviteRecord>()
    private val joins = mutableMapOf<String, JoinRequest>()
    private val accepted = mutableSetOf<String>()

    @Synchronized
    override fun createDuo(): String = UUID.randomUUID().toString()

    @Synchronized
    override fun publish(invite: InviteRecord) {
        invites[invite.inviteId] = invite
    }

    @Synchronized
    override fun fetch(inviteId: String): InviteRecord? = invites[inviteId]?.takeIf { inviteId !in accepted }

    @Synchronized
    override fun sendJoin(
        inviteId: String,
        request: JoinRequest,
    ) {
        require(inviteId in invites) { "invitation inconnue" }
        joins[inviteId] = request
    }

    @Synchronized
    override fun pendingJoin(inviteId: String): JoinRequest? = joins[inviteId]

    @Synchronized
    override fun dropJoin(inviteId: String) {
        joins.remove(inviteId)
    }

    @Synchronized
    override fun accept(inviteId: String) {
        accepted += inviteId
        joins.remove(inviteId)
    }

    @Synchronized
    override fun isAccepted(inviteId: String): Boolean = inviteId in accepted
}
