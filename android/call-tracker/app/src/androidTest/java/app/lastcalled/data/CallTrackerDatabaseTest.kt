package app.lastcalled.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.lastcalled.data.local.CallRecordEntity
import app.lastcalled.data.local.CallTrackerDatabase
import app.lastcalled.data.local.ContactEntity
import app.lastcalled.data.local.ContactPhoneEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallTrackerDatabaseTest {
    private val database = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        CallTrackerDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun systemCallLogIdPreventsDuplicateImport() = runBlocking {
        database.contactDao().upsertContacts(listOf(contact("contact:1", "Anika")))
        val original = call(id = 42, contactId = "contact:1", timestamp = 1_000)
        database.callRecordDao().upsertAll(listOf(original))
        database.callRecordDao().upsertAll(
            listOf(original.copy(durationSeconds = 120, updatedAt = 2_000)),
        )

        assertEquals(1, database.callRecordDao().countAll())
        assertEquals(120L, database.callRecordDao().findBySystemId(42)?.durationSeconds)
    }

    @Test
    fun longestSinceCallSortPutsNeverCalledPeopleFirst() = runBlocking {
        database.contactDao().upsertContacts(
            listOf(contact("contact:1", "Anika"), contact("contact:2", "Bea")),
        )
        database.contactDao().upsertPhones(
            listOf(
                ContactPhoneEntity("contact:1", "+919876543210", "+919876543210"),
                ContactPhoneEntity("contact:2", "+919812345678", "+919812345678"),
            ),
        )
        database.callRecordDao().upsertAll(
            listOf(call(id = 1, contactId = "contact:1", timestamp = 10_000)),
        )

        val summaries = database.contactDao()
            .observeSummaries(query = "", cutoffMillis = 0, sortOrder = "LONGEST")
            .first()

        assertEquals("contact:2", summaries.first().contactId)
        assertNull(summaries.first().latestOutgoingAt)
        assertEquals("contact:1", summaries.last().contactId)
    }

    @Test
    fun outgoingFilterIgnoresIncomingCallsAndSupportsSearchAndSorting() = runBlocking {
        database.contactDao().upsertContacts(
            listOf(
                contact("contact:a", "Alice"),
                contact("contact:b", "Bob"),
                contact("contact:c", "No Calls"),
            ),
        )
        database.contactDao().upsertPhones(
            listOf(
                ContactPhoneEntity("contact:a", "+12025550101", "+12025550101"),
                ContactPhoneEntity("contact:b", "+12025550102", "+12025550102"),
                ContactPhoneEntity("contact:c", "+12025550103", "+12025550103"),
            ),
        )
        database.callRecordDao().upsertAll(
            listOf(
                call(id = 1, contactId = "contact:a", timestamp = 100)
                    .copy(direction = "OUTGOING"),
                call(id = 2, contactId = "contact:a", timestamp = 900)
                    .copy(direction = "INCOMING"),
                call(id = 3, contactId = "contact:b", timestamp = 500)
                    .copy(direction = "OUTGOING"),
            ),
        )
        val dao = database.contactDao()

        val pastCutoff = dao.observeSummaries("", 200, "LONGEST").first()
        assertEquals(listOf("contact:c", "contact:a"), pastCutoff.map { it.contactId })

        val searchResult = dao.observeSummaries("ali", 0, "ALPHABETICAL").first()
        assertEquals(listOf("contact:a"), searchResult.map { it.contactId })

        val phoneSearchResult = dao.observeSummaries(
            query = "2025550102",
            numberQuery = "2025550102",
            cutoffMillis = 0,
            sortOrder = "ALPHABETICAL",
        ).first()
        assertEquals(listOf("contact:b"), phoneSearchResult.map { it.contactId })

        val recentFirst = dao.observeSummaries("", 0, "RECENT").first()
        assertEquals(
            listOf("contact:b", "contact:a", "contact:c"),
            recentFirst.map { it.contactId },
        )
    }

    private fun contact(id: String, name: String) = ContactEntity(
        id = id,
        displayName = name,
        phoneNumber = null,
        photoUri = null,
        isUnknown = false,
        isAvailable = true,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun call(id: Long, contactId: String, timestamp: Long) = CallRecordEntity(
        systemCallLogId = id,
        contactId = contactId,
        contactNameSnapshot = contactId,
        phoneNumber = null,
        timestamp = timestamp,
        durationSeconds = 30,
        direction = "OUTGOING",
        source = "PHONE",
        callType = "VOICE",
        accountComponent = null,
        createdAt = 0,
        updatedAt = 0,
    )
}
