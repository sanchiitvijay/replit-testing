package app.lastcalled.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CallClassificationTest {
    @Test
    fun mapsSystemCallDirectionsWithoutGuessing() {
        assertEquals(CallDirection.INCOMING, CallClassification.direction(1))
        assertEquals(CallDirection.OUTGOING, CallClassification.direction(2))
        assertEquals(CallDirection.MISSED, CallClassification.direction(3))
        assertEquals(CallDirection.REJECTED, CallClassification.direction(5))
        assertEquals(CallDirection.REJECTED, CallClassification.direction(6))
        assertEquals(CallDirection.VOICEMAIL, CallClassification.direction(4))
        assertEquals(CallDirection.ANSWERED_EXTERNALLY, CallClassification.direction(7))
        assertEquals(CallDirection.SCREENED, CallClassification.direction(8))
        assertEquals(CallDirection.UNKNOWN, CallClassification.direction(99))
    }

    @Test
    fun recognizesOnlyExplicitWhatsAppAccountMetadata() {
        assertEquals(
            CallSource.WHATSAPP,
            CallClassification.source(
                "com.whatsapp/com.whatsapp.voipcalling.VoipConnectionService",
                unifiedHistoryAvailable = true,
            ),
        )
        assertEquals(
            CallSource.WHATSAPP,
            CallClassification.source("com.whatsapp.w4b/example.Service", true),
        )
        assertEquals(
            CallSource.UNKNOWN,
            CallClassification.source("com.example.voip/example.CallService", true),
        )
        assertEquals(
            CallSource.PHONE,
            CallClassification.source(null, unifiedHistoryAvailable = false),
        )
        assertEquals(
            CallSource.UNKNOWN,
            CallClassification.source(null, unifiedHistoryAvailable = true),
        )
    }

    @Test
    fun videoRequiresTheSystemVideoFeatureFlag() {
        assertEquals(CallMediaType.VIDEO, CallClassification.mediaType(1))
        assertEquals(CallMediaType.VOICE, CallClassification.mediaType(0))
        assertEquals(CallMediaType.UNKNOWN, CallClassification.mediaType(null))
    }
}
