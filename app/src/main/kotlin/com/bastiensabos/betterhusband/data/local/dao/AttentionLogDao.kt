package com.bastiensabos.betterhusband.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bastiensabos.betterhusband.data.local.entity.AttentionLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AttentionLogDao {
    @Query("SELECT * FROM attention_log ORDER BY date")
    fun observeAll(): Flow<List<AttentionLogEntity>>

    /** Une ligne par jour : choisir une autre attention le même jour remplace la ligne. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AttentionLogEntity)

    @Query("UPDATE attention_log SET done = :done WHERE date = :date")
    suspend fun setDone(
        date: LocalDate,
        done: Boolean,
    )

    @Query("DELETE FROM attention_log")
    suspend fun deleteAll()
}
