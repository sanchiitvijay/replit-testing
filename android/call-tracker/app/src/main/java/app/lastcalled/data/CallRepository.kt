package app.lastcalled.data

import android.content.Context
import android.os.Build
import androidx.room.withTransaction
import app.lastcalled.data.local.AppSettingsEntity
import app.lastcalled.data.local.CallRecordEntity
import app.lastcalled.data.local.CallTrackerDatabase
import app.lastcalled.data.local.ContactEntity
import app.lastcalled.data.local.ContactPhoneEntity
import app.lastcalled.data.platform.CallLogDataSource
import app.lastcalled.data.platform.ContactsDataSource
import app.lastcalled.domain.CallClassification
import app.lastcalled.domain.CallDirection
import app.lastcalled.domain.CallMediaType
import app.lastcalled.domain.CallSource
import app.lastcalled.domain.ContactSummary
import app.lastcalled.domain.ImportedContact
import app.lastcalled.domain.RawCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface RefreshOutcome {
    data class Complete(
        val callCount: Int,
        val contactCount: Int,
        val contactsRefreshed: Boolean,
        val contactsReadFailed: Boolean,
    ) : RefreshOutcome
    data object CallLogPermissionMissing : RefreshOutcome
    data object CallLogReadFailed : RefreshOutcome
}

class CallRepository(
    private val database: CallTrackerDatabase,
    private val callLogDataSource: CallLogDataSource,
    private val contactsDataSource: ContactsDataSource,
    private val context: Context,
) {
    private val contactDao = database.contactDao()
    private val callDao = database.callRecordDao()
    private val settingsDao = database.appSettingsDao()
    private val phoneMatcher = PhoneNumberMatcher(context)

    fun observeSettings(): Flow<AppSettingsEntity> = settingsDao.observe().map { setting ->
        setting ?: AppSettingsEntity()
    }

    fun observePeople(
        query: String,
        cutoffMillis: Long,
        sortOrder: String,
    ): Flow<List<ContactSummary>> = contactDao.observeSummaries(
        query = query,
        numberQuery = query.filter(Char::isDigit),
        cutoffMillis = cutoffMillis,
        sortOrder = sortOrder,
    )

    fun observeHistory(contactId: String): Flow<List<CallRecordEntity>> =
        callDao.observeHistory(contactId)

    fun observeLatestOutgoing(contactId: String): Flow<CallRecordEntity?> =
        callDao.observeLatestOutgoing(contactId)

    fun observeContact(contactId: String): Flow<ContactEntity?> =
        contactDao.observeContact(contactId)

    suspend fun getSettings(): AppSettingsEntity = settingsDao.get() ?: AppSettingsEntity()

    suspend fun updateDefaultThreshold(days: Int) {
        database.withTransaction {
            val current = settingsDao.get() ?: AppSettingsEntity()
            settingsDao.save(current.copy(defaultThresholdDays = days))
        }
    }

    suspend fun updateRefreshOnForeground(enabled: Boolean) {
        database.withTransaction {
            val current = settingsDao.get() ?: AppSettingsEntity()
            settingsDao.save(current.copy(refreshOnForeground = enabled))
        }
    }

    suspend fun refresh(): RefreshOutcome = withContext(Dispatchers.IO) {
        val rawCalls = try {
            callLogDataSource.readLatest()
        } catch (_: SecurityException) {
            return@withContext RefreshOutcome.CallLogPermissionMissing
        } catch (_: Exception) {
            return@withContext RefreshOutcome.CallLogReadFailed
        }

        var contactsReadFailed = false
        val importedContacts = try {
            contactsDataSource.readWithPhoneNumbers()
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            contactsReadFailed = true
            null
        }

        val now = System.currentTimeMillis()
        val result = database.withTransaction {
            if (importedContacts != null) {
                val previousContacts = contactDao.getAllContacts().associateBy(ContactEntity::id)
                contactDao.markImportedContactsUnavailable()
                contactDao.clearPhoneMappings()
                val contactEntities = importedContacts.map { contact ->
                    ContactEntity(
                        id = contact.id,
                        displayName = contact.displayName,
                        phoneNumber = contact.phoneNumbers.firstOrNull(),
                        photoUri = contact.photoUri,
                        isUnknown = false,
                        isAvailable = true,
                        createdAt = previousContacts[contact.id]?.createdAt ?: now,
                        updatedAt = now,
                    )
                }
                contactDao.upsertContacts(contactEntities)
                contactDao.upsertPhones(importedContacts.flatMap(::toPhoneEntities))
            }

            val existingRecords = rawCalls
                .map(RawCall::systemId)
                .distinct()
                .chunked(MAX_SQL_BINDS)
                .flatMap { ids -> callDao.findExisting(ids) }
                .associateBy(CallRecordEntity::systemCallLogId)

            val contacts = contactDao.getAllContacts()
            val candidates = phoneMatcher.candidates(contacts, contactDao.getAllPhones())
            val contactById = contacts.associateBy(ContactEntity::id)
            val unknownContacts = linkedMapOf<String, ContactEntity>()
            val matchCache = mutableMapOf<Pair<String, String?>, PhoneCandidate?>()

            val records = rawCalls.map { raw ->
                val previous = existingRecords[raw.systemId]
                val cacheKey = raw.number.orEmpty() to raw.countryIso
                val matched = if (matchCache.containsKey(cacheKey)) {
                    matchCache[cacheKey]
                } else {
                    phoneMatcher.match(raw.number, raw.countryIso, candidates).also {
                        matchCache[cacheKey] = it
                    }
                }
                val previousContact = previous?.let { contactById[it.contactId] }
                val preserveKnownAssociation = previous != null &&
                    previousContact?.isUnknown == false
                val contactId = if (preserveKnownAssociation) {
                    previous!!.contactId
                } else {
                    matched?.contactId ?: previous?.contactId
                    ?: phoneMatcher.stableUnknownKey(raw.number, raw.countryIso, raw.systemId)
                }
                val currentContact = contactById[contactId]
                val fallbackName = previous?.contactNameSnapshot?.takeIf(String::isNotBlank)
                    ?: raw.number?.takeIf(String::isNotBlank)
                    ?: "Unknown caller"
                val contactName = if (preserveKnownAssociation) {
                    previous!!.contactNameSnapshot
                } else matched?.displayName
                    ?: currentContact?.displayName
                    ?: fallbackName

                if (matched == null && currentContact == null) {
                    unknownContacts[contactId] = ContactEntity(
                        id = contactId,
                        displayName = "Unknown caller",
                        phoneNumber = raw.number,
                        photoUri = null,
                        isUnknown = true,
                        isAvailable = true,
                        createdAt = previous?.createdAt ?: now,
                        updatedAt = now,
                    )
                }

                val direction = CallClassification.direction(raw.systemType)
                val unifiedHistoryAvailable = Build.VERSION.SDK_INT >= 36
                CallRecordEntity(
                    systemCallLogId = raw.systemId,
                    contactId = contactId,
                    contactNameSnapshot = contactName,
                    phoneNumber = raw.number ?: previous?.phoneNumber,
                    timestamp = raw.timestamp,
                    durationSeconds = raw.durationSeconds,
                    direction = direction.name,
                    source = CallClassification.source(
                        raw.accountComponent,
                        unifiedHistoryAvailable,
                    ).name,
                    callType = CallClassification.mediaType(raw.features).name,
                    accountComponent = raw.accountComponent,
                    createdAt = previous?.createdAt ?: now,
                    updatedAt = now,
                )
            }

            contactDao.upsertContacts(unknownContacts.values.toList())
            callDao.upsertAll(records)
            settingsDao.save(getSettings().copy(lastSuccessfulRefreshAt = now))
            records.size to (importedContacts?.size ?: contacts.count { !it.isUnknown })
        }

        RefreshOutcome.Complete(
            callCount = result.first,
            contactCount = result.second,
            contactsRefreshed = importedContacts != null,
            contactsReadFailed = contactsReadFailed,
        )
    }

    suspend fun resetImportedCalls() = withContext(Dispatchers.IO) {
        database.withTransaction {
            callDao.deleteAll()
            database.openHelper.writableDatabase.execSQL(
                "DELETE FROM contacts WHERE isUnknown = 1",
            )
        }
    }

    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        settingsDao.save(AppSettingsEntity())
    }

    private fun toPhoneEntities(contact: ImportedContact): List<ContactPhoneEntity> =
        contact.phoneNumbers.map { number ->
            ContactPhoneEntity(
                contactId = contact.id,
                number = number,
                normalizedNumber = phoneMatcher.normalize(number),
            )
        }

    private companion object {
        const val MAX_SQL_BINDS = 800
    }
}
