package com.bastiensabos.betterhusband.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/** Dates stockées en ISO-8601 (`2026-11-06`) : lisibles, triables, indépendantes du fuseau. */
class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
}
