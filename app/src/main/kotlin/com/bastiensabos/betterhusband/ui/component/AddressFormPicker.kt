package com.bastiensabos.betterhusband.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.ui.theme.Spacing

/** Choix « mon partenaire / ma partenaire / sans accord de genre », partagé par l'onboarding et la fiche. */
@Composable
fun AddressFormPicker(
    selected: AddressForm,
    onSelected: (AddressForm) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = Spacing()
    Column(modifier = modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        Text(
            text = stringResource(R.string.onboarding_address_form_question),
            style = MaterialTheme.typography.titleMedium,
        )
        AddressForm.entries.forEach { form ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = spacing.touchTarget)
                        .selectable(
                            selected = selected == form,
                            onClick = { onSelected(form) },
                            role = Role.RadioButton,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Le clic est géré par la ligne entière (cible ≥ 48 dp) : pas de callback sur le bouton.
                RadioButton(selected = selected == form, onClick = null)
                Text(text = stringResource(form.labelRes()), modifier = Modifier.padding(start = spacing.sm))
            }
        }
    }
}

@StringRes
private fun AddressForm.labelRes(): Int =
    when (this) {
        AddressForm.MASCULINE -> R.string.address_form_masculine
        AddressForm.FEMININE -> R.string.address_form_feminine
        AddressForm.NEUTRAL -> R.string.address_form_neutral
    }
