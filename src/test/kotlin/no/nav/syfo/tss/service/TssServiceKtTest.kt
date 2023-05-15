package no.nav.syfo.tss.service

import com.fasterxml.jackson.module.kotlin.readValue
import  no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.syfo.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TssServiceKtTest {
    @Test
    internal fun `Returns correct tssid for emottak when samhandleravd is FATU`() {
        val expectedTssId = "80000347193"

        val enkeltSamhandler: List<XMLSamhandler>? = objectMapper.readValue(
            TssServiceKtTest::class.java.getResourceAsStream("/tssfatu.json")!!.readBytes()
                .toString(Charsets.UTF_8),
        )

        val samhandlerOrgnavn = "test legesenter"

        val tssId = filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgnavn)?.tssid

        assertEquals(expectedTssId, tssId)

    }

    @Test
    internal fun `Returns correct tssid even when one samhandlerAvd125 is null`() {
        val expectedTssId = "80000347193"

        val enkeltSamhandler: List<XMLSamhandler>? = objectMapper.readValue(
            TssServiceKtTest::class.java.getResourceAsStream("/tssavd125null.json")!!.readBytes()
                .toString(Charsets.UTF_8),
        )

        val samhandlerOrgnavn = "test legesenter"

        val tssId = filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgnavn)?.tssid

        assertEquals(expectedTssId, tssId)

    }
}