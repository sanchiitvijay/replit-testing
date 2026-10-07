package app.lastcalled.domain

enum class CallDirection {
    OUTGOING,
    INCOMING,
    MISSED,
    REJECTED,
    VOICEMAIL,
    ANSWERED_EXTERNALLY,
    SCREENED,
    UNKNOWN,
}

enum class CallSource {
    PHONE,
    WHATSAPP,
    UNKNOWN,
}

enum class CallMediaType {
    VOICE,
    VIDEO,
    UNKNOWN,
}

data class ImportedContact(
    val id: String,
    val displayName: String,
    val photoUri: String?,
    val phoneNumbers: List<String>,
)

data class RawCall(
    val systemId: Long,
    val number: String?,
    val timestamp: Long,
    val durationSeconds: Long,
    val systemType: Int,
    val features: Int?,
    val accountComponent: String?,
    val countryIso: String?,
)

data class ContactSummary(
    val contactId: String,
    val displayName: String,
    val phoneNumber: String?,
    val photoUri: String?,
    val isUnknown: Boolean,
    val isAvailable: Boolean,
    val latestOutgoingAt: Long?,
    val latestOutgoingSource: String?,
    val latestOutgoingType: String?,
    val latestOutgoingDurationSeconds: Long?,
    val totalCallCount: Int,
)

object CallClassification {
    fun direction(systemType: Int): CallDirection = when (systemType) {
        1 -> CallDirection.INCOMING
        2 -> CallDirection.OUTGOING
        3 -> CallDirection.MISSED
        4 -> CallDirection.VOICEMAIL
        5, 6 -> CallDirection.REJECTED
        7 -> CallDirection.ANSWERED_EXTERNALLY
        8 -> CallDirection.SCREENED
        else -> CallDirection.UNKNOWN
    }

    fun source(accountComponent: String?, unifiedHistoryAvailable: Boolean): CallSource {
        val packageName = accountComponent
            ?.substringBefore('/')
            ?.takeIf(String::isNotBlank)

        return when (packageName) {
            "com.whatsapp", "com.whatsapp.w4b" -> CallSource.WHATSAPP
            "com.android.phone" -> CallSource.PHONE
            null -> if (unifiedHistoryAvailable) CallSource.UNKNOWN else CallSource.PHONE
            else -> CallSource.UNKNOWN
        }
    }

    fun mediaType(features: Int?): CallMediaType {
        if (features == null) return CallMediaType.UNKNOWN
        return if (features and VIDEO_FEATURE_FLAG != 0) {
            CallMediaType.VIDEO
        } else {
            CallMediaType.VOICE
        }
    }

    private const val VIDEO_FEATURE_FLAG = 1
}

object RelativeTime {
    fun format(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val elapsedSeconds = ((nowMillis - timestampMillis).coerceAtLeast(0L)) / 1_000L
        if (elapsedSeconds < 60) return "Just now"

        val minutes = elapsedSeconds / 60
        if (minutes < 60) return quantity(minutes, "minute")

        val hours = elapsedSeconds / 3_600
        if (hours < 24) return quantity(hours, "hour")

        val days = elapsedSeconds / 86_400
        return quantity(days, "day")
    }

    private fun quantity(value: Long, unit: String): String =
        "$value $unit${if (value == 1L) "" else "s"} ago"
}
