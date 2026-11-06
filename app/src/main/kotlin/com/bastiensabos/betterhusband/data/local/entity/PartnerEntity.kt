package com.bastiensabos.betterhusband.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Ligne unique (id fixe) : la v1 gère un seul ou une seule partenaire. */
@Entity(tableName = "partner")
data class PartnerEntity(
    @PrimaryKey val id: Int = SINGLE_PARTNER_ID,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "address_form") val addressForm: String,
    @ColumnInfo(name = "birthday") val birthday: LocalDate?,
    @ColumnInfo(name = "relationship_start") val relationshipStart: LocalDate?,
) {
    companion object {
        const val SINGLE_PARTNER_ID = 1
    }
}
