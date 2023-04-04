package no.nav.syfo.tss.service

import no.nav.syfo.log
import no.nav.syfo.mq.producerForQueue
import no.nav.syfo.objectMapper
import no.nav.syfo.securelog
import javax.jms.Connection
import javax.jms.Session
import kotlin.math.max
import no.nav.helse.tssSamhandlerData.XMLSamhAvdPraType
import no.nav.helse.tssSamhandlerData.XMLSamhandler
import no.nav.helse.tssSamhandlerData.XMLTypeKomplett
import org.apache.commons.text.similarity.LevenshteinDistance

suspend fun findBestTssIdEmottak(
    samhandlerfnr: String,
    samhandlerOrgName: String,
    connection: Connection,
    tssQueue: String,
): TSSident? {
    return try {
        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val tssProducer = session.producerForQueue("queue:///$tssQueue?targetClient=1")

        val enkeltSamhandler = fetchTssSamhandlerData(samhandlerfnr, tssProducer, session)
        securelog.info("enkeltSamhandler: ${objectMapper.writeValueAsString(enkeltSamhandler)}")

        return filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgName)

    } catch (exception: Exception) {
        log.warn("Call to tss throws error", exception)
        null
    }
}

fun filterOutTssIdForEmottak(enkeltSamhandler: List<XMLSamhandler>?, samhandlerOrgName: String): TSSident? {
    if (enkeltSamhandler?.firstOrNull()?.samhandlerAvd125 != null)
    {
        val samhandlerAvdelding = samhandlerMatchingPaaOrganisjonsNavn(enkeltSamhandler.first().samhandlerAvd125.samhAvd, samhandlerOrgName)?.samhandlerAvdeling

        if (samhandlerAvdelding?.idOffTSS != null && (
                    !samhandlerAvdelingIsLegevakt(samhandlerAvdelding)
                            && !samhandlerAvdelingIsFastlegeOrFastlonnet(samhandlerAvdelding))) {
           return TSSident(samhandlerAvdelding.idOffTSS)
        }
    }
   return null
}



fun samhandlerMatchingPaaOrganisjonsNavn(samhandlereAvdelinger: List<XMLSamhAvdPraType>, samhandlerOrgName: String): SamhandlerAvdelingMatch? {

    val aktiveSamhandlereMedNavn = samhandlereAvdelinger
        .filter { samhandlerAvdeling -> samhandlerAvdeling.gyldigAvd == "J" }
        .filter { samhandlerAvdeling -> samhandlerAvdeling.kilde == "FKR" }
        .filter { samhandlerAvdeling -> !samhandlerAvdeling.avdNavn.isNullOrEmpty() }

    return if (aktiveSamhandlereMedNavn.isNotEmpty()) {
        samhandlereAvdelinger.map { samhandlerAvdeling ->
        SamhandlerAvdelingMatch(samhandlerAvdeling, calculatePercentageStringMatch(samhandlerAvdeling.avdNavn.lowercase(), samhandlerOrgName.lowercase()) * 100)
    }.maxByOrNull { it.percentageMatch }
    }else {
        null
    }

}

data class SamhandlerAvdelingMatch(val samhandlerAvdeling: XMLSamhAvdPraType, val percentageMatch: Double)

fun samhandlerAvdelingIsLegevakt(samhandlereAvdeling: XMLSamhAvdPraType): Boolean =
    !samhandlereAvdeling.typeAvd.isNullOrEmpty() && (
            samhandlereAvdeling.typeAvd == "LEVA" ||
                    samhandlereAvdeling.typeAvd == "LEKO"
            )

fun samhandlerAvdelingIsFastlegeOrFastlonnet(samhandlereAvdeling: XMLSamhAvdPraType): Boolean =
    !samhandlereAvdeling.typeAvd.isNullOrEmpty() && (
            samhandlereAvdeling.typeAvd == "FALO" ||
                    samhandlereAvdeling.typeAvd == "FALE"
            )


fun calculatePercentageStringMatch(str1: String?, str2: String): Double {
    val maxDistance = max(str1?.length!!, str2.length).toDouble()
    val distance = LevenshteinDistance().apply(str2, str1).toDouble()
    return (maxDistance - distance) / maxDistance
}

suspend fun findBestTssInfotrygdId(
    samhandlerfnr: String,
    connection: Connection,
    tssQueue: String,
): TSSident? {
    return try {
        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val tssProducer = session.producerForQueue("queue:///$tssQueue?targetClient=1")

        val enkeltSamhandler = fetchTssSamhandlerData(samhandlerfnr, tssProducer, session)
        securelog.info("enkeltSamhandler: ${objectMapper.writeValueAsString(enkeltSamhandler)}")

        val tssid = enkeltSamhandler?.firstOrNull()?.samhandlerAvd125?.samhAvd?.find {
            it.avdNr == "01"
        }?.idOffTSS

        if (tssid != null) {
            TSSident(tssid)
        }

        null
    } catch (e: Exception) {
        log.warn("Call to tss throws error", e)
        null
    }
}

data class TSSident(
    val tssid: String,
)
