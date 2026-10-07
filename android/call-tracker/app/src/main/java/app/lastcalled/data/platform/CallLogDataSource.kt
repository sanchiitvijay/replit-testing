package app.lastcalled.data.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.provider.CallLog
import androidx.core.content.ContextCompat
import app.lastcalled.domain.RawCall

class CallLogDataSource(private val context: Context) {
    fun readLatest(): List<RawCall> {
        checkPermission()

        val projection = buildList {
            add(CallLog.Calls._ID)
            add(CallLog.Calls.NUMBER)
            add(CallLog.Calls.DATE)
            add(CallLog.Calls.DURATION)
            add(CallLog.Calls.TYPE)
            add(CallLog.Calls.FEATURES)
            add(CallLog.Calls.COUNTRY_ISO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)
            }
        }.toTypedArray()

        val uri = when {
            Build.VERSION.SDK_INT >= 37 -> CallLog.Calls.CONTENT_URI_WITH_VOIP_CALLS
            Build.VERSION.SDK_INT >= 36 -> CallLog.Calls.CONTENT_URI.buildUpon()
                .appendQueryParameter("include_voip_calls", "true")
                .build()
            else -> CallLog.Calls.CONTENT_URI
        }.buildUpon()
            .appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, MAX_IMPORT_SIZE.toString())
            .build()

        val result = mutableListOf<RawCall>()
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                result += cursor.toRawCall()
            }
        }
        return result
    }

    private fun Cursor.toRawCall(): RawCall {
        val componentColumn = getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)
        val featureColumn = getColumnIndex(CallLog.Calls.FEATURES)
        val number = getString(getColumnIndexOrThrow(CallLog.Calls.NUMBER))
            ?.takeIf(String::isNotBlank)
        val countryIso = getString(getColumnIndexOrThrow(CallLog.Calls.COUNTRY_ISO))
            ?.takeIf(String::isNotBlank)

        return RawCall(
            systemId = getLong(getColumnIndexOrThrow(CallLog.Calls._ID)),
            number = number,
            timestamp = getLong(getColumnIndexOrThrow(CallLog.Calls.DATE)),
            durationSeconds = getLong(getColumnIndexOrThrow(CallLog.Calls.DURATION)).coerceAtLeast(0L),
            systemType = getInt(getColumnIndexOrThrow(CallLog.Calls.TYPE)),
            features = if (featureColumn >= 0 && !isNull(featureColumn)) getInt(featureColumn) else null,
            accountComponent = if (componentColumn >= 0) getString(componentColumn) else null,
            countryIso = countryIso,
        )
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Call history permission is not granted.")
        }
    }

    private companion object {
        const val MAX_IMPORT_SIZE = 10_000
    }
}
