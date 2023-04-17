package no.nav.syfo.tss.service

import no.nav.syfo.log
import kotlin.math.max
import no.nav.helse.tssSamhandlerData.XMLSamhAvdPraType
import no.nav.helse.tssSamhandlerData.XMLSamhandler
import no.nav.syfo.Environment
import no.nav.syfo.ServiceUser
import no.nav.syfo.redis.EnkeltSamhandlerFromTSSResponsRedis
import org.apache.commons.text.similarity.LevenshteinDistance

class TssService(private val environment: Environment,
                 private val serviceUser: ServiceUser,
                 private val enkeltSamhandlerFromTSSResponsRedis: EnkeltSamhandlerFromTSSResponsRedis
) {
    fun findBestTssIdEmottak(
        samhandlerfnr: String,
        samhandlerOrgName: String,
    ): TSSident? {
        return try {
            val enkeltSamhandler = fetchTssSamhandlerData(samhandlerfnr, environment, serviceUser, enkeltSamhandlerFromTSSResponsRedis)

            return filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgName)

        } catch (exception: Exception) {
            log.warn("Call to tss throws error", exception)
            null
        }
    }

    fun findBestTssInfotrygdId(
        samhandlerfnr: String,
        samhandlerOrgName: String,
    ): TSSident? {
        return try {
            val enkeltSamhandler = fetchTssSamhandlerData(samhandlerfnr, environment, serviceUser, enkeltSamhandlerFromTSSResponsRedis)

            return  filterOutTssIdForInfotrygd(enkeltSamhandler, samhandlerOrgName)
        } catch (e: Exception) {
            log.warn("Call to tss throws error", e)
            null
        }
    }
}

fun filterOutTssIdForInfotrygd(enkeltSamhandler: List<XMLSamhandler>?, samhandlerOrgName: String): TSSident? {
    if (enkeltSamhandler?.firstOrNull()?.samhandlerAvd125 != null)
    {
        val samhandlerAvdelding = samhandlerMatchingPaaOrganisjonsNavn(enkeltSamhandler.first().samhandlerAvd125.samhAvd, samhandlerOrgName)?.samhandlerAvdeling

        if (samhandlerAvdelding?.idOffTSS != null && (
                    !samhandlerAvdelingIsLegevakt(samhandlerAvdelding) &&
                            !samhandlerAvdelingIsSykehusOrRegionalHelseforetak(samhandlerAvdelding))) {
            return TSSident(samhandlerAvdelding.idOffTSS)
        }
        else {
            enkeltSamhandler.firstOrNull()?.samhandlerAvd125?.samhAvd?.find {
                it.avdNr == "01"
            }?.idOffTSS
        }
    }
    return null
}

fun filterOutTssIdForEmottak(enkeltSamhandler: List<XMLSamhandler>?, samhandlerOrgName: String): TSSident? {
    if (enkeltSamhandler?.firstOrNull()?.samhandlerAvd125 != null)
    {
        val samhandlerAvdelding = samhandlerMatchingPaaOrganisjonsNavn(enkeltSamhandler.first().samhandlerAvd125.samhAvd, samhandlerOrgName)?.samhandlerAvdeling

        if (samhandlerAvdelding?.idOffTSS != null && (
                    !samhandlerAvdelingIsLegevakt(samhandlerAvdelding) &&
                            !samhandlerAvdelingIsSykehusOrRegionalHelseforetak(samhandlerAvdelding))) {
           return TSSident(samhandlerAvdelding.idOffTSS)
        }
    }
   return null
}



fun samhandlerMatchingPaaOrganisjonsNavn(samhandlereAvdelinger: List<XMLSamhAvdPraType>, samhandlerOrgName: String): SamhandlerAvdelingMatch? {

    val aktiveSamhandlereMedNavn = samhandlereAvdelinger
        .filter { samhandlerAvdeling -> samhandlerAvdeling.gyldigAvd == "J" }
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

fun samhandlerAvdelingIsSykehusOrRegionalHelseforetak(samhandlereAvdeling: XMLSamhAvdPraType): Boolean =
    !samhandlereAvdeling.typeAvd.isNullOrEmpty() && (
            samhandlereAvdeling.typeAvd == "SYKE" ||
                    samhandlereAvdeling.typeAvd == "RHFO"
            )


fun calculatePercentageStringMatch(str1: String?, str2: String): Double {
    val maxDistance = max(str1?.length!!, str2.length).toDouble()
    val distance = LevenshteinDistance().apply(str2, str1).toDouble()
    return (maxDistance - distance) / maxDistance
}

data class TSSident(
    val tssid: String,
)
