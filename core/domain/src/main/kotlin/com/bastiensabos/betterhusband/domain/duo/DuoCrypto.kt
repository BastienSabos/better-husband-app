package com.bastiensabos.betterhusband.domain.duo

import com.bastiensabos.betterhusband.domain.model.SharedType
import com.google.crypto.tink.subtle.AesGcmJce
import com.google.crypto.tink.subtle.Hkdf
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Bloc illisible, altéré ou d'une version inconnue : il est ignoré, jamais appliqué à moitié. */
class UnreadableBlockException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Clés dérivées de la clé de couple `K` (ADR 0004, § 2). [root] fait 32 octets et ne quitte jamais le téléphone ;
 * elle est rangée chiffrée par le Keystore côté application.
 */
class CoupleKeys(
    private val root: ByteArray,
) {
    init {
        require(root.size == KEY_SIZE) { "La clé de couple fait $KEY_SIZE octets" }
    }

    /** La clé racine, pour la ranger chiffrée par le Keystore et rien d'autre : ne jamais l'afficher ni la journaliser. */
    fun exportRoot(): ByteArray = root.copyOf()

    private val dataKey = hkdf(root, "duo-v1 data")
    private val idKey = hkdf(root, "duo-v1 id")
    private val verifyKey = hkdf(root, "duo-v1 verify")
    private val memberKey = hkdf(root, "duo-v1 member")

    /**
     * Identifiant de bloc vu par le serveur : HMAC de `type ‖ uid`, tronqué à 16 octets, en hexadécimal. Stable (un
     * enregistrement garde le même bloc), mais sans lien visible avec l'identifiant réel.
     */
    fun blobId(
        type: SharedType,
        uid: String,
    ): String = hmac(idKey, "${type.name}:$uid".toByteArray()).copyOf(BLOB_ID_SIZE).toHex()

    /**
     * Chiffre [plaintext] (le JSON d'un enregistrement) pour le bloc [blobId] du duo [duoId]. Le texte est complété à
     * un multiple de [PAD_BLOCK] octets : le serveur ne devine pas la longueur d'une envie à la taille du bloc.
     */
    fun seal(
        plaintext: ByteArray,
        duoId: String,
        blobId: String,
    ): ByteArray = byteArrayOf(FORMAT_VERSION) + AesGcmJce(dataKey).encrypt(pad(plaintext), aad(duoId, blobId))

    /** Déchiffre un bloc produit par [seal] ; tout écart (clé, duo, bloc, octet modifié) lève [UnreadableBlockException]. */
    fun open(
        sealed: ByteArray,
        duoId: String,
        blobId: String,
    ): ByteArray {
        if (sealed.isEmpty() || sealed[0] != FORMAT_VERSION) throw UnreadableBlockException("Version de bloc inconnue")
        val padded =
            try {
                AesGcmJce(dataKey).decrypt(sealed.copyOfRange(1, sealed.size), aad(duoId, blobId))
            } catch (e: GeneralSecurityException) {
                throw UnreadableBlockException("Bloc illisible", e)
            }
        return unpad(padded)
    }

    /**
     * Identifiant de membre de l'hôte ([host] vrai) ou de l'invité, tiré de la clé : les deux téléphones connaissent
     * les deux identifiants dès l'invitation, sans rien échanger de plus. Forme d'UUID, sans lien avec un compte.
     */
    fun memberId(host: Boolean): String {
        val bytes = ByteBuffer.wrap(hmac(memberKey, (if (host) "host" else "guest").toByteArray()))
        return UUID(bytes.long, bytes.long).toString()
    }

    /**
     * Code de vérification affiché sur les deux téléphones : [VERIFY_WORDS] indices de 0 à 255, que l'interface
     * traduit en mots. Deux codes identiques prouvent que les deux téléphones ont la même clé.
     */
    fun verificationCode(): List<Int> = verifyKey.take(VERIFY_WORDS).map { it.toInt() and BYTE_MASK }

    companion object {
        const val KEY_SIZE = 32
        const val BLOB_ID_SIZE = 16
        const val PAD_BLOCK = 256
        const val VERIFY_WORDS = 4
        private const val FORMAT_VERSION: Byte = 1
        private const val LENGTH_PREFIX = Int.SIZE_BYTES
        private const val BYTE_MASK = 0xFF

        private fun aad(
            duoId: String,
            blobId: String,
        ) = "duo-v1|$duoId|$blobId|$FORMAT_VERSION".toByteArray()

        internal fun pad(plaintext: ByteArray): ByteArray {
            val used = LENGTH_PREFIX + plaintext.size
            val size = (used + PAD_BLOCK - 1) / PAD_BLOCK * PAD_BLOCK
            return ByteBuffer
                .allocate(size)
                .putInt(plaintext.size)
                .put(plaintext)
                .array()
        }

        private fun unpad(padded: ByteArray): ByteArray {
            if (padded.size < LENGTH_PREFIX) throw UnreadableBlockException("Bloc trop court")
            val length = ByteBuffer.wrap(padded).int
            if (length !in 0..padded.size - LENGTH_PREFIX) throw UnreadableBlockException("Longueur incohérente")
            return padded.copyOfRange(LENGTH_PREFIX, LENGTH_PREFIX + length)
        }
    }
}

internal fun hkdf(
    ikm: ByteArray,
    info: String,
    salt: ByteArray = ByteArray(0),
): ByteArray = Hkdf.computeHkdf("HMACSHA256", ikm, salt, info.toByteArray(), CoupleKeys.KEY_SIZE)

internal fun hmac(
    key: ByteArray,
    data: ByteArray,
): ByteArray =
    Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(data)
    }

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
