package com.bastiensabos.betterhusband.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bastiensabos.betterhusband.data.local.entity.DuoMemberEntity
import com.bastiensabos.betterhusband.data.local.entity.DuoStateEntity
import com.bastiensabos.betterhusband.data.local.entity.WishEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishDao {
    @Query("SELECT * FROM wish WHERE deleted = 0 ORDER BY created_at DESC, id DESC")
    fun observeAll(): Flow<List<WishEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WishEntity): Long

    @Query("DELETE FROM wish")
    suspend fun deleteAll()
}

@Dao
interface DuoDao {
    @Query("SELECT * FROM duo_member")
    fun observeMembers(): Flow<List<DuoMemberEntity>>

    @Query("SELECT * FROM duo_state WHERE id = 1")
    fun observeState(): Flow<DuoStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(entity: DuoMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(entity: DuoStateEntity)

    @Query("DELETE FROM duo_member")
    suspend fun deleteMembers()

    @Query("DELETE FROM duo_state")
    suspend fun deleteState()
}
