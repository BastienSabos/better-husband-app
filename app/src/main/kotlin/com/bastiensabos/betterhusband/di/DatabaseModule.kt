package com.bastiensabos.betterhusband.di

import android.content.Context
import androidx.room.Room
import com.bastiensabos.betterhusband.data.local.AppDatabase
import com.bastiensabos.betterhusband.data.local.DatabaseKeyProvider
import com.bastiensabos.betterhusband.data.local.KeystoreDatabaseKeyProvider
import com.bastiensabos.betterhusband.data.local.dao.PartnerDao
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
            .build()
    }

    @Provides
    fun providePartnerDao(database: AppDatabase): PartnerDao = database.partnerDao()
}
