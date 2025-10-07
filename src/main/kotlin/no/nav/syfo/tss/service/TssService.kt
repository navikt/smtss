package no.nav.syfo.tss.service

import io.valkey.JedisPool
import jakarta.jms.Connection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import no.nav.helse.tss.samhandler.data.XMLSamhAvdPraType
import no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.syfo.EnvironmentVariables
import no.nav.syfo.logger
import no.nav.syfo.objectMapper
import no.nav.syfo.securelog
import org.apache.commons.text.similarity.LevenshteinDistance

class TssService(
    private val environmentVariables: EnvironmentVariables,
    private val jedisPool: JedisPool,
    private val connection: Connection,
) {
    fun findBestTssIdEmottak(
        samhandlerfnr: String,
        samhandlerOrgName: String,
        requestId: String,
        orgnummer: String?,
    ): TSSident? {
        val enkeltSamhandler =
            fetchTssSamhandlerData(
                samhandlerfnr,
                environmentVariables,
                jedisPool,
                requestId,
                connection,
            )
        val tssId =
            filterOutTssIdForEmottak(enkeltSamhandler, samhandlerOrgName, requestId, orgnummer)
        logTssId("emottak", tssId, samhandlerfnr, requestId, samhandlerOrgName, enkeltSamhandler)
        return tssId
    }

    fun findBestTssIdInfotrygd(
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
                connection,
            )
        val tssId = filterOutTssIdForInfotrygd(enkeltSamhandler, samhandlerOrgName)
        logTssId("infotrygd", tssId, samhandlerfnr, requestId, samhandlerOrgName, enkeltSamhandler)
        return tssId
    }

    fun getSamhandlerInst(
        samhandlerId: String,
        requestId: String,
        samhandlerIdType: String,
    ): List<XMLSamhandler>? {
        return fetchTssSamhandlerInst(
            connection,
            environmentVariables,
            samhandlerId,
            requestId,
            samhandlerIdType
        )
    }

    fun findBestTssIdArena(
        samhandlerfnr: String,
        samhandlerOrgName: String,
        requestId: String,
        orgnummer: String?,
    ): TSSident? {
        val enkeltSamhandler =
            fetchTssSamhandlerData(
                samhandlerfnr,
                environmentVariables,
                jedisPool,
                requestId,
                connection,
            )

        val tssId = filterOutTssIdForArena(enkeltSamhandler, samhandlerOrgName, orgnummer)
        logTssId("arena", tssId, samhandlerfnr, requestId, samhandlerOrgName, enkeltSamhandler)
        return tssId
    }

    fun findAllSamhandler(
        samhandlerfnr: String,
        requestId: String,
    ): List<XMLSamhandler>? {
        return fetchTssSamhandlerData(
            samhandlerfnr,
            environmentVariables,
            jedisPool,
            requestId,
            connection,
        )
    }

    private fun logTssId(
        forSystem: String,
        tssId: TSSident?,
        samhandlerfnr: String,
        requestId: String,
        samhandlerOrgName: String,
        enkeltSamhandler: List<XMLSamhandler>?
    ) {
        if (tssId != null) {
            securelog.info(
                "Did find tssId: $tssId for $forSystem: fnr: $samhandlerfnr, requestId: $requestId, orgname: $samhandlerOrgName, from this data: ${
                    objectMapper.writeValueAsString(
                        enkeltSamhandler,
                    )
                }",
            )
        } else {
            securelog.info(
                "Did not find tssId for $forSystem: fnr: $samhandlerfnr, requestId: $requestId, orgname: $samhandlerOrgName, from this data: ${
                    objectMapper.writeValueAsString(
                        enkeltSamhandler,
                    )
                }",
            )
        }
    }
}

fun filterOutTssIdForArena(
    enkeltSamhandler: List<XMLSamhandler>?,
    samhandlerOrgName: String,
    samhandlerOrgnummer: String?,
): TSSident? {
    val samhandlereAvdelinger = enkeltSamhandler?.filter { it.samhandlerAvd125 != null }
    if (samhandlereAvdelinger?.flatMapNotNull { it.samhandlerAvd125?.samhAvd } != null) {
        val dissallowedTypes =
            listOf(
                "AUDI",
                "FRBE",
                "HELS",
                "JORD",
                "KURS",
                "LARO",
                "LOGO",
                "MULT",
                "OKSY",
                "OPPT",
                "ORTO",
                "PASI",
                "PSNE",
                "PSNO",
                "PSSP",
                "PSUT",
                "RHFO",
                "KORI",
                "SYPL",
                "SYKE",
                "TAPO",
                "TPPR",
                "TPUT",
                "URRE",
                "UTKA",
                "KOHS",
                "KOMU",
                "BIRE",
                "BEUT",
                "SYPL",
            )
        val arenaApprovedSamhandlerAvdelinger =
            samhandlereAvdelinger
                .flatMapNotNull { it.samhandlerAvd125?.samhAvd }
                .asSequence()
                .filterNot { it.typeAvd in dissallowedTypes }
                .filter { it.avdNr != "01" }
                .toList()

        if (samhandlerOrgnummer != null) {
            val samhandlerOrgnummerMatch =
                arenaApprovedSamhandlerAvdelinger.filter { it.offNrAvd == samhandlerOrgnummer }
            val tssId = tryGetTssId(samhandlerOrgnummerMatch)
            if (tssId != null) {
                return tssId
            }
        }

        val samhandlerAvdelding =
            samhandlerMatchingPaaOrganisjonsNavn(
                arenaApprovedSamhandlerAvdelinger,
                samhandlerOrgName,
            )
        val max = samhandlerAvdelding.maxOfOrNull { it.percentageMatch }
        if (max == null) return null
        val samhanldingNameMatch =
            samhandlerAvdelding.filter { it.percentageMatch >= max }.map { it.samhandlerAvdeling }

        return tryGetTssId(samhanldingNameMatch)
    }
    return null
}

private fun tryGetTssId(samhandlerAvdeling: List<XMLSamhAvdPraType>): TSSident? {
    if (samhandlerAvdeling.isNotEmpty()) {
        val samhandlerUtenLegevakt =
            samhandlerAvdeling.firstOrNull { !samhandlerAvdelingIsLegevakt(it) }
        return if (samhandlerUtenLegevakt != null) {
            TSSident(samhandlerUtenLegevakt.idOffTSS)
        } else {
            TSSident(samhandlerAvdeling.first().idOffTSS)
        }
    }
    return null
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
                    samhandlerOrgName,
                )
                .filter {
                    it.samhandlerAvdeling.idOffTSS != null &&
                        !samhandlerAvdelingIsLegevakt(it.samhandlerAvdeling) &&
                        !samhandlerAvdelingIsSykehusOrRegionalHelseforetak(
                            it.samhandlerAvdeling,
                        )
                }

        return if (samhandlerAvdelding.isNotEmpty()) {
            TSSident(samhandlerAvdelding.maxBy { it.percentageMatch }.samhandlerAvdeling.idOffTSS)
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
                    ?.idOffTSS!!,
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
    requestId: String,
    orgnummer: String?,
): TSSident? {
    val samhandlereAvdelinger =
        enkeltSamhandler?.flatMapNotNull { it.samhandlerAvd125?.samhAvd } ?: emptyList()
    if (samhandlereAvdelinger.isNotEmpty()) {
        val aktiveSamhandlereMedNavn =
            getAktiveSamhandlere(samhandlereAvdelinger).filter {
                it.idOffTSS != null &&
                    !samhandlerAvdelingIsLegevakt(it) &&
                    !samhandlerAvdelingIsSykehusOrRegionalHelseforetak(
                        it,
                    ) &&
                    !samhandlerAvdelingIsAvdNr01(it)
            }

        if (orgnummer != null) {
            logger.info("Trying to get tssId from orgnummer $orgnummer and $requestId")
            val samhandlerFromOrgnummer =
                aktiveSamhandlereMedNavn.firstOrNull { it.offNrAvd == orgnummer }
            if (samhandlerFromOrgnummer != null) {
                logger.info(
                    "Found tssId for orgunnmer: $orgnummer and requestId: $requestId, tssId: ${samhandlerFromOrgnummer.idOffTSS}"
                )
                return TSSident(samhandlerFromOrgnummer.idOffTSS)
            }
        }

        val samhandlereWithNameMath =
            getSamhandlereWithNameMatch(
                    aktiveSamhandlereMedNavn,
                    samhandlerOrgName,
                )
                .maxByOrNull { it.percentageMatch }

        return filtererBortSamhandlderPraksiserPaaProsentMatch(
                samhandlereWithNameMath,
                70.0,
                samhandlerOrgName,
                requestId,
            )
            ?.let { TSSident(it.samhandlerAvdeling.idOffTSS) }
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

fun getAktiveSamhandlere(
    samhandlereAvdelinger: List<XMLSamhAvdPraType>,
): List<XMLSamhAvdPraType> {
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

    return aktiveSamhandlereMedNavn
}

fun samhandlerMatchingPaaOrganisjonsNavn(
    samhandlereAvdelinger: List<XMLSamhAvdPraType>,
    samhandlerOrgName: String
): List<SamhandlerAvdelingMatch> {
    val aktiveSamhandlereMedNavn = getAktiveSamhandlere(samhandlereAvdelinger)
    return getSamhandlereWithNameMatch(aktiveSamhandlereMedNavn, samhandlerOrgName)
}

private fun getSamhandlereWithNameMatch(
    aktiveSamhandlereMedNavn: List<XMLSamhAvdPraType>,
    samhandlerOrgName: String
): List<SamhandlerAvdelingMatch> {
    return aktiveSamhandlereMedNavn.map { samhandlerAvdeling ->
        SamhandlerAvdelingMatch(
            samhandlerAvdeling,
            calculatePercentageStringMatch(
                samhandlerAvdeling.avdNavn.lowercase(),
                samhandlerOrgName.lowercase(),
            ) * 100,
        )
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
    val distance = LevenshteinDistance.getDefaultInstance().apply(str2, str1).toDouble()
    return (maxDistance - distance) / maxDistance
}

data class TSSident(
    val tssid: String,
)

inline fun <T, R> Iterable<T>.flatMapNotNull(transform: (T) -> Iterable<R?>?): List<R> = buildList {
    this@flatMapNotNull.forEach { element -> transform(element)?.forEach { it?.let(::add) } }
}
