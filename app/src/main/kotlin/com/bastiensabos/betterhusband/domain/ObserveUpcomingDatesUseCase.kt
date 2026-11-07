package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.data.repository.ImportantDateRepository
import com.bastiensabos.betterhusband.data.repository.PartnerRepository
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.DateLabel
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Réunit les dates saisies à la main et celles déjà connues de la fiche partenaire (anniversaire, début de la
 * relation, anniversaires des proches), triées par prochaine échéance. « Aujourd'hui » est lu à chaque
 * émission : l'UI doit se réabonner ou rafraîchir au retour au premier plan pour passer minuit.
 */
class ObserveUpcomingDatesUseCase
    @Inject
    constructor(
        private val partnerRepository: PartnerRepository,
        private val importantDateRepository: ImportantDateRepository,
        private val clock: Clock,
    ) {
        operator fun invoke(): Flow<List<UpcomingDate>> =
            combine(partnerRepository.observePartner(), importantDateRepository.observeAll()) { partner, dates ->
                val today = LocalDate.now(clock)
                (partner.derivedDates(today) + dates.map { it.toUpcoming(today) })
                    .filterNotNull()
                    .sortedWith(compareBy({ it.date }, { it.key }))
            }

        private fun Partner?.derivedDates(today: LocalDate): List<UpcomingDate?> {
            if (this == null) return emptyList()
            return listOf(
                birthday?.let {
                    upcoming(
                        "partner-birthday",
                        DateLabel.PartnerBirthday(firstName),
                        DateKind.BIRTHDAY,
                        it,
                        true,
                        today,
                        null,
                    )
                },
                relationshipStart?.let {
                    upcoming("relationship-start", DateLabel.RelationshipStart, DateKind.ANNIVERSARY, it, true, today, null)
                },
            ) +
                relatives.mapIndexed { index, relative ->
                    relative.birthday?.let {
                        upcoming("relative-$index", DateLabel.RelativeBirthday(relative.name), DateKind.BIRTHDAY, it, true, today, null)
                    }
                }
        }

        private fun ImportantDate.toUpcoming(today: LocalDate) =
            upcoming("date-$id", DateLabel.Custom(title), kind, date, recurring, today, id)

        private fun upcoming(
            key: String,
            label: DateLabel,
            kind: DateKind,
            original: LocalDate,
            recurring: Boolean,
            today: LocalDate,
            editableId: Long?,
        ): UpcomingDate? {
            val next = DateMath.nextOccurrence(original, recurring, today) ?: return null
            return UpcomingDate(
                key = key,
                label = label,
                kind = kind,
                date = next,
                daysUntil = ChronoUnit.DAYS.between(today, next),
                yearsSince = if (recurring) next.year - original.year else null,
                editableId = editableId,
            )
        }
    }
