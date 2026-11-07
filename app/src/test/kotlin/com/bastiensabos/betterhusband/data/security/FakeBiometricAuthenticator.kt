package com.bastiensabos.betterhusband.data.security

import androidx.fragment.app.FragmentActivity

class FakeBiometricAuthenticator(
    var available: Boolean = true,
    var succeeds: Boolean = true,
) : BiometricAuthenticator {
    override fun canAuthenticate(): Boolean = available

    override suspend fun authenticate(activity: FragmentActivity): Boolean = succeeds
}
