package no.nav.syfo.tss.service

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.syfo.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TssServiceKtTest {
    @Test
    internal fun `Returns correct tssid for emottak when samhandleravd is FATU`() {
        val expectedTssId = "80000347193"
        val orgnummer = "123456789"
        val enkeltSamhandler: List<XMLSamhandler>? =
            objectMapper.readValue(
                TssServiceKtTest::class
                    .java
                    .getResourceAsStream("/tssfatu.json")!!
                    .readBytes()
                    .toString(Charsets.UTF_8),
            )

        val samhandlerOrgnavn = "test legesenter"
        val requestId = "132-23344-213123"

        val tssId =
            filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgnavn, requestId, orgnummer)
                ?.tssid

        assertEquals(expectedTssId, tssId)
    }

    @Test
    internal fun `Returns legekontor if same name as legevakt without orgnummer`() {
        val expectedTssId = "80000347195"
        val orgnummer = null
        val enkeltSamhandler: List<XMLSamhandler>? =
            objectMapper.readValue(
                TssServiceKtTest::class
                    .java
                    .getResourceAsStream("/tssLegevaktOgLegekontor.json")!!
                    .readBytes()
                    .toString(Charsets.UTF_8),
            )

        val samhandlerOrgnavn = "TEST LEGESENTER AS"
        val requestId = "132-23344-213123"

        val tssId =
            filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgnavn, requestId, orgnummer)
                ?.tssid

        assertEquals(expectedTssId, tssId)
    }
    @Test
    internal fun `Returns legekontor if same name as legevakt with orgnummer`() {
        val expectedTssId = "80000347195"
        val orgnummer = "1234"
        val enkeltSamhandler: List<XMLSamhandler>? =
            objectMapper.readValue(
                TssServiceKtTest::class
                    .java
                    .getResourceAsStream("/tssLegevaktOgLegekontor.json")!!
                    .readBytes()
                    .toString(Charsets.UTF_8),
            )

        val samhandlerOrgnavn = "TEST LEGESENTER AS"
        val requestId = "132-23344-213123"

        val tssId =
            filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgnavn, requestId, orgnummer)
                ?.tssid

        assertEquals(expectedTssId, tssId)
    }

    @Test
    internal fun `Returns correct tssid even when one samhandlerAvd125 is null`() {
        val expectedTssId = "80000347193"
        val orgnummer = "123456789"
        val enkeltSamhandler: List<XMLSamhandler>? =
            objectMapper.readValue(
                TssServiceKtTest::class
                    .java
                    .getResourceAsStream("/tssavd125null.json")!!
                    .readBytes()
                    .toString(Charsets.UTF_8),
            )

        val samhandlerOrgnavn = "test legesenter"
        val requestId = "132-23344-213123"

        val tssId =
            filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgnavn, requestId, orgnummer)
                ?.tssid

        assertEquals(expectedTssId, tssId)
    }

    @Test
    internal fun `Returns correct tssid when match prosent is 83`() {
        val expectedTssId = "80000347193"
        val orgnummer = "123456789"
        val enkeltSamhandler: List<XMLSamhandler>? =
            objectMapper.readValue(
                TssServiceKtTest::class
                    .java
                    .getResourceAsStream("/tssavd125null.json")!!
                    .readBytes()
                    .toString(Charsets.UTF_8),
            )

        val samhandlerOrgnavn = "test legesenterete"
        val requestId = "132-23344-213123"

        val tssId =
            filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgnavn, requestId, orgnummer)
                ?.tssid

        assertEquals(expectedTssId, tssId)
    }

    @Test
    internal fun `Returns correct tssid even when one samhandlerAvd125 avdNavn is null`() {
        val expectedTssId = "80000347193"
        val orgnummer = "123456789"
        val enkeltSamhandler: List<XMLSamhandler>? =
            objectMapper.readValue(
                TssServiceKtTest::class
                    .java
                    .getResourceAsStream("/tssavd125avdnavnnull.json")!!
                    .readBytes()
                    .toString(Charsets.UTF_8),
            )

        val samhandlerOrgnavn = "test legesenter"
        val requestId = "132-23344-213123"

        val tssId =
            filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgnavn, requestId, orgnummer)
                ?.tssid

        assertEquals(expectedTssId, tssId)
    }

    @Test
    internal fun `Returns correct arena tssid even when one of samhandlerAvd125 typeAvd is SYPL`() {
        val expectedTssId = "80000347193"
        val orgnummer = "123456789"
        val enkeltSamhandler: List<XMLSamhandler>? =
            objectMapper.readValue(
                TssServiceKtTest::class
                    .java
                    .getResourceAsStream("/tssavd125typeavdsykepleier.json")!!
                    .readBytes()
                    .toString(Charsets.UTF_8),
            )

        val samhandlerOrgnavn = "test legesenter"

        val tssId = filterOutTssIdForArena(enkeltSamhandler, samhandlerOrgnavn, orgnummer)?.tssid

        assertEquals(expectedTssId, tssId)
    }
}
