package com.bastiensabos.betterhusband.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bastiensabos.betterhusband.data.local.dao.ImportantDateDao
import com.bastiensabos.betterhusband.data.local.dao.PartnerDao
import com.bastiensabos.betterhusband.data.local.entity.ImportantDateEntity
import com.bastiensabos.betterhusband.data.local.entity.PartnerEntity
import com.bastiensabos.betterhusband.data.local.entity.RelativeEntity
import com.bastiensabos.betterhusband.data.local.entity.SizeEntity
import com.bastiensabos.betterhusband.data.local.entity.TasteEntity

/**
 * Base unique chiffrée par SQLCipher (voir docs/adr/0002). Toute évolution de schéma passe par une migration
 * explicite : jamais de migration destructive.
 */
@Database(
    entities = [PartnerEntity::class, TasteEntity::class, SizeEntity::class, RelativeEntity::class, ImportantDateEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partnerDao(): PartnerDao

    abstract fun importantDateDao(): ImportantDateDao

    companion object {
        const val NAME = "better_husband.db"
    }
}
