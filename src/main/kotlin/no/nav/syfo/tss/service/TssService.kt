package no.nav.syfo.tss.service

import no.nav.syfo.log
import javax.jms.MessageProducer
import javax.jms.Session

suspend fun findBestTssIdEmottak(
    samhandlerfnr: String,
    tssProducer: MessageProducer,
    session: Session
): String? {
    return try {
        val enkeltSamhandler = fetchTssSamhandlerData(samhandlerfnr, tssProducer, session)

        enkeltSamhandler?.firstOrNull()?.samhandlerAvd125?.samhAvd?.find {
            it.avdNr == "01"
        }?.idOffTSS
    } catch (e: Exception) {
        log.error("Call to tss throws error", e)
        null
    }
}

suspend fun findBestTssInfotrygdId(
    samhandlerfnr: String,
    tssProducer: MessageProducer,
    session: Session
): String? {
    return try {
        val enkeltSamhandler = fetchTssSamhandlerData(samhandlerfnr, tssProducer, session)

        enkeltSamhandler?.firstOrNull()?.samhandlerAvd125?.samhAvd?.find {
            it.avdNr == "01"
        }?.idOffTSS
    } catch (e: Exception) {
        log.error("Call to tss throws error", e)
        null
    }
}
