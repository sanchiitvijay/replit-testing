package app.lastcalled.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lastcalled.ui.CallTrackerViewModel
import app.lastcalled.ui.theme.Border
import java.util.Date
import java.text.DateFormat

private enum class DestructiveAction { RESET_CALLS, CLEAR_APP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CallTrackerViewModel,
    hasCallLogAccess: Boolean,
    hasContactsAccess: Boolean,
    onBack: () -> Unit,
    onManagePermissions: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    var confirmation by remember { mutableStateOf<DestructiveAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Settings", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Local controls and data access",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (notice != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Text(
                        notice.orEmpty(),
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            SettingsSection(title = "Device access") {
                PermissionStatusRow(
                    title = "Call history",
                    detail = if (hasCallLogAccess) "Allowed" else "Required to import calls",
                    granted = hasCallLogAccess,
                )
                HorizontalDivider(color = Border)
                PermissionStatusRow(
                    title = "Contacts",
                    detail = if (hasContactsAccess) "Allowed" else "Optional · unmatched calls remain unknown",
                    granted = hasContactsAccess,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onManagePermissions,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Manage Android permissions")
                }
            }

            SettingsSection(title = "Check-in list") {
                Text(
                    text = "Default time filter",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Show people you have not called in at least this many days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 7, 14, 30).forEach { days ->
                        FilterChip(
                            selected = settings.defaultThresholdDays == days,
                            onClick = { viewModel.updateDefaultThreshold(days) },
                            label = { Text("${days}d") },
                        )
                    }
                }
                HorizontalDivider(color = Border)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            "Refresh when opened",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Re-read Android call history when this app returns to the foreground.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.refreshOnForeground,
                        onCheckedChange = viewModel::updateRefreshOnForeground,
                    )
                }
            }

            SettingsSection(title = "WhatsApp and other calling apps") {
                Text(
                    text = "Android does not let this app open WhatsApp’s private call database. Last Called imports WhatsApp calls only when Android exposes them in its call log and their source can be identified. Missing calls are not reconstructed or guessed.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "On Android 17, unified call history is controlled by Android and each calling app. This app does not request notification access or monitor notification text.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection(title = "Local data") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Stored in this app’s private data area. No internet permission, account, server, analytics, or sync.",
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                settings.lastSuccessfulRefreshAt?.let { lastRefresh ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Last imported ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastRefresh))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::refresh,
                    enabled = hasCallLogAccess && !refreshing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("  Refresh call history")
                }
                TextButton(
                    onClick = { confirmation = DestructiveAction.RESET_CALLS },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                    Text("  Reset imported call records")
                }
                OutlinedButton(
                    onClick = { confirmation = DestructiveAction.CLEAR_APP },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Clear all app data")
                }
                Text(
                    "These actions only affect Last Called’s local database. They never delete calls from your phone or WhatsApp.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "For a complete privacy and platform explanation, see PRIVACY.md and WHATSAPP_SUPPORT.md in the project.",
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    confirmation?.let { action ->
        val clearAll = action == DestructiveAction.CLEAR_APP
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(if (clearAll) "Clear all app data?" else "Reset imported calls?") },
            text = {
                Text(
                    if (clearAll) {
                        "This removes this app’s saved contacts, imported call records, and preferences. It does not delete phone or WhatsApp call history."
                    } else {
                        "This removes imported call records and unknown-caller groups from this app. Your phone’s actual call history is not changed."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (clearAll) viewModel.clearLocalData()
                        else viewModel.resetImportedCalls()
                        confirmation = null
                    },
                ) {
                    Text(if (clearAll) "Clear app data" else "Reset records")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(21.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun PermissionStatusRow(title: String, detail: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (granted) "On" else "Off",
            style = MaterialTheme.typography.labelMedium,
            color = if (granted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
        )
    }
}
