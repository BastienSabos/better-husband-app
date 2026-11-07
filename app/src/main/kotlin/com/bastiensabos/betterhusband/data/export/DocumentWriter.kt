package com.bastiensabos.betterhusband.data.export

import android.content.Context
import androidx.core.net.toUri
import com.bastiensabos.betterhusband.di.IoDispatcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/** Écrit un texte dans le document que l'utilisateur a choisi (Storage Access Framework, sans permission). */
interface DocumentWriter {
    /** Lève [IOException] si le document ne peut pas être écrit. */
    suspend fun write(
        uri: String,
        content: String,
    )
}

class ContentResolverDocumentWriter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) : DocumentWriter {
        override suspend fun write(
            uri: String,
            content: String,
        ) = withContext(io) {
            // "wt" : tronque un fichier existant, sinon l'ancien contenu resterait derrière le nouveau.
            val stream = context.contentResolver.openOutputStream(uri.toUri(), "wt") ?: throw IOException("No stream")
            stream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class DocumentWriterModule {
    @Binds
    abstract fun bindDocumentWriter(impl: ContentResolverDocumentWriter): DocumentWriter
}
