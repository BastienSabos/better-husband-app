package com.bastiensabos.betterhusband.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "idea")
data class IdeaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "category") val category: String,
    /** Millisecondes depuis l'epoch UTC. */
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "done") val done: Boolean,
    /** `uid` de l'envie de l'autre à laquelle l'idée répond ; l'idée reste privée (`docs/duo.md`). */
    @ColumnInfo(name = "wish_uid") val wishUid: String? = null,
)
