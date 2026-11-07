package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.data.repository.AttentionLogRepository
import com.bastiensabos.betterhusband.data.repository.CoupleProfileRepository
import com.bastiensabos.betterhusband.data.repository.HouseholdTaskRepository
import com.bastiensabos.betterhusband.data.repository.IdeaRepository
import com.bastiensabos.betterhusband.data.repository.ImportantDateRepository
import com.bastiensabos.betterhusband.data.repository.PartnerRepository
import com.bastiensabos.betterhusband.data.work.ReminderScheduler
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Droit d'accès et de portabilité : tout ce que l'app a enregistré, en un JSON lisible. */
class ExportUserDataUseCase
    @Inject
    constructor(
        private val partners: PartnerRepository,
        private val dates: ImportantDateRepository,
        private val ideas: IdeaRepository,
        private val tasks: HouseholdTaskRepository,
        private val attentions: AttentionLogRepository,
        private val coupleProfile: CoupleProfileRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(): String =
            DataExport.toJson(
                UserDataSnapshot(
                    partner = partners.observePartner().first(),
                    dates = dates.observeAll().first(),
                    ideas = ideas.observeAll().first(),
                    tasks = tasks.observeAll().first(),
                    attentions = attentions.observeAll().first(),
                    exportedAt = Instant.now(clock),
                    coupleProfile = coupleProfile.observe().first(),
                ),
            )
    }

/**
 * Droit à l'effacement : vide toute la base et annule les rappels planifiés. La fiche partenaire part en dernier :
 * sa disparition ramène l'app à l'onboarding, quand le reste est déjà effacé.
 */
class DeleteAllUserDataUseCase
    @Inject
    constructor(
        private val partners: PartnerRepository,
        private val dates: ImportantDateRepository,
        private val ideas: IdeaRepository,
        private val tasks: HouseholdTaskRepository,
        private val attentions: AttentionLogRepository,
        private val coupleProfile: CoupleProfileRepository,
        private val reminders: ReminderScheduler,
    ) {
        suspend operator fun invoke() {
            dates.deleteAll()
            ideas.deleteAll()
            tasks.deleteAll()
            attentions.deleteAll()
            coupleProfile.deleteAll()
            reminders.reschedule(emptyList())
            partners.deletePartner()
        }
    }
