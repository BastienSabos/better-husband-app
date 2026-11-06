package com.bastiensabos.betterhusband.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Fournit la passphrase SQLCipher. Elle ne doit jamais être loggée ni affichée. */
interface DatabaseKeyProvider {
    fun passphrase(): ByteArray
}

/**
 * Passphrase de 32 octets aléatoires générée au premier lancement, chiffrée en AES-GCM par une clé non
 * exportable de l'Android Keystore, puis stockée dans `noBackupFilesDir` (jamais sauvegardée ni transférée).
 */
@Singleton
class KeystoreDatabaseKeyProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DatabaseKeyProvider {
        @Synchronized
        override fun passphrase(): ByteArray {
            val file = File(context.noBackupFilesDir, PASSPHRASE_FILE)
            val key = keystoreKey()
            return if (file.exists()) {
                decrypt(key, file.readBytes())
            } else {
                ByteArray(PASSPHRASE_BYTES).also {
                    SecureRandom().nextBytes(it)
                    file.writeBytes(encrypt(key, it))
                }
            }
        }

        private fun keystoreKey(): SecretKey {
            val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
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

        private fun encrypt(
            key: SecretKey,
            plain: ByteArray,
        ): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
            // Format : IV (12 octets) puis texte chiffré avec tag GCM.
            return cipher.iv + cipher.doFinal(plain)
        }

        private fun decrypt(
            key: SecretKey,
            blob: ByteArray,
        ): ByteArray {
            val cipher =
                Cipher.getInstance(TRANSFORMATION).apply {
                    init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES))
                }
            return cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES)
        }

        private companion object {
            const val ANDROID_KEYSTORE = "AndroidKeyStore"
            const val KEY_ALIAS = "better_husband_db_key"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val PASSPHRASE_FILE = "db.key"
            const val PASSPHRASE_BYTES = 32
            const val KEY_SIZE_BITS = 256
            const val IV_BYTES = 12
            const val TAG_BITS = 128
        }
    }
