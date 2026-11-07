package com.bastiensabos.betterhusband.data.work

import com.bastiensabos.betterhusband.domain.PlannedReminder

class FakeReminderScheduler : ReminderScheduler {
    val calls = mutableListOf<List<PlannedReminder>>()

    override fun reschedule(reminders: List<PlannedReminder>) {
        calls += reminders
    }

    override fun ensureDailySync() = Unit
}
