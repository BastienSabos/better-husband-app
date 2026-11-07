package com.bastiensabos.betterhusband.data.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bastiensabos.betterhusband.domain.PlannedReminder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface ReminderScheduler {
    /** Remplace tous les rappels planifiés par [reminders]. Idempotent. */
    fun reschedule(reminders: List<PlannedReminder>)

    /** S'assure qu'une synchro quotidienne tourne, même si l'app n'est pas rouverte. */
    fun ensureDailySync()
}

@Singleton
class WorkManagerReminderScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val clock: Clock,
    ) : ReminderScheduler {
        private val workManager: WorkManager get() = WorkManager.getInstance(context)

        override fun reschedule(reminders: List<PlannedReminder>) {
            workManager.cancelAllWorkByTag(TAG_REMINDER)
            val now = clock.instant()
            reminders.forEach { reminder ->
                // Seuls la clé de la date et le décalage sont stockés : WorkManager n'est pas chiffré.
                val request =
                    OneTimeWorkRequestBuilder<DateReminderWorker>()
                        .setInitialDelay(Duration.between(now, reminder.triggerAt.toInstant()).coerceAtLeast(Duration.ZERO))
                        .setInputData(
                            workDataOf(
                                DateReminderWorker.KEY_DATE to reminder.dateKey,
                                DateReminderWorker.KEY_OFFSET to reminder.offsetDays,
                            ),
                        ).addTag(TAG_REMINDER)
                        .build()
                workManager.enqueueUniqueWork(reminder.workName, ExistingWorkPolicy.REPLACE, request)
            }
        }

        override fun ensureDailySync() {
            val request = PeriodicWorkRequestBuilder<ReminderSyncWorker>(1, TimeUnit.DAYS).build()
            workManager.enqueueUniquePeriodicWork(DAILY_SYNC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        private companion object {
            const val TAG_REMINDER = "date-reminder"
            const val DAILY_SYNC_NAME = "reminder-daily-sync"
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
    @Binds
    abstract fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
}
