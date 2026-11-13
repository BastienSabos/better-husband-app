package com.bastiensabos.betterhusband.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Range la clé racine du couple (32 octets), hors de la base : elle ne doit jamais être loggée ni affichée. */
interface CoupleKeyVault {
    fun save(root: ByteArray)

    /** `null` si aucun duo n'a été rejoint, ou si la clé est illisible (Keystore réinitialisé). */
    fun load(): ByteArray?

    fun clear()
}

/**
 * Comme la passphrase de la base : chiffrée en AES-GCM par une clé non exportable de l'Android Keystore (alias
 * distinct), stockée dans `noBackupFilesDir`, donc jamais sauvegardée ni transférée. Ne se teste que sur appareil.
 */
@Singleton
class KeystoreCoupleKeyVault
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : CoupleKeyVault {
        private val file get() = File(context.noBackupFilesDir, KEY_FILE)

        @Synchronized
        override fun save(root: ByteArray) {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, keystoreKey()) }
            // Format : IV (12 octets) puis texte chiffré avec tag GCM.
            file.writeBytes(cipher.iv + cipher.doFinal(root))
        }

        @Synchronized
        override fun load(): ByteArray? {
            val blob = file.takeIf { it.exists() }?.readBytes() ?: return null
            // Jamais de nouvelle clé ici : si le Keystore a perdu l'ancienne, le fichier ne se lira plus et il est effacé.
            val root =
                runCatching {
                    val key = existingKey() ?: error("clé Keystore absente")
                    val cipher =
                        Cipher.getInstance(TRANSFORMATION).apply {
                            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES))
                        }
                    cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES)
                }.getOrNull()
            if (root == null) file.delete()
            return root
        }

        @Synchronized
        override fun clear() {
            file.delete()
            // Une erreur du Keystore ne doit pas interrompre l'effacement du reste des données.
            runCatching {
                val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
            }
        }

        private fun existingKey(): SecretKey? {
            val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            return store.getKey(KEY_ALIAS, null) as? SecretKey
        }

        private fun keystoreKey(): SecretKey {
            existingKey()?.let { return it }
            val spec =
                KeyGenParameterSpec
                    .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build()
            return KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                .apply { init(spec) }
                .generateKey()
        }

        private companion object {
            const val ANDROID_KEYSTORE = "AndroidKeyStore"
            const val KEY_ALIAS = "better_husband_couple_key"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val KEY_FILE = "couple.key"
            const val KEY_SIZE_BITS = 256
            const val IV_BYTES = 12
            const val TAG_BITS = 128
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class CoupleKeyVaultModule {
    @Binds
    abstract fun bindCoupleKeyVault(impl: KeystoreCoupleKeyVault): CoupleKeyVault
}
