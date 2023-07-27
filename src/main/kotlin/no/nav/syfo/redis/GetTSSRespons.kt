package no.nav.syfo.redis

import no.nav.syfo.logger
import no.nav.syfo.objectMapper
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

fun getTSSRespons(
    jedisPool: JedisPool,
    samhandlerfnr: String
): JedisEnkeltSamhandlerFromTSSResponsModel? {
    var jedis: Jedis? = null
    return try {
        jedis = jedisPool.resource
        return jedis.get(samhandlerfnr)?.let {
            objectMapper.readValue(it, JedisEnkeltSamhandlerFromTSSResponsModel::class.java)
        }
    } catch (exception: Exception) {
        logger.error("Could not get enkeltSamhandlerFromTSSRespons in Redis", exception)
        null
    } finally {
        jedis?.close()
    }
}
