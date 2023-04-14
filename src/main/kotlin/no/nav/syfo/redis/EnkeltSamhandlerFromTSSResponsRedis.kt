package no.nav.syfo.redis

import no.nav.syfo.log
import no.nav.syfo.objectMapper
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import java.time.OffsetDateTime
import java.time.ZoneOffset
import no.nav.helse.tssSamhandlerData.XMLTypeKomplett

class EnkeltSamhandlerFromTSSResponsRedis(private var jedisPool: JedisPool, private val redisSecret: String) {
    fun save(
        samhandlerfnr: String,
        enkeltSamhandlerFromTSSRespons: List<XMLTypeKomplett>?,
        timestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
    ) {
        var jedis: Jedis? = null
        try {
            jedis = jedisPool.resource
            jedis.auth(redisSecret)
            val jedisEnkeltSamhandlerFromTSSResponsModel = JedisEnkeltSamhandlerFromTSSResponsModel(timestamp, enkeltSamhandlerFromTSSRespons)
            jedis.set(
                "fnr:${samhandlerfnr}",
                objectMapper.writeValueAsString(jedisEnkeltSamhandlerFromTSSResponsModel)
            )


        } catch (ex: Exception) {
            log.error("Could not save behandler in Redis", ex)
        } finally {
            jedis?.close()
        }
    }

    fun get(samhandlerfnr: String): JedisEnkeltSamhandlerFromTSSResponsModel? {
        return when (samhandlerfnr.isNotBlank()) {
            true -> initRedis { jedis ->
                jedis.get("fnr:$samhandlerfnr")?.let {
                    getEnkeltSamhandlerFromTSSResponsFromRedis(jedis, it)
                }
            }

            false -> null
        }
    }

    private fun initRedis(block: (jedis: Jedis) -> JedisEnkeltSamhandlerFromTSSResponsModel?): JedisEnkeltSamhandlerFromTSSResponsModel? {
        var jedis: Jedis? = null
        return try {
            jedis = jedisPool.resource
            jedis.auth(redisSecret)
            block.invoke(jedis)
        } catch (ex: Exception) {
            log.error("Could not get enkeltSamhandlerFromTSSRespons in Redis", ex)
            null
        } finally {
            jedis?.close()
        }
    }

    private fun getEnkeltSamhandlerFromTSSResponsFromRedis(
        jedis: Jedis,
        samhandlerfnr: String
    ): JedisEnkeltSamhandlerFromTSSResponsModel? {
        val behandlerString = jedis.get("fnr:$samhandlerfnr")
        return when (behandlerString.isNullOrBlank()) {
            true -> null
            false -> objectMapper.readValue(behandlerString, JedisEnkeltSamhandlerFromTSSResponsModel::class.java)
        }
    }
}
