package no.nav.syfo.tss.service

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.tssSamhandlerData.XMLSamhandler
import no.nav.helse.tssSamhandlerData.XMLTypeKomplett
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
}