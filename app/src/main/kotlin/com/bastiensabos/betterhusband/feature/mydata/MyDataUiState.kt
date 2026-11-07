package com.bastiensabos.betterhusband.feature.mydata

enum class ExportStatus { IDLE, RUNNING, DONE, FAILED }

data class MyDataUiState(
    val lockEnabled: Boolean = false,
    /** Faux si l'appareil n'a ni biométrie ni code d'écran : on ne peut pas activer un verrou qui ne s'ouvre pas. */
    val lockAvailable: Boolean = true,
    val exportStatus: ExportStatus = ExportStatus.IDLE,
    val confirmingDelete: Boolean = false,
    val deleting: Boolean = false,
)
