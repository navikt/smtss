package no.nav.syfo.tss.service

import java.io.StringReader
import javax.jms.MessageProducer
import javax.jms.Session
import javax.jms.TemporaryQueue
import javax.jms.TextMessage
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource
import no.nav.helse.tssSamhandlerData.XMLSamhandler
import no.nav.helse.tssSamhandlerData.XMLSamhandlerIDataB910Type
import no.nav.helse.tssSamhandlerData.XMLTServicerutiner
import no.nav.helse.tssSamhandlerData.XMLTidOFF1
import no.nav.helse.tssSamhandlerData.XMLTssSamhandlerData
import no.nav.syfo.Environment
import no.nav.syfo.ServiceUser
import no.nav.syfo.log
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.mq.producerForQueue
import no.nav.syfo.objectMapper
import no.nav.syfo.redis.EnkeltSamhandlerFromTSSResponsRedis
import no.nav.syfo.securelog
import no.nav.syfo.util.toString
import no.nav.syfo.util.tssSamhandlerdataInputMarshaller
import no.nav.syfo.util.tssSamhandlerdataUnmarshaller
import org.xml.sax.InputSource


fun fetchTssSamhandlerData(
    samhandlerfnr: String,
    environment: Environment,
    serviceUser: ServiceUser,
    enkeltSamhandlerFromTSSResponsRedis: EnkeltSamhandlerFromTSSResponsRedis
): List<XMLSamhandler>? {

    val fromRedis = enkeltSamhandlerFromTSSResponsRedis.get(samhandlerfnr)
    if (fromRedis != null) {
        log.info("Fetched enkeltSamhandlerFromTSSRespons from redis")
        return fromRedis.enkeltSamhandlerFromTSSRespons
    }
    val tssSamhandlerDatainput = XMLTssSamhandlerData().apply {
        tssInputData = XMLTssSamhandlerData.TssInputData().apply {
            tssServiceRutine = XMLTServicerutiner().apply {
                samhandlerIDataB910 = XMLSamhandlerIDataB910Type().apply {
                    ofFid = XMLTidOFF1().apply {
                        idOff = samhandlerfnr
                        kodeIdType = setFnrOrDnr(samhandlerfnr)
                    }
                    historikk = "N"
                }
            }
        }
    }

    securelog.info("Request to tss: ${objectMapper.writeValueAsString(tssSamhandlerDatainput)}")

    connectionFactory(environment).createConnection(serviceUser.serviceuserUsername, serviceUser.serviceuserPassword)
        .use { connection ->
            connection.start()
            val session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE)

            val tssSamhnadlerInfoProducer = session.producerForQueue("queue:///${environment.tssQueue}?targetClient=1")


            val temporaryQueue = session.createTemporaryQueue()
            try {
                securelog.info("xml request to tss ${tssSamhandlerdataInputMarshaller.toString(tssSamhandlerDatainput)}")
                sendTssSporring(tssSamhnadlerInfoProducer, session, tssSamhandlerDatainput, temporaryQueue)
                session.createConsumer(temporaryQueue).use { tmpConsumer ->
                    val consumedMessage = tmpConsumer.receive(20000) as TextMessage
                    return findEnkeltSamhandlerFromTSSRespons( safeUnmarshal(consumedMessage.text)).also {
                        log.info("Fetched enkeltSamhandlerFromTSSRespons from tss")
                        if (!it.isNullOrEmpty()) {
                            enkeltSamhandlerFromTSSResponsRedis.save(samhandlerfnr, it)
                        }
                    }
                }
            } catch (exception: Exception) {
                log.warn("An error occured while getting data from tss, ${exception.message}")
                return emptyList()
            } finally {
                temporaryQueue.delete()
            }
        }
}

fun safeUnmarshal(consumedMessageText: String): XMLTssSamhandlerData {
    securelog.info("the consumedMessageText: $consumedMessageText")
    //Disable XXE
    val spf = SAXParserFactory.newInstance()
    spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)

    val xmlSource: Source = SAXSource(
        spf.newSAXParser().xmlReader,
        InputSource(StringReader(consumedMessageText))
    )
    return tssSamhandlerdataUnmarshaller.unmarshal(xmlSource) as XMLTssSamhandlerData
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

fun findEnkeltSamhandlerFromTSSRespons(tssSamhandlerInfoResponse: XMLTssSamhandlerData): List<XMLSamhandler>? {
    securelog.info("Response to tss: ${objectMapper.writeValueAsString(tssSamhandlerInfoResponse)}")
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
