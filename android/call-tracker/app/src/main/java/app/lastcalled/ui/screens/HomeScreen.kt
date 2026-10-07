package app.lastcalled.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import app.lastcalled.domain.CallSource
import app.lastcalled.domain.ContactSummary
import app.lastcalled.domain.RelativeTime
import app.lastcalled.ui.CallTrackerViewModel
import app.lastcalled.ui.SortOrder
import app.lastcalled.ui.theme.Border
import app.lastcalled.ui.theme.Forest
import app.lastcalled.ui.theme.Sage
import app.lastcalled.ui.theme.WarmWhite
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CallTrackerViewModel,
    hasContactsAccess: Boolean,
    onOpenSettings: () -> Unit,
    onOpenPerson: (ContactSummary) -> Unit,
    onManagePermissions: () -> Unit,
) {
    val people by viewModel.people.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedThreshold by viewModel.selectedThresholdDays.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSortOrder.collectAsStateWithLifecycle()
    var searchOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val activeThreshold = selectedThreshold ?: settings.defaultThresholdDays

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Last Called", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "A little nudge to stay in touch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) viewModel.updateSearch("")
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (searchOpen) "Close search" else "Search people",
                        )
                    }
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !refreshing,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh call history")
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 8.dp,
                bottom = 30.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (searchOpen) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::updateSearch,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Find a person") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            }

            if (!hasContactsAccess) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        onClick = onManagePermissions,
                    ) {
                        Text(
                            text = "Contacts access is off. Calls are still tracked, but names may appear as unknown. Manage access",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (notice != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Text(
                            text = notice.orEmpty(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            item {
                CallFocusCard(
                    overdueCount = if (activeThreshold < 0) people.size else people.count {
                        it.latestOutgoingAt == null ||
                            it.latestOutgoingAt < System.currentTimeMillis() -
                            activeThreshold * DAY_MILLIS
                    },
                    thresholdDays = activeThreshold,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            Text(
                                text = "Your people",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (people.isEmpty()) "No one in this view yet"
                                else "${people.size} ${if (people.size == 1) "person" else "people"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        androidx.compose.material3.TextButton(onClick = {
                            sortMenuOpen = true
                        }) {
                            Text(
                                when (selectedSort) {
                                    SortOrder.LONGEST -> "Longest ago"
                                    SortOrder.RECENT -> "Recent"
                                    SortOrder.ALPHABETICAL -> "A–Z"
                                },
                            )
                            SortMenu(
                                expanded = sortMenuOpen,
                                onDismiss = { sortMenuOpen = false },
                                onSelect = { order ->
                                    viewModel.selectSortOrder(order)
                                    sortMenuOpen = false
                                },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(3, 7, 14, 30, -1).forEach { days ->
                            FilterChip(
                                selected = activeThreshold == days,
                                onClick = { viewModel.selectThreshold(days) },
                                label = {
                                    Text(if (days < 0) "Everyone" else "${days}d+")
                                },
                            )
                        }
                    }
                }
            }

            if (people.isEmpty()) {
                item {
                    EmptyPeopleState(
                        searchActive = query.isNotBlank(),
                        onRefresh = viewModel::refresh,
                    )
                }
            } else {
                items(people, key = ContactSummary::contactId) { person ->
                    PersonRow(person = person, onClick = { onOpenPerson(person) })
                }
            }
        }
    }
}

@Composable
private fun CallFocusCard(overdueCount: Int, thresholdDays: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Forest),
        shape = RoundedCornerShape(25.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 21.dp, vertical = 20.dp)) {
            Surface(
                color = WarmWhite.copy(alpha = 0.14f),
                shape = CircleShape,
            ) {
                Text(
                    text = "YOUR CHECK-IN LIST",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    color = WarmWhite,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = when {
                    thresholdDays < 0 -> "$overdueCount ${if (overdueCount == 1) "person" else "people"} in your list"
                    overdueCount == 0 -> "You’re all caught up."
                    else -> "$overdueCount ${if (overdueCount == 1) "person" else "people"} to check in with"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = WarmWhite,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = if (thresholdDays < 0) "Showing everyone from your contacts and call history."
                else "Showing people you haven’t called in ${thresholdDays} days or more.",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmWhite.copy(alpha = 0.82f),
            )
        }
    }
}

@Composable
private fun PersonRow(person: ContactSummary, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            ContactAvatar(name = person.displayName, photoUri = person.photoUri, size = 48)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = person.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!person.isAvailable && !person.isUnknown) {
                        Text(
                            text = " · removed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = person.latestOutgoingAt?.let { RelativeTime.format(it) } ?: "Never called",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (person.latestOutgoingAt == null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (person.latestOutgoingAt == null) FontWeight.Medium
                    else FontWeight.Normal,
                )
                if (person.latestOutgoingAt != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${sourceLabel(person.latestOutgoingSource)} · ${mediaLabel(person.latestOutgoingType)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (!person.phoneNumber.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = person.phoneNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View call history",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun EmptyPeopleState(searchActive: Boolean, onRefresh: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (searchActive) "No matching people" else "No people to show yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = if (searchActive) "Try another name or phone number."
                else "Refresh to import the call history and contacts Android makes available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!searchActive) {
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.OutlinedButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(" Refresh now")
                }
            }
        }
    }
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (SortOrder) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Longest since a call") },
            onClick = { onSelect(SortOrder.LONGEST) },
        )
        DropdownMenuItem(
            text = { Text("Recently called") },
            onClick = { onSelect(SortOrder.RECENT) },
        )
        DropdownMenuItem(
            text = { Text("Alphabetical") },
            onClick = { onSelect(SortOrder.ALPHABETICAL) },
        )
    }
}

private fun sourceLabel(source: String?): String = when (source) {
    CallSource.PHONE.name -> "Phone"
    CallSource.WHATSAPP.name -> "WhatsApp"
    else -> "Unknown source"
}

private fun mediaLabel(type: String?): String = when (type) {
    "VIDEO" -> "Video"
    "VOICE" -> "Voice"
    else -> "Call type unknown"
}

private const val DAY_MILLIS = 86_400_000L
