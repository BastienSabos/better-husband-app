package com.bastiensabos.betterhusband.data.work

import com.bastiensabos.betterhusband.di.ApplicationScope
import com.bastiensabos.betterhusband.domain.ObserveUpcomingDatesUseCase
import com.bastiensabos.betterhusband.domain.ReminderPlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Garde les rappels planifiés alignés avec les dates : à chaque changement, au lancement et chaque jour. */
@Singleton
class ReminderSync
    @Inject
    constructor(
        private val observeUpcomingDates: ObserveUpcomingDatesUseCase,
        private val planner: ReminderPlanner,
        private val scheduler: ReminderScheduler,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        fun start() {
            scheduler.ensureDailySync()
            scope.launch {
                observeUpcomingDates()
                    .map(planner::plan)
                    .distinctUntilChanged()
                    .collect(scheduler::reschedule)
            }
        }

        suspend fun syncOnce() {
            scheduler.reschedule(planner.plan(observeUpcomingDates().first()))
        }
    }
