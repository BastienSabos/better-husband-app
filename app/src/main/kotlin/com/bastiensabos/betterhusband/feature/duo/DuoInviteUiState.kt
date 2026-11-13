package com.bastiensabos.betterhusband.feature.duo

/** Où en est l'invitation, pour celui qui invite comme pour celui qui rejoint. */
sealed interface DuoStage {
    data object Intro : DuoStage

    /** L'hôte a créé le lien et attend que l'autre l'ouvre. */
    data class HostWaiting(
        val url: String,
    ) : DuoStage

    /** L'invité a répondu et attend d'être ajouté. */
    data object GuestWaiting : DuoStage

    /** Les deux téléphones ont la même clé ; [code] (indices de mots, de 0 à 255) est à comparer, facultatif. */
    data class Joined(
        val code: List<Int>,
    ) : DuoStage
}

enum class DuoNotice { LINK_INVALID, LINK_UNAVAILABLE, ANSWER_REFUSED, SAVE_FAILED, LEAVE_FAILED }

data class DuoInviteUiState(
    val stage: DuoStage = DuoStage.Intro,
    val linkInput: String = "",
    val notice: DuoNotice? = null,
    /** Un duo est déjà gardé sur ce téléphone : l'écran propose d'en sortir plutôt que d'inviter. */
    val inDuo: Boolean = false,
    /** La confirmation « Quitter le duo » est ouverte. */
    val confirmLeave: Boolean = false,
) {
    val canJoin: Boolean get() = stage == DuoStage.Intro && !inDuo && linkInput.isNotBlank()
}
