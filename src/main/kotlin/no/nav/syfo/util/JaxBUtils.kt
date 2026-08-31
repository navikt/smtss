package no.nav.syfo.util

import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import no.nav.helse.tss.samhandler.data.XMLTssSamhandlerData

val tssSamhandlerdataInputJaxBContext: JAXBContext =
    JAXBContext.newInstance(XMLTssSamhandlerData::class.java)
val tssSamhandlerdataInputMarshaller: Marshaller =
    tssSamhandlerdataInputJaxBContext.createMarshaller()

fun Marshaller.toString(input: Any): String =
    StringWriter().use {
        marshal(input, it)
        it.toString()
    }
