package no.nav.syfo.valkey

import io.valkey.Jedis
import io.valkey.JedisPool
import no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.syfo.logger
import no.nav.syfo.objectMapper

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
        logger.info("Saved enkeltSamhandlerFromTSSRespons in valkey")

    } catch (exception: Exception) {
        logger.error("Could not save enkeltSamhandlerFromTSSRespons in valkey", exception)
    } finally {
        jedis?.close()
    }
}
