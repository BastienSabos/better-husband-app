package com.bastiensabos.betterhusband.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bastiensabos.betterhusband.data.local.entity.HouseholdTaskEntity
import com.bastiensabos.betterhusband.data.local.entity.SyncColumns
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HouseholdTaskDao {
    @Query("SELECT * FROM household_task WHERE deleted = 0 ORDER BY id")
    fun observeAll(): Flow<List<HouseholdTaskEntity>>

    /** Colonnes de synchro de la ligne, pour qu'une modification n'en change pas l'identifiant ni le partage. */
    @Query("SELECT uid, shared, revision, author, deleted, local_changed_at FROM household_task WHERE id = :id")
    suspend fun syncOf(id: Long): SyncColumns?

    /** Retourne l'identifiant de la ligne (créée ou remplacée). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HouseholdTaskEntity): Long

    @Query("UPDATE household_task SET last_done_on = :date WHERE id = :id")
    suspend fun setLastDone(
        id: Long,
        date: LocalDate?,
    )

    @Query("DELETE FROM household_task WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM household_task")
    suspend fun deleteAll()
}
