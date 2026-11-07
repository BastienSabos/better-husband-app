package com.bastiensabos.betterhusband.feature.mydata

enum class ExportStatus { IDLE, RUNNING, DONE, FAILED }

data class MyDataUiState(
    val exportStatus: ExportStatus = ExportStatus.IDLE,
    val confirmingDelete: Boolean = false,
    val deleting: Boolean = false,
)
