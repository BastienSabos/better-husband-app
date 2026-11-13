package com.bastiensabos.betterhusband.data.security

/** Coffre de clé de couple en mémoire (le vrai passe par le Keystore, qui ne se teste que sur appareil). */
class FakeCoupleKeyVault : CoupleKeyVault {
    var stored: ByteArray? = null
    var failOnSave = false

    override fun save(root: ByteArray) {
        check(!failOnSave) { "Keystore indisponible" }
        stored = root.copyOf()
    }

    override fun load(): ByteArray? = stored?.copyOf()

    override fun clear() {
        stored = null
    }
}
