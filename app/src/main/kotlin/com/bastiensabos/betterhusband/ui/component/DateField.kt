package com.bastiensabos.betterhusband.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Date optionnelle : un bouton qui ouvre le sélecteur Material, et un moyen de l'effacer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = Spacing()
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                Text(value?.format(formatter) ?: stringResource(R.string.date_field_empty))
            }
            if (value != null) {
                TextButton(onClick = { onValueChange(null) }, modifier = Modifier.heightIn(min = spacing.touchTarget)) {
                    Text(stringResource(R.string.date_field_clear))
                }
            }
        }
    }

    if (showPicker) {
        // Le sélecteur Material travaille en millisecondes UTC à minuit.
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            onValueChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showPicker = false
                    },
                ) { Text(stringResource(R.string.date_field_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.date_field_cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
