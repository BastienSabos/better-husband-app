package com.bastiensabos.betterhusband.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bastiensabos.betterhusband.R
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/** Prouve que c'est bien la personne qui tient le téléphone : biométrie, ou code de l'appareil en repli. */
interface BiometricAuthenticator {
    /** `false` si l'appareil n'a ni biométrie enregistrée ni code d'écran : le verrouillage ne peut pas s'activer. */
    fun canAuthenticate(): Boolean

    /** Affiche la demande système. `true` seulement si l'utilisateur est authentifié. À appeler sur le fil principal. */
    suspend fun authenticate(activity: FragmentActivity): Boolean
}

class AndroidBiometricAuthenticator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BiometricAuthenticator {
        override fun canAuthenticate(): Boolean =
            BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

        override suspend fun authenticate(activity: FragmentActivity): Boolean =
            suspendCancellableCoroutine { continuation ->
                val callback =
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            if (continuation.isActive) continuation.resume(true)
                        }

                        // Annulation, trop d'essais, pas de matériel : l'écran de verrouillage reste, avec « Réessayer ».
                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence,
                        ) {
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }
                val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
                val info =
                    BiometricPrompt.PromptInfo
                        .Builder()
                        .setTitle(activity.getString(R.string.lock_prompt_title))
                        .setAllowedAuthenticators(AUTHENTICATORS)
                        .build()
                prompt.authenticate(info)
                continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            }

        private companion object {
            // Biométrie « faible » (visage, empreinte) ou code d'écran : le plus large qui marche dès Android 8.
            const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class BiometricAuthenticatorModule {
    @Binds
    abstract fun bindBiometricAuthenticator(impl: AndroidBiometricAuthenticator): BiometricAuthenticator
}
