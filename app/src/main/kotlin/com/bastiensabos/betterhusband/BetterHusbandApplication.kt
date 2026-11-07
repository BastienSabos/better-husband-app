package com.bastiensabos.betterhusband

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bastiensabos.betterhusband.data.work.ReminderSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BetterHusbandApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var reminderSync: ReminderSync

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Replanifie les rappels à chaque lancement : couvre redémarrage, mise à jour et changement d'heure.
        reminderSync.start()
    }
}
