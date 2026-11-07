package com.bastiensabos.betterhusband.domain

import java.time.Duration
import java.time.Instant

/** Quand l'application se reverrouille après être passée en arrière-plan. */
object LockPolicy {
    /** Assez long pour répondre à un message ou changer d'appli sans retaper son code, assez court pour protéger. */
    val DEFAULT_GRACE: Duration = Duration.ofMinutes(1)

    /**
     * [leftAt] : l'instant où l'appli a quitté l'écran, `null` si elle ne l'a pas quitté (premier plan continu).
     * Une horloge revenue en arrière rend la durée négative : on reverrouille plutôt que de faire confiance.
     */
    fun shouldRelock(
        leftAt: Instant?,
        now: Instant,
        grace: Duration = DEFAULT_GRACE,
    ): Boolean {
        if (leftAt == null) return false
        val away = Duration.between(leftAt, now)
        return away.isNegative || away >= grace
    }
}
