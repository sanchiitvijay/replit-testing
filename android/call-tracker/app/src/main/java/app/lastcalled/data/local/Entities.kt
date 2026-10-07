package app.lastcalled.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    indices = [Index(value = ["isUnknown"]), Index(value = ["displayName"])],
)
data class ContactEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val phoneNumber: String?,
    val photoUri: String?,
    val isUnknown: Boolean,
    val isAvailable: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "contact_phones",
    primaryKeys = ["contactId", "number"],
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("normalizedNumber"), Index("contactId")],
)
data class ContactPhoneEntity(
    val contactId: String,
    val number: String,
    val normalizedNumber: String,
)

@Entity(
    tableName = "call_records",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("contactId"),
        Index(value = ["contactId", "timestamp"]),
        Index(value = ["contactId", "direction", "timestamp"]),
    ],
)
data class CallRecordEntity(
    @PrimaryKey val systemCallLogId: Long,
    val contactId: String,
    val contactNameSnapshot: String,
    val phoneNumber: String?,
    val timestamp: Long,
    val durationSeconds: Long,
    val direction: String,
    val source: String,
    val callType: String,
    val accountComponent: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val defaultThresholdDays: Int = 7,
    val refreshOnForeground: Boolean = true,
    val lastSuccessfulRefreshAt: Long? = null,
)
