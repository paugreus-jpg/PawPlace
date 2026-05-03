package com.example.dogmap.ui.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dogmap.R
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.viewmodel.NotificationsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(onBackClick: () -> Unit) {
    val vm: NotificationsViewModel = viewModel(factory = LocalViewModelFactory.current)
    val settings by vm.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            SettingRow(
                label = stringResource(R.string.notif_global),
                checked = settings.globalEnabled,
                onCheckedChange = vm::toggleGlobal
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SettingRow(
                label = stringResource(R.string.notif_social),
                checked = settings.socialEnabled,
                enabled = settings.globalEnabled,
                onCheckedChange = vm::toggleSocial
            )
            SettingRow(
                label = stringResource(R.string.notif_reminders),
                checked = settings.remindersEnabled,
                enabled = settings.globalEnabled,
                onCheckedChange = vm::toggleReminders
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                stringResource(R.string.notif_dnd),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))

            val dndEnabled = settings.dndStartHour >= 0 && settings.dndEndHour >= 0
            SettingRow(
                label = if (dndEnabled)
                    "${stringResource(R.string.notif_dnd_from)} ${settings.dndStartHour}:00 " +
                        "${stringResource(R.string.notif_dnd_to)} ${settings.dndEndHour}:00"
                else stringResource(R.string.notif_dnd_disabled),
                checked = dndEnabled,
                onCheckedChange = { on ->
                    if (on) vm.setDnd(22, 8) else vm.setDnd(-1, -1)
                }
            )

            if (dndEnabled) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${stringResource(R.string.notif_dnd_from)}: ${settings.dndStartHour}:00",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = settings.dndStartHour.toFloat(),
                    onValueChange = { vm.setDnd(it.roundToInt(), settings.dndEndHour) },
                    valueRange = 0f..23f,
                    steps = 22
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${stringResource(R.string.notif_dnd_to)}: ${settings.dndEndHour}:00",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = settings.dndEndHour.toFloat(),
                    onValueChange = { vm.setDnd(settings.dndStartHour, it.roundToInt()) },
                    valueRange = 0f..23f,
                    steps = 22
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                   else MaterialTheme.colorScheme.outline
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
