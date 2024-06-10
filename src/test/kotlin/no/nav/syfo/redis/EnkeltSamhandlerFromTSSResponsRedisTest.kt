package no.nav.syfo.redis

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.syfo.objectMapper
import no.nav.syfo.tss.service.TssServiceKtTest
import org.junit.AfterClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

val redisContainer: GenericContainer<Nothing> = GenericContainer("redis:7.0.12-alpine")


internal class EnkeltSamhandlerFromTSSResponsRedisTest {

    init {
        redisContainer.withExposedPorts(6379)
        redisContainer.start()
    }

    val jedisPool =
        JedisPool(JedisPoolConfig(), redisContainer.host, redisContainer.getMappedPort(6379))

    @Test
    internal fun `Should cache enkeltSamhandlerFromTSSRespons in redis`() {
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
        @AfterClass
        fun stopRedis() {
            redisContainer.stop()
        }
    }
}
