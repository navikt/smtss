package no.nav.syfo.valkey

import com.fasterxml.jackson.module.kotlin.readValue
import io.valkey.JedisPool
import io.valkey.JedisPoolConfig
import no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.syfo.objectMapper
import no.nav.syfo.tss.service.TssServiceKtTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer

val valkeyContainer: GenericContainer<Nothing> = GenericContainer("valkey/valkey:8.0.2-alpine")

internal class EnkeltSamhandlerFromTSSResponsValkeyTest {

    init {
        valkeyContainer.withExposedPorts(6379)
        valkeyContainer.start()
    }

    val jedisPool =
        JedisPool(JedisPoolConfig(), valkeyContainer.host, valkeyContainer.getMappedPort(6379))

    @Test
    internal fun `Should cache enkeltSamhandlerFromTSSRespons in valkey`() {
        val samhandlerfnr = "1232134124"

        val enkeltSamhandlerFromTSSRespons: List<XMLSamhandler>? =
            objectMapper.readValue(
                TssServiceKtTest::class
                    .java
                    .getResourceAsStream("/tssfatu.json")!!
                    .readBytes()
                    .toString(Charsets.UTF_8),
            )

        saveTSSRespons(jedisPool, samhandlerfnr, enkeltSamhandlerFromTSSRespons)

        val cachedEnkeltSamhandlerFromTSSRespons = getTSSRespons(jedisPool, samhandlerfnr)

        assertEquals(
            enkeltSamhandlerFromTSSRespons?.firstOrNull()?.samhandlerAvd125?.antSamhAvd,
            cachedEnkeltSamhandlerFromTSSRespons
                ?.enkeltSamhandlerFromTSSRespons
                ?.firstOrNull()
                ?.samhandlerAvd125
                ?.antSamhAvd,
        )
    }

    companion object {
        @JvmStatic
        @AfterAll
        fun stopValkey() {
            valkeyContainer.stop()
        }
    }
}
