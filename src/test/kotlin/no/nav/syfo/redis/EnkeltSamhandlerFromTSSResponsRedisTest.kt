package no.nav.syfo.redis

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.tssSamhandlerData.XMLTypeKomplett
import no.nav.syfo.objectMapper
import no.nav.syfo.tss.service.TssServiceKtTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.embedded.RedisServer

internal class EnkeltSamhandlerFromTSSResponsRedisTest {
    private val randomport = (1000..9999).random()
    private val redisServer = RedisServer.newRedisServer()
        .port(randomport)
        .setting("bind 127.0.0.1")
        .setting("requirepass secret")
        .build()

    @BeforeEach
    fun startRedis() {
        redisServer.start()
    }

    @AfterEach
    fun stopRedis() {
        redisServer.stop()
    }

    private val jedisPool = JedisPool(JedisPoolConfig(), "localhost", randomport)

    private val enkeltSamhandlerFromTSSResponsRedis = EnkeltSamhandlerFromTSSResponsRedis(jedisPool, "secret")


    @Test
    internal fun `Should cache enkeltSamhandlerFromTSSRespons in redis`() {
        val samhandlerfnr = "1232134124"

        val enkeltSamhandlerFromTSSRespons: List<XMLTypeKomplett>? = objectMapper.readValue(
            TssServiceKtTest::class.java.getResourceAsStream("/tssfatu.json")!!.readBytes()
                .toString(Charsets.UTF_8),
        )

        enkeltSamhandlerFromTSSResponsRedis.save(samhandlerfnr, enkeltSamhandlerFromTSSRespons)

        val cachedEnkeltSamhandlerFromTSSRespons = enkeltSamhandlerFromTSSResponsRedis.get(samhandlerfnr)

        assertEquals(
            enkeltSamhandlerFromTSSRespons?.firstOrNull()?.samhandlerAvd125?.antSamhAvd,
            cachedEnkeltSamhandlerFromTSSRespons?.enkeltSamhandlerFromTSSRespons?.firstOrNull()?.samhandlerAvd125?.antSamhAvd
        )

    }
}