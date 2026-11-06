package com.bastiensabos.betterhusband.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Un goût (une fleur, un plat, une couleur, une marque). Catégorie stockée par nom d'enum. */
@Entity(tableName = "partner_taste")
data class TasteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "label") val label: String,
)

/** Une taille par type (vêtements, chaussures, bague). */
@Entity(tableName = "partner_size")
data class SizeEntity(
    @PrimaryKey @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "value") val value: String,
)

@Entity(tableName = "partner_relative")
data class RelativeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "relation") val relation: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "birthday") val birthday: LocalDate?,
)
