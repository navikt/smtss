package no.nav.syfo.util

import no.nav.helse.tssSamhandlerData.XMLTssSamhandlerData
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.stream.XMLInputFactory

val tssSamhandlerdataInputJaxBContext: JAXBContext = JAXBContext.newInstance(XMLTssSamhandlerData::class.java)
val tssSamhandlerdataInputMarshaller: Marshaller = tssSamhandlerdataInputJaxBContext.createMarshaller()
val tssSamhandlerdataUnmarshaller: Unmarshaller = tssSamhandlerdataInputJaxBContext.createUnmarshaller().apply {
    setProperty(XMLInputFactory.SUPPORT_DTD, false);
}

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}
