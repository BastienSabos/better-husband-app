package com.bastiensabos.betterhusband.di

import android.content.Context
import androidx.room.Room
import com.bastiensabos.betterhusband.data.local.AppDatabase
import com.bastiensabos.betterhusband.data.local.DatabaseKeyProvider
import com.bastiensabos.betterhusband.data.local.KeystoreDatabaseKeyProvider
import com.bastiensabos.betterhusband.data.local.MIGRATION_10_11
import com.bastiensabos.betterhusband.data.local.MIGRATION_1_2
import com.bastiensabos.betterhusband.data.local.MIGRATION_2_3
import com.bastiensabos.betterhusband.data.local.MIGRATION_3_4
import com.bastiensabos.betterhusband.data.local.MIGRATION_4_5
import com.bastiensabos.betterhusband.data.local.MIGRATION_5_6
import com.bastiensabos.betterhusband.data.local.MIGRATION_6_7
import com.bastiensabos.betterhusband.data.local.MIGRATION_7_8
import com.bastiensabos.betterhusband.data.local.MIGRATION_8_9
import com.bastiensabos.betterhusband.data.local.MIGRATION_9_10
import com.bastiensabos.betterhusband.data.local.dao.AttentionLogDao
import com.bastiensabos.betterhusband.data.local.dao.CoupleProfileDao
import com.bastiensabos.betterhusband.data.local.dao.DuoDao
import com.bastiensabos.betterhusband.data.local.dao.DuoExitDao
import com.bastiensabos.betterhusband.data.local.dao.HouseholdTaskDao
import com.bastiensabos.betterhusband.data.local.dao.IdeaDao
import com.bastiensabos.betterhusband.data.local.dao.ImportantDateDao
import com.bastiensabos.betterhusband.data.local.dao.PartnerDao
import com.bastiensabos.betterhusband.data.local.dao.WishDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindingsModule {
    @Binds
    abstract fun bindKeyProvider(impl: KeystoreDatabaseKeyProvider): DatabaseKeyProvider
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider,
    ): AppDatabase {
        System.loadLibrary("sqlcipher")
        return Room
            .databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(keyProvider.passphrase()))
            // Jamais de migration destructive : on ne perd pas les souvenirs d'un couple.
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
            ).build()
    }

    @Provides
    fun providePartnerDao(database: AppDatabase): PartnerDao = database.partnerDao()

    @Provides
    fun provideImportantDateDao(database: AppDatabase): ImportantDateDao = database.importantDateDao()

    @Provides
    fun provideIdeaDao(database: AppDatabase): IdeaDao = database.ideaDao()

    @Provides
    fun provideHouseholdTaskDao(database: AppDatabase): HouseholdTaskDao = database.householdTaskDao()

    @Provides
    fun provideWishDao(database: AppDatabase): WishDao = database.wishDao()

    @Provides
    fun provideDuoDao(database: AppDatabase): DuoDao = database.duoDao()

    @Provides
    fun provideDuoExitDao(database: AppDatabase): DuoExitDao = database.duoExitDao()

    @Provides
    fun provideAttentionLogDao(database: AppDatabase): AttentionLogDao = database.attentionLogDao()

    @Provides
    fun provideCoupleProfileDao(database: AppDatabase): CoupleProfileDao = database.coupleProfileDao()
}
