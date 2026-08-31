package no.nav.syfo.valkey

import io.valkey.Jedis
import io.valkey.JedisPool
import no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.syfo.jsonMapper
import no.nav.syfo.logger

fun saveTSSRespons(
    jedisPool: JedisPool,
    samhandlerfnr: String,
    enkeltSamhandlerFromTSSRespons: List<XMLSamhandler>?,
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
            jsonMapper.writeValueAsString(jedisEnkeltSamhandlerFromTSSResponsModel),
        )
        logger.info("Saved enkeltSamhandlerFromTSSRespons in valkey")
    } catch (exception: Exception) {
        logger.error("Could not save enkeltSamhandlerFromTSSRespons in valkey", exception)
    } finally {
        jedis?.close()
    }
}
