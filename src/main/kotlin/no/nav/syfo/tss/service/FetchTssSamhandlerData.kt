package no.nav.syfo.tss.service

import jakarta.jms.Connection
import jakarta.jms.MessageProducer
import jakarta.jms.Session
import jakarta.jms.TemporaryQueue
import jakarta.jms.TextMessage
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource
import no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.helse.tss.samhandler.data.XMLSamhandlerIDataB910Type
import no.nav.helse.tss.samhandler.data.XMLTServicerutiner
import no.nav.helse.tss.samhandler.data.XMLTidOFF1
import no.nav.helse.tss.samhandler.data.XMLTssSamhandlerData
import no.nav.syfo.EnvironmentVariables
import no.nav.syfo.logger
import no.nav.syfo.mq.producerForQueue
import no.nav.syfo.objectMapper
import no.nav.syfo.redis.getTSSRespons
import no.nav.syfo.redis.saveTSSRespons
import no.nav.syfo.securelog
import no.nav.syfo.util.toString
import no.nav.syfo.util.tssSamhandlerdataInputMarshaller
import no.nav.syfo.util.tssSamhandlerdataUnmarshaller
import org.xml.sax.InputSource
import redis.clients.jedis.JedisPool

fun fetchTssSamhandlerData(
    samhandlerfnr: String,
    environmentVariables: EnvironmentVariables,
    jedisPool: JedisPool,
    requestId: String,
    connection: Connection
): List<XMLSamhandler>? {

    val fromRedis = getTSSRespons(jedisPool, samhandlerfnr)
    if (fromRedis != null) {
        logger.info("Fetched enkeltSamhandlerFromTSSRespons from redis for requestId: $requestId")
        return fromRedis.enkeltSamhandlerFromTSSRespons
    }
    val tssSamhandlerDatainput =
        XMLTssSamhandlerData().apply {
            tssInputData =
                XMLTssSamhandlerData.TssInputData().apply {
                    tssServiceRutine =
                        XMLTServicerutiner().apply {
                            samhandlerIDataB910 =
                                XMLSamhandlerIDataB910Type().apply {
                                    ofFid =
                                        XMLTidOFF1().apply {
                                            idOff = samhandlerfnr
                                            kodeIdType = setFnrOrDnr(samhandlerfnr)
                                        }
                                    historikk = "N"
                                }
                        }
                }
        }

    securelog.info(
        "Request to tss for requestId:$requestId requsesttss: ${objectMapper.writeValueAsString(tssSamhandlerDatainput)}"
    )

    connection.createSession(Session.CLIENT_ACKNOWLEDGE).use { session ->
        val temporaryQueue = session.createTemporaryQueue()

        val tssSamhnadlerInfoProducer =
            session.producerForQueue("queue:///${environmentVariables.tssQueue}?targetClient=1")

        try {
            sendTssSporring(
                tssSamhnadlerInfoProducer,
                session,
                tssSamhandlerDatainput,
                temporaryQueue
            )
            session.createConsumer(temporaryQueue).use { tmpConsumer ->
                val consumedMessage = tmpConsumer.receive(20000) as TextMessage
                return findEnkeltSamhandlerFromTSSRespons(
                        safeUnmarshal(consumedMessage.text),
                        requestId
                    )
                    .also {
                        logger.info(
                            "Fetched enkeltSamhandlerFromTSSRespons from tss for requestId: $requestId"
                        )
                        if (!it.isNullOrEmpty()) {
                            saveTSSRespons(jedisPool, samhandlerfnr, it)
                        }
                    }
            }
        } catch (exception: Exception) {
            logger.error(
                "An error occured while getting data from tss requestId: $requestId error message:, ${exception.message}"
            )
            throw exception
        } finally {
            temporaryQueue.delete()
        }
    }
}

fun sendTssSporring(
    producer: MessageProducer,
    session: Session,
    tssSamhandlerData: XMLTssSamhandlerData,
    temporaryQueue: TemporaryQueue,
) =
    producer.send(
        session.createTextMessage().apply {
            text = tssSamhandlerdataInputMarshaller.toString(tssSamhandlerData)
            jmsReplyTo = temporaryQueue
        },
    )

fun findEnkeltSamhandlerFromTSSRespons(
    tssSamhandlerInfoResponse: XMLTssSamhandlerData,
    requestId: String
): List<XMLSamhandler>? {
    securelog.info(
        "Response from tss for requestId:$requestId : ${
            objectMapper.writeValueAsString(
                tssSamhandlerInfoResponse
            )
        }"
    )
    return tssSamhandlerInfoResponse.tssOutputData?.samhandlerODataB910?.enkeltSamhandler
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

private fun safeUnmarshal(inputMessageText: String): XMLTssSamhandlerData {
    // Disable XXE
    val spf: SAXParserFactory = SAXParserFactory.newInstance()
    spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    spf.isNamespaceAware = true

    val xmlSource: Source =
        SAXSource(
            spf.newSAXParser().xmlReader,
            InputSource(StringReader(inputMessageText)),
        )
    return tssSamhandlerdataUnmarshaller.unmarshal(xmlSource) as XMLTssSamhandlerData
}
