package com.bastiensabos.betterhusband.di

import com.bastiensabos.betterhusband.domain.duo.DuoInviteServer
import com.bastiensabos.betterhusband.domain.duo.InMemoryDuoInviteServer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Tant que le projet Firebase n'existe pas, le serveur d'invitations est un faux en mémoire (aucun réseau). */
@Module
@InstallIn(SingletonComponent::class)
object DuoModule {
    @Provides
    @Singleton
    fun provideInviteServer(): DuoInviteServer = InMemoryDuoInviteServer()
}
