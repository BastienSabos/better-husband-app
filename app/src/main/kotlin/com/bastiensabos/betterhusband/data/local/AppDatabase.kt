package com.bastiensabos.betterhusband.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bastiensabos.betterhusband.data.local.dao.PartnerDao
import com.bastiensabos.betterhusband.data.local.entity.PartnerEntity

/**
 * Base unique chiffrée par SQLCipher (voir docs/adr/0002). Toute évolution de schéma passe par une migration
 * explicite : jamais de migration destructive.
 */
@Database(entities = [PartnerEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partnerDao(): PartnerDao

    companion object {
        const val NAME = "better_husband.db"
    }
}
