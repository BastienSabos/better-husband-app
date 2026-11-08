package com.bastiensabos.betterhusband.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bastiensabos.betterhusband.data.local.entity.ImportantDateEntity
import com.bastiensabos.betterhusband.data.local.entity.SyncColumns
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportantDateDao {
    @Query("SELECT * FROM important_date WHERE deleted = 0 ORDER BY date, id")
    fun observeAll(): Flow<List<ImportantDateEntity>>

    /** Colonnes de synchro de la ligne, pour qu'une modification n'en change pas l'identifiant ni le partage. */
    @Query("SELECT uid, shared, revision, author, deleted, local_changed_at FROM important_date WHERE id = :id")
    suspend fun syncOf(id: Long): SyncColumns?

    /** Retourne l'identifiant de la ligne (créée ou remplacée). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ImportantDateEntity): Long

    @Query("DELETE FROM important_date WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM important_date")
    suspend fun deleteAll()
}
