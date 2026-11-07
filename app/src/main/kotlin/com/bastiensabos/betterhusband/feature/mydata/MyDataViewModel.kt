package com.bastiensabos.betterhusband.feature.mydata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.data.export.DocumentWriter
import com.bastiensabos.betterhusband.data.security.BiometricAuthenticator
import com.bastiensabos.betterhusband.data.security.LockSettings
import com.bastiensabos.betterhusband.domain.DeleteAllUserDataUseCase
import com.bastiensabos.betterhusband.domain.ExportUserDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** « Mes données » : verrouillage, export en fichier, ou tout effacer après confirmation. */
@HiltViewModel
class MyDataViewModel
    @Inject
    constructor(
        private val exportUserData: ExportUserDataUseCase,
        private val deleteAllUserData: DeleteAllUserDataUseCase,
        private val writer: DocumentWriter,
        private val lockSettings: LockSettings,
        authenticator: BiometricAuthenticator,
        private val clock: Clock,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                MyDataUiState(lockEnabled = lockSettings.enabled.value, lockAvailable = authenticator.canAuthenticate()),
            )
        val uiState: StateFlow<MyDataUiState> = _uiState.asStateFlow()

        fun onLockChange(enabled: Boolean) {
            // Un verrou sans moyen de l'ouvrir enfermerait l'utilisateur dehors : on refuse de l'activer.
            if (enabled && !_uiState.value.lockAvailable) return
            lockSettings.setEnabled(enabled)
            _uiState.update { it.copy(lockEnabled = enabled) }
        }

        /** Nom proposé dans le sélecteur de fichiers : daté, sans aucune donnée personnelle. */
        fun suggestedFileName(): String = "mes-donnees-${LocalDate.now(clock)}.json"

        /** [uri] est le document que l'utilisateur vient de choisir. */
        fun onExportTo(uri: String) {
            if (_uiState.value.exportStatus == ExportStatus.RUNNING) return
            _uiState.update { it.copy(exportStatus = ExportStatus.RUNNING) }
            viewModelScope.launch {
                val status =
                    try {
                        writer.write(uri, exportUserData())
                        ExportStatus.DONE
                    } catch (_: IOException) {
                        ExportStatus.FAILED
                    }
                _uiState.update { it.copy(exportStatus = status) }
            }
        }

        /** L'utilisateur a fermé le sélecteur sans choisir : rien à dire, on revient au repos. */
        fun onExportCancel() = _uiState.update { it.copy(exportStatus = ExportStatus.IDLE) }

        fun onDeleteRequest() = _uiState.update { it.copy(confirmingDelete = true) }

        fun onDeleteDismiss() = _uiState.update { it.copy(confirmingDelete = false) }

        fun onDeleteConfirm() {
            if (_uiState.value.deleting) return
            _uiState.update { it.copy(confirmingDelete = false, deleting = true) }
            viewModelScope.launch {
                deleteAllUserData()
                // La fiche effacée, l'app repart seule sur l'onboarding : rien d'autre à faire ici.
                _uiState.update { it.copy(deleting = false) }
            }
        }
    }
