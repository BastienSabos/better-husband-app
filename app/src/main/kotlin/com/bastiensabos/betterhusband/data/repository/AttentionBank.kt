package com.bastiensabos.betterhusband.data.repository

import android.content.Context
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.AttentionGroup
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Banque d'attentions (`res/values/attentions.xml`), une liste par [AttentionGroup]. L'identifiant d'une attention
 * vaut l'offset de son groupe plus sa position : un groupe ne fait que s'allonger, jamais se réordonner.
 */
interface AttentionBank {
    /** Nombre d'attentions écrites dans chaque groupe ; un groupe absent n'en a pas encore. */
    val groupSizes: Map<AttentionGroup, Int>

    /** Texte de l'attention [id] ; un identifiant inconnu (groupe plus court) retombe sur la première. */
    fun text(id: Int): String
}

@Singleton
class ResourceAttentionBank
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AttentionBank {
        private val groups: Map<AttentionGroup, Array<String>> by lazy {
            ARRAYS.mapValues { (_, array) -> context.resources.getStringArray(array) }
        }

        override val groupSizes: Map<AttentionGroup, Int> get() = groups.mapValues { it.value.size }

        override fun text(id: Int): String {
            val text = AttentionGroup.of(id)?.let { group -> groups[group]?.getOrNull(id - group.offset) }
            return text ?: groups.getValue(AttentionGroup.EVERYONE).first()
        }

        private companion object {
            /** Les groupes déjà écrits ; les autres arrivent avec leurs textes. */
            val ARRAYS = mapOf(AttentionGroup.EVERYONE to R.array.attention_bank)
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class AttentionBankModule {
    @Binds
    abstract fun bindAttentionBank(impl: ResourceAttentionBank): AttentionBank
}
