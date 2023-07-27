package no.nav.syfo.redis

import no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.syfo.logger
import no.nav.syfo.objectMapper
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

fun saveTSSRespons(
    jedisPool: JedisPool,
    samhandlerfnr: String,
    enkeltSamhandlerFromTSSRespons: List<XMLSamhandler>?
) {
    val secondsIn48Hours: Long = 172800
    var jedis: Jedis? = null
    try {
        jedis = jedisPool.resource
        val jedisEnkeltSamhandlerFromTSSResponsModel =
            JedisEnkeltSamhandlerFromTSSResponsModel(enkeltSamhandlerFromTSSRespons)
        jedis.setex(
            samhandlerfnr,
            secondsIn48Hours,
            objectMapper.writeValueAsString(jedisEnkeltSamhandlerFromTSSResponsModel),
        )
    } catch (exception: Exception) {
        logger.error("Could not save enkeltSamhandlerFromTSSRespons in Redis", exception)
    } finally {
        jedis?.close()
    }
}
