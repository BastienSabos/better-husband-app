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
            /** Une liste par groupe ; un test d'architecture vérifie qu'aucune liste n'est oubliée. */
            val ARRAYS =
                mapOf(
                    AttentionGroup.EVERYONE to R.array.attention_bank,
                    AttentionGroup.IN_PERSON to R.array.attention_bank_in_person,
                    AttentionGroup.TOGETHER to R.array.attention_bank_together,
                    AttentionGroup.APART to R.array.attention_bank_apart,
                    AttentionGroup.LONG_DISTANCE to R.array.attention_bank_long_distance,
                    AttentionGroup.NO_KIDS to R.array.attention_bank_no_kids,
                    AttentionGroup.KIDS to R.array.attention_bank_kids,
                    AttentionGroup.YOUNG_KIDS to R.array.attention_bank_young_kids,
                    AttentionGroup.TEENS to R.array.attention_bank_teens,
                    AttentionGroup.NEW_COUPLE to R.array.attention_bank_new_couple,
                    AttentionGroup.LONG_COUPLE to R.array.attention_bank_long_couple,
                    AttentionGroup.TREATS to R.array.attention_bank_treats,
                    AttentionGroup.SPLURGE to R.array.attention_bank_splurge,
                    AttentionGroup.TAKES_TIME to R.array.attention_bank_takes_time,
                    AttentionGroup.WINTER to R.array.attention_bank_winter,
                    AttentionGroup.SPRING to R.array.attention_bank_spring,
                    AttentionGroup.SUMMER to R.array.attention_bank_summer,
                    AttentionGroup.AUTUMN to R.array.attention_bank_autumn,
                    AttentionGroup.NATURE to R.array.attention_bank_nature,
                    AttentionGroup.CULTURE to R.array.attention_bank_culture,
                    AttentionGroup.SPORT to R.array.attention_bank_sport,
                    AttentionGroup.COOKING to R.array.attention_bank_cooking,
                    AttentionGroup.GAMES to R.array.attention_bank_games,
                    AttentionGroup.TRAVEL to R.array.attention_bank_travel,
                    AttentionGroup.MUSIC to R.array.attention_bank_music,
                    AttentionGroup.READING to R.array.attention_bank_reading,
                    AttentionGroup.DIY to R.array.attention_bank_diy,
                    AttentionGroup.WELLBEING to R.array.attention_bank_wellbeing,
                )
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class AttentionBankModule {
    @Binds
    abstract fun bindAttentionBank(impl: ResourceAttentionBank): AttentionBank
}
