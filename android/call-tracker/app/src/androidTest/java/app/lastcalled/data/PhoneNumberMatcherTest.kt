package app.lastcalled.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhoneNumberMatcherTest {
    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext
    private val matcher = PhoneNumberMatcher(context)

    @Test
    fun normalizesInternationalAndIndianNationalForms() {
        assertEquals(
            "+919876543210",
            matcher.normalize("+91 98765 43210", "IN"),
        )
        assertEquals(
            "+919876543210",
            matcher.normalize("09876543210", "IN"),
        )
    }

    @Test
    fun matchesFormattingDifferencesButDoesNotGuessBySuffixAlone() {
        val contact = PhoneCandidate(
            contactId = "contact:1",
            displayName = "Anika",
            phoneNumber = "+91 98765 43210",
            photoUri = null,
            isAvailable = true,
        )

        assertNotNull(matcher.match("09876543210", "IN", listOf(contact)))
        assertEquals(null, matcher.match("11111143210", "IN", listOf(contact)))
    }
}
