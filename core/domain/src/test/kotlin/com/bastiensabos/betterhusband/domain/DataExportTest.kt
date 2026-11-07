package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.AttentionLogEntry
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.Idea
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.domain.model.Relative
import com.bastiensabos.betterhusband.domain.model.SizeKind
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.bastiensabos.betterhusband.domain.model.Taste
import com.bastiensabos.betterhusband.domain.model.TasteCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DataExportTest {
    private val exportedAt = Instant.parse("2027-01-20T10:00:00Z")

    private fun snapshot(
        partner: Partner? = null,
        ideas: List<Idea> = emptyList(),
    ) = UserDataSnapshot(
        partner = partner,
        dates = emptyList(),
        ideas = ideas,
        tasks = emptyList(),
        attentions = emptyList(),
        exportedAt = exportedAt,
    )

    @Test
    fun `empty snapshot gives empty lists and a null partner`() {
        val json = DataExport.toJson(snapshot())

        assertThat(json).contains("\"formatVersion\": 1")
        assertThat(json).contains("\"exportedAt\": \"2027-01-20T10:00:00Z\"")
        assertThat(json).contains("\"partner\": null")
        assertThat(json).contains("\"ideas\": []")
        assertThat(json).contains("\"attentions\": []")
    }

    @Test
    fun `partner profile is exported with tastes sizes and relatives`() {
        val partner =
            Partner(
                firstName = "Camille",
                addressForm = AddressForm.NEUTRAL,
                birthday = LocalDate.of(1990, 5, 17),
                tastes = listOf(Taste(TasteCategory.FLOWERS, "pivoines")),
                sizes = mapOf(SizeKind.SHOES to "40"),
                relatives = listOf(Relative("Alex", "ami")),
            )

        val json = DataExport.toJson(snapshot(partner = partner))

        assertThat(json).contains("\"firstName\": \"Camille\"")
        assertThat(json).contains("\"addressForm\": \"NEUTRAL\"")
        assertThat(json).contains("\"birthday\": \"1990-05-17\"")
        assertThat(json).contains("\"relationshipStart\": null")
        assertThat(json).contains("\"category\": \"FLOWERS\"")
        assertThat(json).contains("\"SHOES\": \"40\"")
        assertThat(json).contains("\"relation\": \"ami\"")
    }

    @Test
    fun `quotes backslashes and line breaks are escaped`() {
        val idea = Idea(text = "Un \"mot\" \\ doux\nligne\ttab\u0001", category = IdeaCategory.GIFT, createdAt = exportedAt)

        val json = DataExport.toJson(snapshot(ideas = listOf(idea)))

        assertThat(json).contains("\"text\": \"Un \\\"mot\\\" \\\\ doux\\nligne\\ttab\\u0001\"")
    }

    @Test
    fun `accents and emoji stay readable`() {
        val idea = Idea(text = "Café à Noël 🎁", category = IdeaCategory.OUTING, createdAt = exportedAt, done = true)

        val json = DataExport.toJson(snapshot(ideas = listOf(idea)))

        assertThat(json).contains("\"text\": \"Café à Noël 🎁\"")
        assertThat(json).contains("\"done\": true")
    }

    @Test
    fun `dates tasks and attentions are exported`() {
        val data =
            UserDataSnapshot(
                partner = null,
                dates = listOf(ImportantDate(title = "Voyage", kind = DateKind.OTHER, date = LocalDate.of(2027, 8, 1), recurring = false)),
                ideas = emptyList(),
                tasks = listOf(HouseholdTask(title = "Courses", repeat = TaskRepeat.WEEKLY, createdOn = LocalDate.of(2027, 1, 4))),
                attentions = listOf(AttentionLogEntry(LocalDate.of(2027, 1, 19), attentionId = 12, done = true)),
                exportedAt = exportedAt,
            )

        val json = DataExport.toJson(data)

        assertThat(json).contains("\"title\": \"Voyage\"")
        assertThat(json).contains("\"recurring\": false")
        assertThat(json).contains("\"repeat\": \"WEEKLY\"")
        assertThat(json).contains("\"lastDoneOn\": null")
        assertThat(json).contains("\"attentionId\": 12")
    }

    @Test
    fun `export is indented and ends with a newline`() {
        val json = DataExport.toJson(snapshot())

        assertThat(json).startsWith("{\n  \"formatVersion\"")
        assertThat(json).endsWith("}\n")
    }
}
