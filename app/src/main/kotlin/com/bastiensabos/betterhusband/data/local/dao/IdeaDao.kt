package com.bastiensabos.betterhusband.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.bastiensabos.betterhusband.data.local.entity.IdeaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    /** À garder d'abord, les idées déjà faites en bas ; les plus récentes en premier. */
    @Query("SELECT * FROM idea ORDER BY done, created_at DESC, id DESC")
    fun observeAll(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM idea WHERE wish_uid = :wishUid LIMIT 1")
    suspend fun findByWish(wishUid: String): IdeaEntity?

    /** Retourne l'identifiant de la ligne créée. */
    @Insert
    suspend fun insert(entity: IdeaEntity): Long

    @Query("UPDATE idea SET text = :text, category = :category WHERE id = :id")
    suspend fun updateContent(
        id: Long,
        text: String,
        category: String,
    )

    @Query("UPDATE idea SET done = :done WHERE id = :id")
    suspend fun setDone(
        id: Long,
        done: Boolean,
    )

    @Query("DELETE FROM idea WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM idea")
    suspend fun deleteAll()
}
