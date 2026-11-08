package com.bastiensabos.betterhusband.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "important_date", indices = [Index(value = ["uid"], unique = true)])
data class ImportantDateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "recurring") val recurring: Boolean,
    @Embedded val sync: SyncColumns = SyncColumns(),
)
