package com.bastiensabos.betterhusband.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bastiensabos.betterhusband.domain.ObserveUpcomingDatesUseCase
import com.bastiensabos.betterhusband.domain.ReminderPlanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Worker fin : retrouve la date dans la base chiffrée puis délègue l'affichage à [ReminderNotifier]. */
@HiltWorker
class DateReminderWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val observeUpcomingDates: ObserveUpcomingDatesUseCase,
        private val notifier: ReminderNotifier,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val dateKey = inputData.getString(KEY_DATE) ?: return Result.failure()
            val offset = inputData.getInt(KEY_OFFSET, -1)
            if (offset < 0) return Result.failure()
            // Date supprimée ou déplacée depuis la planification : on n'envoie rien.
            val due = ReminderPlanner.selectDue(observeUpcomingDates().first(), dateKey, offset)
            if (due != null) notifier.notify(due, offset)
            return Result.success()
        }

        companion object {
            const val KEY_DATE = "date_key"
            const val KEY_OFFSET = "offset_days"
        }
    }
