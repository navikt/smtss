package no.nav.syfo.redis

import  no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.syfo.log
import no.nav.syfo.objectMapper
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

class EnkeltSamhandlerFromTSSResponsRedis(private var jedisPool: JedisPool, private val redisSecret: String) {
    fun save(
        samhandlerfnr: String,
        enkeltSamhandlerFromTSSRespons: List<XMLSamhandler>?
    ) {
        val secondsIn48Hours: Long = 172800
        var jedis: Jedis? = null
        try {
            jedis = jedisPool.resource
            jedis.auth(redisSecret)
            val jedisEnkeltSamhandlerFromTSSResponsModel =
                JedisEnkeltSamhandlerFromTSSResponsModel(enkeltSamhandlerFromTSSRespons)
            jedis.setex(
                samhandlerfnr,
                secondsIn48Hours,
                objectMapper.writeValueAsString(jedisEnkeltSamhandlerFromTSSResponsModel)
            )

        } catch (exception: Exception) {
            log.error("Could not save enkeltSamhandlerFromTSSRespons in Redis", exception)
        } finally {
            jedis?.close()
        }
    }

    fun get(samhandlerfnr: String): JedisEnkeltSamhandlerFromTSSResponsModel? {
        var jedis: Jedis? = null
        return try {
            jedis = jedisPool.resource
            jedis.auth(redisSecret)
            return jedis.get(samhandlerfnr)?.let {
                objectMapper.readValue(it, JedisEnkeltSamhandlerFromTSSResponsModel::class.java)
            }
        } catch (exception: Exception) {
            log.error("Could not get enkeltSamhandlerFromTSSRespons in Redis", exception)
            null
        } finally {
            jedis?.close()
        }

    }
}
