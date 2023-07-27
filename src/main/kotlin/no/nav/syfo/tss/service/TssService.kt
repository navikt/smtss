package no.nav.syfo.tss.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.jms.Connection
import kotlin.math.max
import no.nav.helse.tss.samhandler.data.XMLSamhAvdPraType
import no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.syfo.EnvironmentVariables
import no.nav.syfo.logger
import org.apache.commons.text.similarity.LevenshteinDistance
import redis.clients.jedis.JedisPool

class TssService(
    private val environmentVariables: EnvironmentVariables,
    private val jedisPool: JedisPool,
    private val connection: Connection,
) {

    fun findBestTssIdEmottak(
        samhandlerfnr: String,
        samhandlerOrgName: String,
        requestId: String
    ): TSSident? {
        val enkeltSamhandler =
            fetchTssSamhandlerData(
                samhandlerfnr,
                environmentVariables,
                jedisPool,
                requestId,
                connection
            )
        return filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgName, requestId)
    }

    fun findBestTssInfotrygdId(
        samhandlerfnr: String,
        samhandlerOrgName: String,
        requestId: String
    ): TSSident? {
        val enkeltSamhandler =
            fetchTssSamhandlerData(
                samhandlerfnr,
                environmentVariables,
                jedisPool,
                requestId,
                connection
            )

        return filterOutTssIdForInfotrygd(enkeltSamhandler, samhandlerOrgName)
    }
}

fun filterOutTssIdForInfotrygd(
    enkeltSamhandler: List<XMLSamhandler>?,
    samhandlerOrgName: String
): TSSident? {
    val samhandlereAvdelinger = enkeltSamhandler?.filter { it.samhandlerAvd125 != null }
    if (samhandlereAvdelinger?.flatMapNotNull { it.samhandlerAvd125?.samhAvd } != null) {
        val samhandlerAvdelding =
            samhandlerMatchingPaaOrganisjonsNavn(
                    samhandlereAvdelinger.flatMapNotNull { it.samhandlerAvd125?.samhAvd },
                    samhandlerOrgName
                )
                ?.samhandlerAvdeling

        return if (
            samhandlerAvdelding?.idOffTSS != null &&
                (!samhandlerAvdelingIsLegevakt(samhandlerAvdelding) &&
                    !samhandlerAvdelingIsSykehusOrRegionalHelseforetak(samhandlerAvdelding))
        ) {
            TSSident(samhandlerAvdelding.idOffTSS)
        } else if (
            enkeltSamhandler
                .firstOrNull()
                ?.samhandlerAvd125
                ?.samhAvd
                ?.find { it.avdNr == "01" }
                ?.idOffTSS != null
        ) {

            TSSident(
                enkeltSamhandler
                    .firstOrNull()
                    ?.samhandlerAvd125
                    ?.samhAvd
                    ?.find { it.avdNr == "01" }
                    ?.idOffTSS!!
            )
        } else {
            null
        }
    }
    return null
}

fun filterOutTssIdForEmottak(
    enkeltSamhandler: List<XMLSamhandler>?,
    samhandlerOrgName: String,
    requestId: String
): TSSident? {
    val samhandlereAvdelinger = enkeltSamhandler?.filter { it.samhandlerAvd125 != null }
    if (samhandlereAvdelinger?.flatMapNotNull { it.samhandlerAvd125?.samhAvd } != null) {
        val samhandlerMatchingPaaOrganisjonsNavn =
            filtererBortSamhandlderPraksiserPaaProsentMatch(
                samhandlerMatchingPaaOrganisjonsNavn(
                    samhandlereAvdelinger.flatMapNotNull { it.samhandlerAvd125?.samhAvd },
                    samhandlerOrgName
                ),
                70.0,
                samhandlerOrgName,
                requestId
            )
        val samhandlerAvdelding = samhandlerMatchingPaaOrganisjonsNavn?.samhandlerAvdeling

        if (
            samhandlerAvdelding?.idOffTSS != null &&
                samhandlerMatchingPaaOrganisjonsNavn.percentageMatch > 70 &&
                (!samhandlerAvdelingIsLegevakt(samhandlerAvdelding) &&
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
    return if (
        samhandlerAvdelingMatch != null && samhandlerAvdelingMatch.percentageMatch >= prosentMatch
    ) {
        logger.info(
            "Beste match ble samhandler praksis: " +
                "Navn: ${samhandlerAvdelingMatch.samhandlerAvdeling.avdNavn} " +
                "Tssid: ${samhandlerAvdelingMatch.samhandlerAvdeling.idOffTSS} " +
                "Samhandler praksis type: ${samhandlerAvdelingMatch.samhandlerAvdeling.typeAvd} " +
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

    val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val dateToday = LocalDate.now()

    val aktiveSamhandlereMedNavn =
        samhandlereAvdelinger
            .filter { samhandlerAvdeling -> samhandlerAvdeling.gyldigAvd == "J" }
            .filter { samhandlerAvdeling -> !samhandlerAvdeling.avdNavn.isNullOrEmpty() }
            .filter { samhandlerAvdeling ->
                if (samhandlerAvdeling.datoAvdTom.trim().isNotEmpty()) {
                    LocalDate.parse(samhandlerAvdeling.datoAvdTom, dateFormatter) > dateToday
                } else samhandlerAvdeling.datoAvdTom.trim().isEmpty()
            }

    return if (aktiveSamhandlereMedNavn.isNotEmpty()) {
        samhandlereAvdelinger
            .map { samhandlerAvdeling ->
                SamhandlerAvdelingMatch(
                    samhandlerAvdeling,
                    calculatePercentageStringMatch(
                        samhandlerAvdeling.avdNavn.lowercase(),
                        samhandlerOrgName.lowercase()
                    ) * 100
                )
            }
            .maxByOrNull { it.percentageMatch }
    } else {
        null
    }
}

data class SamhandlerAvdelingMatch(
    val samhandlerAvdeling: XMLSamhAvdPraType,
    val percentageMatch: Double
)

fun samhandlerAvdelingIsLegevakt(samhandlereAvdeling: XMLSamhAvdPraType): Boolean =
    !samhandlereAvdeling.typeAvd.isNullOrEmpty() &&
        (samhandlereAvdeling.typeAvd == "LEVA" || samhandlereAvdeling.typeAvd == "LEKO")

fun samhandlerAvdelingIsSykehusOrRegionalHelseforetak(
    samhandlereAvdeling: XMLSamhAvdPraType
): Boolean =
    !samhandlereAvdeling.typeAvd.isNullOrEmpty() &&
        (samhandlereAvdeling.typeAvd == "SYKE" || samhandlereAvdeling.typeAvd == "RHFO")

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
    this@flatMapNotNull.forEach { element -> transform(element)?.forEach { it?.let(::add) } }
}
