package app.lastcalled.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lastcalled.ui.theme.Sage

@Composable
fun PermissionGate(
    isCallLogStep: Boolean,
    permanentlyDenied: Boolean,
    onRequestCallLog: () -> Unit,
    onRequestContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinueWithoutContacts: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Last Called",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = if (isCallLogStep) "Your call history stays on this phone."
                else "Match calls with the people you know.",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isCallLogStep) {
                    "Call log access lets the app calculate when you last called someone. Calls are read and stored locally. There is no account, upload, or internet connection."
                } else {
                    "Contacts access is used only to match local phone numbers to names and photos. If you skip it, calls still appear as unknown callers."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Sage.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        text = if (isCallLogStep) "Permission 1 of 2 · Call history"
                        else "Permission 2 of 2 · Contacts",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isCallLogStep) {
                            "Android’s READ_CALL_LOG permission gives access to entries Android makes available. It cannot reveal calls that were never added to the system call log."
                        } else {
                            "Android’s READ_CONTACTS permission allows local name and phone-number matching. Your contacts are not copied outside this app."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))

            if (permanentlyDenied) {
                Text(
                    text = "Access was denied. Enable it in Android app settings to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Open app settings")
                }
            } else if (isCallLogStep) {
                Button(onClick = onRequestCallLog, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow call history")
                }
            } else {
                Button(onClick = onRequestContacts, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow contacts")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onContinueWithoutContacts,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Continue without contacts")
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "WhatsApp note: Android does not give this app direct access to WhatsApp’s private call history. Some calls may appear in Android’s call log; anything missing stays missing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
