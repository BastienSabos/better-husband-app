package com.bastiensabos.betterhusband.data.local.entity

import androidx.room.ColumnInfo
import java.util.UUID

/**
 * Colonnes de synchro d'un enregistrement qui peut être partagé dans le mode duo (ADR 0004, `docs/duo.md`).
 * En solo, [shared] reste faux et rien ne part. Un enregistrement partagé supprimé garde une ligne ([deleted])
 * jusqu'à ce que l'autre téléphone l'ait reçue.
 */
data class SyncColumns(
    @ColumnInfo(name = "uid", defaultValue = "") val uid: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "shared", defaultValue = "0") val shared: Boolean = false,
    @ColumnInfo(name = "revision", defaultValue = "0") val revision: Long = 0,
    @ColumnInfo(name = "author") val author: String? = null,
    @ColumnInfo(name = "deleted", defaultValue = "0") val deleted: Boolean = false,
    @ColumnInfo(name = "local_changed_at", defaultValue = "0") val localChangedAt: Long = 0,
)
