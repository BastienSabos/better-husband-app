package com.bastiensabos.betterhusband.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bastiensabos.betterhusband.data.local.entity.PartnerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerDao {
    @Query("SELECT * FROM partner WHERE id = ${PartnerEntity.SINGLE_PARTNER_ID}")
    fun observe(): Flow<PartnerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(partner: PartnerEntity)

    @Query("DELETE FROM partner")
    suspend fun delete()
}
