package com.bastiensabos.betterhusband.data.repository

import android.content.Context
import com.bastiensabos.betterhusband.R
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Banque d'attentions (`res/values/attentions.xml`). L'identifiant d'une attention est sa position dans la liste :
 * la banque ne fait que s'allonger, jamais se réordonner.
 */
interface AttentionBank {
    val size: Int

    /** Texte de l'attention [id] ; une position inconnue (banque plus courte) retombe sur la première. */
    fun text(id: Int): String
}

@Singleton
class ResourceAttentionBank
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AttentionBank {
        private val items: Array<String> by lazy { context.resources.getStringArray(R.array.attention_bank) }

        override val size: Int get() = items.size

        override fun text(id: Int): String = items.getOrElse(id) { items.first() }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class AttentionBankModule {
    @Binds
    abstract fun bindAttentionBank(impl: ResourceAttentionBank): AttentionBank
}
