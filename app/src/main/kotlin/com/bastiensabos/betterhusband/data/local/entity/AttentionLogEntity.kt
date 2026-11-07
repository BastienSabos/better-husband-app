package com.bastiensabos.betterhusband.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "attention_log")
data class AttentionLogEntity(
    @PrimaryKey @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "attention_id") val attentionId: Int,
    @ColumnInfo(name = "done") val done: Boolean,
)
