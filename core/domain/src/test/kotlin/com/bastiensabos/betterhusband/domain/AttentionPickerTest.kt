package com.bastiensabos.betterhusband.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class AttentionPickerTest {
    private val day = LocalDate.of(2027, 1, 20)

    @Test
    fun `same day and history give the same attention`() {
        assertThat(AttentionPicker.pick(day, 150, listOf(3, 4))).isEqualTo(AttentionPicker.pick(day, 150, listOf(3, 4)))
    }

    @Test
    fun `picked id is inside the bank`() {
        repeat(400) { offset ->
            val id = AttentionPicker.pick(day.plusDays(offset.toLong()), 150, emptyList())
            assertThat(id).isIn(0 until 150)
        }
    }

    @Test
    fun `recent attentions are not picked again`() {
        val first = AttentionPicker.pick(day, 150, emptyList())

        val next = AttentionPicker.pick(day, 150, listOf(first))

        assertThat(next).isNotEqualTo(first)
    }

    @Test
    fun `skipped attentions are not picked either`() {
        val first = AttentionPicker.pick(day, 150, emptyList())
        val second = AttentionPicker.pick(day, 150, emptyList(), skip = listOf(first))
        val third = AttentionPicker.pick(day, 150, emptyList(), skip = listOf(first, second))

        assertThat(setOf(first, second, third)).hasSize(3)
    }

    @Test
    fun `consecutive days do not propose neighbouring attentions`() {
        val a = AttentionPicker.pick(day, 150, emptyList())
        val b = AttentionPicker.pick(day.plusDays(1), 150, emptyList())

        assertThat(Math.abs(a - b)).isGreaterThan(1)
    }

    @Test
    fun `when everything is excluded a repeat is accepted`() {
        val id = AttentionPicker.pick(day, 3, recentIds = listOf(0, 1, 2))

        assertThat(id).isIn(0 until 3)
    }

    @Test
    fun `an empty bank is refused`() {
        assertThrows(IllegalArgumentException::class.java) { AttentionPicker.pick(day, 0, emptyList()) }
    }
}
