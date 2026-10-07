package app.lastcalled.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lastcalled.data.local.CallRecordEntity
import app.lastcalled.domain.CallDirection
import app.lastcalled.domain.ContactSummary
import app.lastcalled.domain.RelativeTime
import app.lastcalled.ui.CallTrackerViewModel
import app.lastcalled.ui.theme.Border
import java.util.Date
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    viewModel: CallTrackerViewModel,
    initialPerson: ContactSummary?,
    onBack: () -> Unit,
) {
    val contact by viewModel.contact.collectAsStateWithLifecycle()
    val history by viewModel.callHistory.collectAsStateWithLifecycle()
    val latestOutgoing by viewModel.latestOutgoing.collectAsStateWithLifecycle()
    var selectedSource by remember { mutableStateOf("All sources") }
    var selectedDirection by remember { mutableStateOf("All directions") }
    var selectedMedia by remember { mutableStateOf("All types") }

    val name = contact?.displayName ?: initialPerson?.displayName ?: "Unknown caller"
    val phoneNumber = contact?.phoneNumber
        ?: initialPerson?.phoneNumber
        ?: history.firstOrNull()?.phoneNumber
    val filteredCalls = remember(history, selectedSource, selectedDirection, selectedMedia) {
        history.filter { record ->
            (selectedSource == "All sources" || record.source == selectedSource.uppercase()) &&
                (selectedDirection == "All directions" ||
                    record.direction == selectedDirection.uppercase().replace(' ', '_')) &&
                (selectedMedia == "All types" || record.callType == selectedMedia.uppercase())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Person details", style = MaterialTheme.typography.titleLarge)
                        Text(
                            name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                PersonHeader(
                    name = name,
                    photoUri = contact?.photoUri ?: initialPerson?.photoUri,
                    phoneNumber = phoneNumber,
                    isAvailable = contact?.isAvailable ?: initialPerson?.isAvailable ?: true,
                    isUnknown = contact?.isUnknown ?: initialPerson?.isUnknown ?: true,
                    latestOutgoing = latestOutgoing,
                    callCount = initialPerson?.totalCallCount ?: history.size,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        text = "Call history",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        HistoryFilter(
                            label = selectedSource,
                            options = listOf("All sources", "Phone", "WhatsApp", "Unknown"),
                            onSelected = { selectedSource = it },
                        )
                        HistoryFilter(
                            label = selectedDirection,
                            options = listOf(
                                "All directions",
                                "Outgoing",
                                "Incoming",
                                "Missed",
                                "Rejected",
                                "Voicemail",
                                "Answered externally",
                                "Screened",
                            ),
                            onSelected = { selectedDirection = it },
                        )
                        HistoryFilter(
                            label = selectedMedia,
                            options = listOf("All types", "Voice", "Video", "Unknown"),
                            onSelected = { selectedMedia = it },
                        )
                    }
                }
            }
            if (filteredCalls.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        border = BorderStroke(1.dp, Border),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            text = if (history.isEmpty()) "No call history available for this person."
                            else "No calls match these filters.",
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(filteredCalls, key = CallRecordEntity::systemCallLogId) { call ->
                    CallHistoryRow(call)
                }
            }
            if (history.size >= 250) {
                item {
                    Text(
                        text = "Showing the 250 most recent calls available in this app.",
                        modifier = Modifier.padding(horizontal = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonHeader(
    name: String,
    photoUri: String?,
    phoneNumber: String?,
    isAvailable: Boolean,
    isUnknown: Boolean,
    latestOutgoing: CallRecordEntity?,
    callCount: Int,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ContactAvatar(name = name, photoUri = photoUri, size = 76)
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isUnknown) "Unknown caller" else name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (!isAvailable && !isUnknown) {
                Text(
                    text = "This contact is no longer in your address book.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            phoneNumber?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "LAST OUTGOING CALL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = latestOutgoing?.let { RelativeTime.format(it.timestamp) }
                            ?: "Never called",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (latestOutgoing != null) {
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = "${sourceName(latestOutgoing.source)} · ${mediaName(latestOutgoing.callType)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = formatCallDate(latestOutgoing.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Duration ${formatDuration(latestOutgoing.durationSeconds)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "$callCount calls saved on this device",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CallHistoryRow(call: CallRecordEntity) {
    val direction = call.direction
    val title = when (direction) {
        CallDirection.OUTGOING.name -> "Outgoing"
        CallDirection.INCOMING.name -> "Incoming"
        CallDirection.MISSED.name -> "Missed"
        CallDirection.REJECTED.name -> "Rejected"
        CallDirection.VOICEMAIL.name -> "Voicemail"
        CallDirection.ANSWERED_EXTERNALLY.name -> "Answered elsewhere"
        CallDirection.SCREENED.name -> "Screened"
        else -> "Direction unknown"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (call.callType == "VIDEO") Icons.Default.Videocam
                else Icons.Default.Call,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(
                    "${sourceName(call.source)} · ${mediaName(call.callType)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatCallDate(call.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatDuration(call.durationSeconds),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryFilter(
    label: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.material3.SuggestionChip(
        onClick = { expanded = true },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FilterList, contentDescription = null)
                Text(" $label", maxLines = 1)
            }
        },
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                    onSelected(option)
                    expanded = false
                },
            )
        }
    }
}

private fun sourceName(source: String): String = when (source) {
    "PHONE" -> "Phone"
    "WHATSAPP" -> "WhatsApp"
    else -> "Unknown source"
}

private fun mediaName(type: String): String = when (type) {
    "VOICE" -> "Voice call"
    "VIDEO" -> "Video call"
    else -> "Call type unknown"
}
