package no.nav.syfo.tss.service

import javax.jms.Connection

import no.nav.syfo.log
import kotlin.math.max
import no.nav.helse.tss.samhandler.data.XMLSamhAvdPraType
import no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.syfo.Environment
import no.nav.syfo.redis.EnkeltSamhandlerFromTSSResponsRedis
import org.apache.commons.text.similarity.LevenshteinDistance

class TssService(
    private val environment: Environment,
    private val enkeltSamhandlerFromTSSResponsRedis: EnkeltSamhandlerFromTSSResponsRedis,
    private val connection: Connection,
) {


    fun findBestTssIdEmottak(
        samhandlerfnr: String,
        samhandlerOrgName: String,
        requestId: String
    ): TSSident? {
        return try {
            val enkeltSamhandler = fetchTssSamhandlerData(
                samhandlerfnr,
                environment,
                enkeltSamhandlerFromTSSResponsRedis,
                requestId,
                connection
            )
            return filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgName, requestId)

        } catch (exception: Exception) {
            log.error("Call to tss throws error for requestId $requestId", exception)
            null
        }
    }

    fun findBestTssInfotrygdId(
        samhandlerfnr: String,
        samhandlerOrgName: String,
        requestId: String
    ): TSSident? {
        return try {
            val enkeltSamhandler = fetchTssSamhandlerData(
                samhandlerfnr,
                environment,
                enkeltSamhandlerFromTSSResponsRedis,
                requestId,
                connection
            )

            return filterOutTssIdForInfotrygd(enkeltSamhandler, samhandlerOrgName)
        } catch (e: Exception) {
            log.error("Call to tss throws error for requestId $requestId", e)
            null
        }
    }
}

fun filterOutTssIdForInfotrygd(enkeltSamhandler: List<XMLSamhandler>?, samhandlerOrgName: String): TSSident? {
    val samhandlereAvdelinger = enkeltSamhandler?.filter { it.samhandlerAvd125 != null }
    if (samhandlereAvdelinger?.flatMapNotNull { it.samhandlerAvd125?.samhAvd } != null) {
        val samhandlerAvdelding = samhandlerMatchingPaaOrganisjonsNavn(
            samhandlereAvdelinger.flatMapNotNull { it.samhandlerAvd125?.samhAvd },
            samhandlerOrgName
        )?.samhandlerAvdeling

        if (samhandlerAvdelding?.idOffTSS != null && (
                    !samhandlerAvdelingIsLegevakt(samhandlerAvdelding) &&
                            !samhandlerAvdelingIsSykehusOrRegionalHelseforetak(samhandlerAvdelding))
        ) {
            return TSSident(samhandlerAvdelding.idOffTSS)
        } else if (enkeltSamhandler.firstOrNull()?.samhandlerAvd125?.samhAvd?.find {
                it.avdNr == "01"
            }?.idOffTSS != null) {

            return TSSident(enkeltSamhandler.firstOrNull()?.samhandlerAvd125?.samhAvd?.find {
                it.avdNr == "01"
            }?.idOffTSS!!)
        } else {
            return null
        }
    }
    return null
}

fun filterOutTssIdForEmottak(enkeltSamhandler: List<XMLSamhandler>?, samhandlerOrgName: String, requestId: String): TSSident? {
    val samhandlereAvdelinger = enkeltSamhandler?.filter { it.samhandlerAvd125 != null }
    if (samhandlereAvdelinger?.flatMapNotNull { it.samhandlerAvd125?.samhAvd } != null) {
        val samhandlerMatchingPaaOrganisjonsNavn = filtererBortSamhandlderPraksiserPaaProsentMatch(samhandlerMatchingPaaOrganisjonsNavn(
            samhandlereAvdelinger.flatMapNotNull { it.samhandlerAvd125?.samhAvd },
            samhandlerOrgName
        ), 70.0, samhandlerOrgName, requestId)
        val samhandlerAvdelding = samhandlerMatchingPaaOrganisjonsNavn?.samhandlerAvdeling

        if (samhandlerAvdelding?.idOffTSS != null && samhandlerMatchingPaaOrganisjonsNavn.percentageMatch > 70 && (
                    !samhandlerAvdelingIsLegevakt(samhandlerAvdelding) &&
                            !samhandlerAvdelingIsSykehusOrRegionalHelseforetak(samhandlerAvdelding)) &&
            !samhandlerAvdelingIsAvdNr01(samhandlerAvdelding)
        ) {
            return TSSident(samhandlerAvdelding.idOffTSS)
        }
    }
    return null
}

fun filtererBortSamhandlderPraksiserPaaProsentMatch(
    samhandlerAvdelingMatch: SamhandlerAvdelingMatch?,
    prosentMatch: Double,
    samhandlerOrgName: String,
    requestId: String
): SamhandlerAvdelingMatch? {
    return if (samhandlerAvdelingMatch != null && samhandlerAvdelingMatch.percentageMatch >= prosentMatch) {
        log.info(
            "Beste match ble samhandler praksis: " +
                    "Navn: ${samhandlerAvdelingMatch.samhandlerAvdeling.avdNavn } " +
                    "Tssid: ${samhandlerAvdelingMatch.samhandlerAvdeling.idOffTSS} " +
                    "Samhandler praksis type: ${samhandlerAvdelingMatch.samhandlerAvdeling.typeAvd } " +
                    "Prosent match:${samhandlerAvdelingMatch.percentageMatch} %, basert på sykmeldingens organisjons navn: $samhandlerOrgName " +
                    "requestId = $requestId",
        )
        samhandlerAvdelingMatch
    } else {
        null
    }
}

fun samhandlerMatchingPaaOrganisjonsNavn(
    samhandlereAvdelinger: List<XMLSamhAvdPraType>,
    samhandlerOrgName: String
): SamhandlerAvdelingMatch? {

    val aktiveSamhandlereMedNavn = samhandlereAvdelinger
        .filter { samhandlerAvdeling -> samhandlerAvdeling.gyldigAvd == "J" }
        .filter { samhandlerAvdeling -> !samhandlerAvdeling.avdNavn.isNullOrEmpty() }

    return if (aktiveSamhandlereMedNavn.isNotEmpty()) {
        samhandlereAvdelinger.map { samhandlerAvdeling ->
            SamhandlerAvdelingMatch(
                samhandlerAvdeling,
                calculatePercentageStringMatch(
                    samhandlerAvdeling.avdNavn.lowercase(),
                    samhandlerOrgName.lowercase()
                ) * 100
            )
        }.maxByOrNull { it.percentageMatch }
    } else {
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

fun samhandlerAvdelingIsAvdNr01(samhandlereAvdeling: XMLSamhAvdPraType): Boolean =
    samhandlereAvdeling.avdNr == "01"

fun calculatePercentageStringMatch(str1: String?, str2: String): Double {
    val maxDistance = max(str1?.length!!, str2.length).toDouble()
    val distance = LevenshteinDistance().apply(str2, str1).toDouble()
    return (maxDistance - distance) / maxDistance
}

data class TSSident(
    val tssid: String,
)


inline fun <T, R> Iterable<T>.flatMapNotNull(transform: (T) -> Iterable<R?>?): List<R> = buildList {
    this@flatMapNotNull.forEach { element ->
        transform(element)?.forEach { it?.let(::add) }
    }
}