package app.lastcalled.data

import android.content.Context
import android.os.Build
import android.telephony.PhoneNumberUtils
import app.lastcalled.data.local.ContactEntity
import app.lastcalled.data.local.ContactPhoneEntity
import java.util.Locale

data class PhoneCandidate(
    val contactId: String,
    val displayName: String,
    val phoneNumber: String,
    val photoUri: String?,
    val isAvailable: Boolean,
)

class PhoneNumberMatcher(private val context: Context) {
    fun match(number: String?, countryIso: String?, candidates: List<PhoneCandidate>): PhoneCandidate? {
        if (number.isNullOrBlank()) return null
        val iso = countryIso?.takeIf { it.length == 2 }?.uppercase(Locale.ROOT)
            ?: Locale.getDefault().country
        val canonical = normalize(number, iso)
        val exactMatches = candidates.filter { normalize(it.phoneNumber, iso) == canonical }
        if (exactMatches.isNotEmpty()) return preferred(exactMatches)

        val suffix = significantDigits(number).takeLast(MIN_SUFFIX_LENGTH)
        if (suffix.length < MIN_SUFFIX_LENGTH) return null
        val nearMatches = candidates.filter { significantDigits(it.phoneNumber).endsWith(suffix) }
        val verifiedMatches = nearMatches.filter { candidate ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PhoneNumberUtils.areSamePhoneNumber(number, candidate.phoneNumber, iso)
            } else {
                @Suppress("DEPRECATION")
                PhoneNumberUtils.compare(context, number, candidate.phoneNumber)
            }
        }
        return verifiedMatches.takeIf(List<PhoneCandidate>::isNotEmpty)?.let(::preferred)
    }

    fun normalize(number: String, countryIso: String? = null): String {
        val iso = countryIso?.takeIf { it.length == 2 }?.uppercase(Locale.ROOT)
            ?: Locale.getDefault().country
        return PhoneNumberUtils.formatNumberToE164(number, iso)
            ?: significantDigits(number)
    }

    fun candidates(
        contacts: List<ContactEntity>,
        phones: List<ContactPhoneEntity>,
    ): List<PhoneCandidate> {
        val byId = contacts.associateBy(ContactEntity::id)
        return phones.mapNotNull { phone ->
            val contact = byId[phone.contactId] ?: return@mapNotNull null
            PhoneCandidate(
                contactId = contact.id,
                displayName = contact.displayName,
                phoneNumber = phone.number,
                photoUri = contact.photoUri,
                isAvailable = contact.isAvailable,
            )
        }.sortedByDescending(PhoneCandidate::isAvailable)
    }

    fun stableUnknownKey(number: String?, countryIso: String?, systemId: Long): String {
        if (number.isNullOrBlank()) return "unknown:private:$systemId"
        return "unknown:${normalize(number, countryIso)}"
    }

    fun significantDigits(number: String): String = number.filter(Char::isDigit)

    private fun preferred(candidates: List<PhoneCandidate>): PhoneCandidate =
        candidates.minWith(
            compareByDescending<PhoneCandidate> { it.isAvailable }
                .thenBy { it.displayName.lowercase(Locale.ROOT) },
        )

    private companion object {
        const val MIN_SUFFIX_LENGTH = 7
    }
}
