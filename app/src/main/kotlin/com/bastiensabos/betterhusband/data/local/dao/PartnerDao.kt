package com.bastiensabos.betterhusband.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bastiensabos.betterhusband.data.local.entity.PartnerEntity
import com.bastiensabos.betterhusband.data.local.entity.RelativeEntity
import com.bastiensabos.betterhusband.data.local.entity.SizeEntity
import com.bastiensabos.betterhusband.data.local.entity.TasteEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PartnerDao {
    @Query("SELECT * FROM partner WHERE id = ${PartnerEntity.SINGLE_PARTNER_ID}")
    abstract fun observePartner(): Flow<PartnerEntity?>

    @Query("SELECT * FROM partner_taste ORDER BY id")
    abstract fun observeTastes(): Flow<List<TasteEntity>>

    @Query("SELECT * FROM partner_size")
    abstract fun observeSizes(): Flow<List<SizeEntity>>

    @Query("SELECT * FROM partner_relative ORDER BY id")
    abstract fun observeRelatives(): Flow<List<RelativeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertPartner(partner: PartnerEntity)

    @Insert
    abstract suspend fun insertTastes(tastes: List<TasteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSizes(sizes: List<SizeEntity>)

    @Insert
    abstract suspend fun insertRelatives(relatives: List<RelativeEntity>)

    @Query("DELETE FROM partner")
    abstract suspend fun deletePartner()

    @Query("DELETE FROM partner_taste")
    abstract suspend fun deleteTastes()

    @Query("DELETE FROM partner_size")
    abstract suspend fun deleteSizes()

    @Query("DELETE FROM partner_relative")
    abstract suspend fun deleteRelatives()

    /** Remplace toute la fiche d'un bloc : l'écran d'édition enregistre l'ensemble ou rien. */
    @Transaction
    open suspend fun replaceProfile(
        partner: PartnerEntity,
        tastes: List<TasteEntity>,
        sizes: List<SizeEntity>,
        relatives: List<RelativeEntity>,
    ) {
        upsertPartner(partner)
        deleteTastes()
        insertTastes(tastes)
        deleteSizes()
        insertSizes(sizes)
        deleteRelatives()
        insertRelatives(relatives)
    }

    @Transaction
    open suspend fun deleteAll() {
        deletePartner()
        deleteTastes()
        deleteSizes()
        deleteRelatives()
    }
}
