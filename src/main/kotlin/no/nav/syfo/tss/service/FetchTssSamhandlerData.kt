package no.nav.syfo.tss.service

import no.nav.helse.tssSamhandlerData.XMLTServicerutiner
import no.nav.helse.tssSamhandlerData.XMLTidOFF1
import no.nav.helse.tssSamhandlerData.XMLTssSamhandlerData
import no.nav.syfo.helpers.retry
import no.nav.syfo.util.toString
import no.nav.syfo.util.tssSamhandlerdataInputMarshaller
import no.nav.syfo.util.tssSamhandlerdataUnmarshaller
import java.io.IOException
import java.io.StringReader
import java.lang.IllegalStateException
import javax.jms.MessageProducer
import javax.jms.Session
import javax.jms.TemporaryQueue
import javax.jms.TextMessage
import no.nav.helse.tssSamhandlerData.XMLSamhandlerIDataB910Type
import no.nav.helse.tssSamhandlerData.XMLTypeKomplett

suspend fun fetchTssSamhandlerData(
    samhandlerfnr: String,
    tssSamhnadlerInfoProducer: MessageProducer,
    session: Session,
): List<XMLTypeKomplett>? =
    retry(
        callName = "tss_fetch_samhandler_data",
        retryIntervals = arrayOf(500L, 1000L, 3000L, 5000L),
        legalExceptions = arrayOf(IOException::class, IllegalStateException::class),
    ) {
        val tssSamhandlerDatainput = XMLTssSamhandlerData().apply {
            tssInputData = XMLTssSamhandlerData.TssInputData().apply {
                tssServiceRutine = XMLTServicerutiner().apply {
                    samhandlerIDataB960 = XMLSamhandlerIDataB910Type().apply {
                        ofFid = XMLTidOFF1().apply {
                            idOff = samhandlerfnr
                            kodeIdType = setFnrOrDnr(samhandlerfnr)
                        }
                        historikk = "J"
                    }
                }
            }
        }

        val temporaryQueue = session.createTemporaryQueue()
        try {
            sendTssSporring(tssSamhnadlerInfoProducer, session, tssSamhandlerDatainput, temporaryQueue)
            session.createConsumer(temporaryQueue).use { tmpConsumer ->
                val consumedMessage = tmpConsumer.receive(20000) as TextMessage
                findEnkeltSamhandlerFromTSSRespons(tssSamhandlerdataUnmarshaller.unmarshal(StringReader(consumedMessage.text)) as XMLTssSamhandlerData)
            }
        } finally {
            temporaryQueue.delete()
        }
    }

fun sendTssSporring(
    producer: MessageProducer,
    session: Session,
    tssSamhandlerData: XMLTssSamhandlerData,
    temporaryQueue: TemporaryQueue,
) = producer.send(
    session.createTextMessage().apply {
        text = tssSamhandlerdataInputMarshaller.toString(tssSamhandlerData)
        jmsReplyTo = temporaryQueue
    },
)

fun findEnkeltSamhandlerFromTSSRespons(tssSamhandlerInfoResponse: XMLTssSamhandlerData): List<XMLTypeKomplett>? {
    return tssSamhandlerInfoResponse.tssOutputData?.samhandlerODataB960?.enkeltSamhandler
}

fun setFnrOrDnr(personNumber: String): String {
    return when (checkPersonNumberIsDnr(personNumber)) {
        true -> "DNR"
        else -> "FNR"
    }
}

fun checkPersonNumberIsDnr(personNumber: String): Boolean {
    val personNumberBornDay = personNumber.substring(0, 2)
    return validatePersonDNumberRange(personNumberBornDay)
}

fun validatePersonDNumberRange(personNumberFirstAndSecoundChar: String): Boolean {
    return personNumberFirstAndSecoundChar.toInt() in 41..71
}
