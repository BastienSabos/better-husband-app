package com.bastiensabos.betterhusband.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bastiensabos.betterhusband.data.local.dao.AttentionLogDao
import com.bastiensabos.betterhusband.data.local.dao.CoupleProfileDao
import com.bastiensabos.betterhusband.data.local.dao.DuoDao
import com.bastiensabos.betterhusband.data.local.dao.HouseholdTaskDao
import com.bastiensabos.betterhusband.data.local.dao.IdeaDao
import com.bastiensabos.betterhusband.data.local.dao.ImportantDateDao
import com.bastiensabos.betterhusband.data.local.dao.PartnerDao
import com.bastiensabos.betterhusband.data.local.dao.WishDao
import com.bastiensabos.betterhusband.data.local.entity.AttentionLogEntity
import com.bastiensabos.betterhusband.data.local.entity.CoupleProfileEntity
import com.bastiensabos.betterhusband.data.local.entity.DuoMemberEntity
import com.bastiensabos.betterhusband.data.local.entity.DuoStateEntity
import com.bastiensabos.betterhusband.data.local.entity.HouseholdTaskEntity
import com.bastiensabos.betterhusband.data.local.entity.IdeaEntity
import com.bastiensabos.betterhusband.data.local.entity.ImportantDateEntity
import com.bastiensabos.betterhusband.data.local.entity.PartnerEntity
import com.bastiensabos.betterhusband.data.local.entity.RelativeEntity
import com.bastiensabos.betterhusband.data.local.entity.SizeEntity
import com.bastiensabos.betterhusband.data.local.entity.TasteEntity
import com.bastiensabos.betterhusband.data.local.entity.WishEntity

/**
 * Base unique chiffrée par SQLCipher (voir docs/adr/0002). Toute évolution de schéma passe par une migration
 * explicite : jamais de migration destructive.
 */
@Database(
    entities = [
        PartnerEntity::class,
        TasteEntity::class,
        SizeEntity::class,
        RelativeEntity::class,
        ImportantDateEntity::class,
        IdeaEntity::class,
        HouseholdTaskEntity::class,
        AttentionLogEntity::class,
        CoupleProfileEntity::class,
        WishEntity::class,
        DuoMemberEntity::class,
        DuoStateEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partnerDao(): PartnerDao

    abstract fun importantDateDao(): ImportantDateDao

    abstract fun ideaDao(): IdeaDao

    abstract fun householdTaskDao(): HouseholdTaskDao

    abstract fun attentionLogDao(): AttentionLogDao

    abstract fun coupleProfileDao(): CoupleProfileDao

    abstract fun wishDao(): WishDao

    abstract fun duoDao(): DuoDao

    companion object {
        const val NAME = "better_husband.db"
    }
}
