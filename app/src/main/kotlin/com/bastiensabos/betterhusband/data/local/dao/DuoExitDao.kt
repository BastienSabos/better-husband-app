package com.bastiensabos.betterhusband.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/**
 * Sortie du duo sur ce téléphone (`docs/duo.md`, « Sortie du duo »), en une transaction : tout ou rien. Les tâches et
 * les dates restent, copiées en solo ; les envies, les marqueurs de suppression et le duo lui-même disparaissent.
 */
@Dao
abstract class DuoExitDao {
    /** La clé de couple n'est pas en base : le dépôt l'efface du Keystore après cette transaction. */
    @Transaction
    open suspend fun leave() {
        deleteWishes()
        deleteTaskTombstones()
        tasksToSolo()
        deleteDateTombstones()
        datesToSolo()
        unlinkIdeas()
        deleteMembers()
        deleteState()
    }

    @Query("DELETE FROM wish")
    protected abstract suspend fun deleteWishes()

    @Query("DELETE FROM household_task WHERE deleted = 1")
    protected abstract suspend fun deleteTaskTombstones()

    @Query("UPDATE household_task SET shared = 0, author = NULL, assignee = NULL, last_done_by = NULL")
    protected abstract suspend fun tasksToSolo()

    @Query("DELETE FROM important_date WHERE deleted = 1")
    protected abstract suspend fun deleteDateTombstones()

    @Query("UPDATE important_date SET shared = 0, author = NULL")
    protected abstract suspend fun datesToSolo()

    @Query("UPDATE idea SET wish_uid = NULL")
    protected abstract suspend fun unlinkIdeas()

    @Query("DELETE FROM duo_member")
    protected abstract suspend fun deleteMembers()

    @Query("DELETE FROM duo_state")
    protected abstract suspend fun deleteState()
}
