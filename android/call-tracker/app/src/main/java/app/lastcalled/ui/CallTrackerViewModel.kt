package app.lastcalled.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lastcalled.CallTrackerApplication
import app.lastcalled.data.CallRepository
import app.lastcalled.data.RefreshOutcome
import app.lastcalled.data.local.AppSettingsEntity
import app.lastcalled.data.local.CallRecordEntity
import app.lastcalled.data.local.ContactEntity
import app.lastcalled.domain.ContactSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CallTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CallRepository =
        (application as CallTrackerApplication).repository

    private val query = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(SortOrder.LONGEST)
    private val selectedThreshold = MutableStateFlow<Int?>(null)
    private val selectedContact = MutableStateFlow<String?>(null)
    private val _refreshing = MutableStateFlow(false)
    private val _notice = MutableStateFlow<String?>(null)
    private var refreshJob: Job? = null

    val settings: StateFlow<AppSettingsEntity> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsEntity())
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    val notice: StateFlow<String?> = _notice.asStateFlow()
    val searchQuery: StateFlow<String> = query.asStateFlow()
    val selectedSortOrder: StateFlow<SortOrder> = sortOrder.asStateFlow()
    val selectedThresholdDays: StateFlow<Int?> = selectedThreshold.asStateFlow()

    val people: StateFlow<List<ContactSummary>> = combine(
        query,
        sortOrder,
        selectedThreshold,
        settings,
    ) { search, sorting, chosenThreshold, appSettings ->
        val threshold = chosenThreshold ?: appSettings.defaultThresholdDays
        val cutoff = if (threshold < 0) 0L
        else System.currentTimeMillis() - threshold * MILLIS_PER_DAY
        PeopleQuery(search.trim(), sorting.name, cutoff)
    }.flatMapLatest { input ->
        repository.observePeople(input.search, input.cutoffMillis, input.sortOrder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val callHistory: StateFlow<List<CallRecordEntity>> = selectedContact
        .flatMapLatest { contactId ->
            if (contactId == null) flowOf(emptyList())
            else repository.observeHistory(contactId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val contact: StateFlow<ContactEntity?> = selectedContact
        .flatMapLatest { contactId ->
            if (contactId == null) flowOf(null)
            else repository.observeContact(contactId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val latestOutgoing: StateFlow<CallRecordEntity?> = selectedContact
        .flatMapLatest { contactId ->
            if (contactId == null) flowOf(null)
            else repository.observeLatestOutgoing(contactId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateSearch(value: String) {
        query.value = value
    }

    fun selectThreshold(days: Int?) {
        selectedThreshold.value = days
    }

    fun selectSortOrder(value: SortOrder) {
        sortOrder.value = value
    }

    fun openContact(contactId: String) {
        selectedContact.value = contactId
    }

    fun closeContact() {
        selectedContact.value = null
    }

    fun onAppResumed() {
        viewModelScope.launch {
            if (repository.getSettings().refreshOnForeground) refresh()
        }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _refreshing.value = true
            _notice.value = null
            try {
                _notice.value = when (val result = repository.refresh()) {
                    is RefreshOutcome.Complete ->
                        when {
                            result.contactsReadFailed ->
                                "Updated ${result.callCount} calls. Contacts could not be read; previous local matches were kept."
                            !result.contactsRefreshed ->
                                "Updated ${result.callCount} calls. Contacts access is off; previous local matches were kept."
                            else ->
                                "Updated ${result.callCount} calls and ${result.contactCount} contacts."
                        }
                    RefreshOutcome.CallLogPermissionMissing ->
                        "Call history access is required to import calls."
                    RefreshOutcome.CallLogReadFailed ->
                        "The device call history could not be read. Try again."
                }
            } catch (_: Exception) {
                _notice.value = "The local import did not finish. Try refreshing again."
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun updateDefaultThreshold(days: Int) {
        viewModelScope.launch {
            try {
                repository.updateDefaultThreshold(days)
                selectedThreshold.value = null
            } catch (_: Exception) {
                _notice.value = "The default time filter could not be saved."
            }
        }
    }

    fun updateRefreshOnForeground(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateRefreshOnForeground(enabled)
            } catch (_: Exception) {
                _notice.value = "The refresh preference could not be saved."
            }
        }
    }

    fun resetImportedCalls() {
        viewModelScope.launch {
            try {
                repository.resetImportedCalls()
                _notice.value = "Imported call records were reset. Device call history was not changed."
            } catch (_: Exception) {
                _notice.value = "The imported call records could not be reset."
            }
        }
    }

    fun clearLocalData() {
        viewModelScope.launch {
            try {
                repository.clearLocalData()
                selectedContact.value = null
                _notice.value = "This app's local data was cleared. Device call history was not changed."
            } catch (_: Exception) {
                _notice.value = "This app's local data could not be cleared."
            }
        }
    }

    private data class PeopleQuery(
        val search: String,
        val sortOrder: String,
        val cutoffMillis: Long,
    )

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}

enum class SortOrder {
    LONGEST,
    RECENT,
    ALPHABETICAL,
}
