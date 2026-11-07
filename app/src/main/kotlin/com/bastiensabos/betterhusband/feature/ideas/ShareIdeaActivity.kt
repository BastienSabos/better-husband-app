package com.bastiensabos.betterhusband.feature.ideas

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Cible du partage Android (texte) : « Ajouter au carnet ». Reste hors de la liste des tâches récentes pour ne
 * pas laisser traîner un brouillon, et n'enregistre rien sans que l'utilisateur appuie sur le bouton.
 */
@AndroidEntryPoint
class ShareIdeaActivity : ComponentActivity() {
    private val viewModel: ShareIdeaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null && intent?.action == Intent.ACTION_SEND) {
            viewModel.onSharedText(intent.getCharSequenceExtra(Intent.EXTRA_TEXT))
        }
        setContent {
            BetterHusbandTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(state.saved) {
                    if (state.saved) {
                        Toast.makeText(this@ShareIdeaActivity, R.string.share_idea_saved, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                ShareIdeaScreen(
                    uiState = state,
                    onTextChange = viewModel::onTextChange,
                    onCategoryChange = viewModel::onCategoryChange,
                    onSave = viewModel::onSave,
                    onCancel = ::finish,
                )
            }
        }
    }
}
