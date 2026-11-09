package com.bastiensabos.betterhusband.domain.duo

import java.util.Base64

/**
 * Le lien d'invitation (ADR 0004, § 3) : `https://<domaine>/i/<invitation>#<secret>`. Le secret est après le `#`, donc
 * jamais envoyé au serveur par un navigateur. Le QR code porte le même lien.
 */
class DuoInviteLink(
    val inviteId: String,
    val secret: ByteArray,
) {
    fun toUrl(host: String): String = "https://$host/i/$inviteId#${ENCODER.encodeToString(secret)}"

    companion object {
        private val ENCODER = Base64.getUrlEncoder().withoutPadding()
        private val ID = Regex("^[0-9a-f]{32}$")
        private val URL = Regex("^https://[^/?#\\s]+/i/([^/?#\\s]+)#([A-Za-z0-9_-]+)$")

        /** Lit un lien collé ou scanné ; `null` s'il n'a pas la forme attendue (rien n'est deviné). */
        fun parse(url: String): DuoInviteLink? {
            val (id, encoded) = URL.matchEntire(url.trim())?.destructured ?: return null
            val secret = runCatching { Base64.getUrlDecoder().decode(encoded) }.getOrNull()
            return if (ID.matches(id) && secret?.size == DuoInvitation.SECRET_SIZE) DuoInviteLink(id, secret) else null
        }
    }
}
