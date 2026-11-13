package com.bastiensabos.betterhusband.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "household_task", indices = [Index(value = ["uid"], unique = true)])
data class HouseholdTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "repeat") val repeat: String,
    @ColumnInfo(name = "created_on") val createdOn: LocalDate,
    @ColumnInfo(name = "last_done_on") val lastDoneOn: LocalDate?,
    @ColumnInfo(name = "assignee") val assignee: String? = null,
    @ColumnInfo(name = "last_done_by") val lastDoneBy: String? = null,
    @Embedded val sync: SyncColumns = SyncColumns(),
)
