package com.bastiensabos.betterhusband.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Ligne unique : le profil du couple. `null` = question pas répondue ; [interests] : noms séparés par des virgules. */
@Entity(tableName = "couple_profile")
data class CoupleProfileEntity(
    @PrimaryKey val id: Int = SINGLE_PROFILE_ID,
    @ColumnInfo(name = "kids") val kids: String?,
    @ColumnInfo(name = "living") val living: String?,
    @ColumnInfo(name = "budget") val budget: String?,
    @ColumnInfo(name = "rhythm") val rhythm: String?,
    @ColumnInfo(name = "interests") val interests: String,
    /** [com.bastiensabos.betterhusband.domain.model.FamilySituation], noms séparés par des virgules. */
    @ColumnInfo(name = "family", defaultValue = "") val family: String = "",
) {
    companion object {
        const val SINGLE_PROFILE_ID = 1
    }
}
