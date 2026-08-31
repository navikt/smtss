package no.nav.syfo.valkey

import io.valkey.Jedis
import io.valkey.JedisPool
import no.nav.syfo.jsonMapper
import no.nav.syfo.logger

fun getTSSRespons(
    jedisPool: JedisPool,
    samhandlerfnr: String,
): JedisEnkeltSamhandlerFromTSSResponsModel? {
    var jedis: Jedis? = null
    return try {
        jedis = jedisPool.resource
        return jedis.get(samhandlerfnr)?.let {
            jsonMapper.readValue(it, JedisEnkeltSamhandlerFromTSSResponsModel::class.java)
        }
    } catch (exception: Exception) {
        logger.error("Could not get enkeltSamhandlerFromTSSRespons in valkey", exception)
        null
    } finally {
        jedis?.close()
    }
}
