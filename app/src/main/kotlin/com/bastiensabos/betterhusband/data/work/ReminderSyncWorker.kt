package com.bastiensabos.betterhusband.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Passe quotidienne : étend l'horizon des rappels et couvre une app non rouverte depuis longtemps. */
@HiltWorker
class ReminderSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val reminderSync: ReminderSync,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            reminderSync.syncOnce()
            return Result.success()
        }
    }
