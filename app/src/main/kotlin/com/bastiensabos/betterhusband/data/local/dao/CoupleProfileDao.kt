package com.bastiensabos.betterhusband.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bastiensabos.betterhusband.data.local.entity.CoupleProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoupleProfileDao {
    @Query("SELECT * FROM couple_profile WHERE id = 1")
    fun observe(): Flow<CoupleProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CoupleProfileEntity)

    @Query("DELETE FROM couple_profile")
    suspend fun deleteAll()
}
