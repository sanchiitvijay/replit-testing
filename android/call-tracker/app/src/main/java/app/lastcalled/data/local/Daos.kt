package app.lastcalled.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import app.lastcalled.domain.ContactSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Upsert
    suspend fun upsertContacts(contacts: List<ContactEntity>)

    @Upsert
    suspend fun upsertPhones(phones: List<ContactPhoneEntity>)

    @Query("UPDATE contacts SET isAvailable = 0 WHERE isUnknown = 0")
    suspend fun markImportedContactsUnavailable()

    @Query("DELETE FROM contact_phones")
    suspend fun clearPhoneMappings()

    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts(): List<ContactEntity>

    @Query("SELECT * FROM contact_phones")
    suspend fun getAllPhones(): List<ContactPhoneEntity>

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    fun observeContact(contactId: String): Flow<ContactEntity?>

    @Query(
        """
        SELECT
            c.id AS contactId,
            c.displayName AS displayName,
            c.phoneNumber AS phoneNumber,
            c.photoUri AS photoUri,
            c.isUnknown AS isUnknown,
            c.isAvailable AS isAvailable,
            (
                SELECT MAX(r.timestamp)
                FROM call_records r
                WHERE r.contactId = c.id AND r.direction = 'OUTGOING'
            ) AS latestOutgoingAt,
            (
                SELECT r.source
                FROM call_records r
                WHERE r.contactId = c.id AND r.direction = 'OUTGOING'
                ORDER BY r.timestamp DESC
                LIMIT 1
            ) AS latestOutgoingSource,
            (
                SELECT r.callType
                FROM call_records r
                WHERE r.contactId = c.id AND r.direction = 'OUTGOING'
                ORDER BY r.timestamp DESC
                LIMIT 1
            ) AS latestOutgoingType,
            (
                SELECT r.durationSeconds
                FROM call_records r
                WHERE r.contactId = c.id AND r.direction = 'OUTGOING'
                ORDER BY r.timestamp DESC
                LIMIT 1
            ) AS latestOutgoingDurationSeconds,
            COUNT(r.systemCallLogId) AS totalCallCount
        FROM contacts c
        LEFT JOIN call_records r ON r.contactId = c.id
        WHERE
            c.isUnknown = 1
            OR EXISTS (SELECT 1 FROM contact_phones p WHERE p.contactId = c.id)
            OR EXISTS (SELECT 1 FROM call_records oldCall WHERE oldCall.contactId = c.id)
        GROUP BY c.id
        HAVING
            (
                :query = ''
                OR c.displayName LIKE '%' || :query || '%'
                OR COALESCE(c.phoneNumber, '') LIKE '%' || :query || '%'
                OR (
                    :numberQuery != ''
                    AND (
                        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(c.phoneNumber, ''), ' ', ''), '-', ''), '(', ''), ')', ''), '+', '')
                            LIKE '%' || :numberQuery || '%'
                        OR EXISTS (
                            SELECT 1 FROM contact_phones p
                            WHERE p.contactId = c.id
                                AND (
                                    p.number LIKE '%' || :query || '%'
                                    OR p.normalizedNumber LIKE '%' || :numberQuery || '%'
                                )
                        )
                    )
                )
            )
            AND (
                :cutoffMillis = 0
                OR MAX(CASE WHEN r.direction = 'OUTGOING' THEN r.timestamp END) IS NULL
                OR MAX(CASE WHEN r.direction = 'OUTGOING' THEN r.timestamp END) <= :cutoffMillis
            )
        ORDER BY
            CASE WHEN :sortOrder = 'LONGEST' AND latestOutgoingAt IS NULL THEN 0 ELSE 1 END ASC,
            CASE WHEN :sortOrder = 'RECENT' THEN latestOutgoingAt END DESC,
            CASE WHEN :sortOrder = 'LONGEST' THEN latestOutgoingAt END ASC,
            c.displayName COLLATE NOCASE ASC
        """,
    )
    fun observeSummaries(
        query: String,
        numberQuery: String,
        cutoffMillis: Long,
        sortOrder: String,
    ): Flow<List<ContactSummary>>
}

@Dao
interface CallRecordDao {
    @Upsert
    suspend fun upsertAll(records: List<CallRecordEntity>)

    @Query("SELECT * FROM call_records WHERE systemCallLogId IN (:systemIds)")
    suspend fun findExisting(systemIds: List<Long>): List<CallRecordEntity>

    @Query(
        """
        SELECT * FROM call_records
        WHERE contactId = :contactId
        ORDER BY timestamp DESC
        LIMIT :limit
        """,
    )
    fun observeHistory(contactId: String, limit: Int = 250): Flow<List<CallRecordEntity>>

    @Query(
        """
        SELECT * FROM call_records
        WHERE contactId = :contactId AND direction = 'OUTGOING'
        ORDER BY timestamp DESC
        LIMIT 1
        """,
    )
    fun observeLatestOutgoing(contactId: String): Flow<CallRecordEntity?>

    @Query("DELETE FROM call_records")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM call_records")
    suspend fun countAll(): Int

    @Query("SELECT * FROM call_records WHERE systemCallLogId = :systemId")
    suspend fun findBySystemId(systemId: Long): CallRecordEntity?
}

@Dao
interface AppSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: AppSettingsEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?
}
