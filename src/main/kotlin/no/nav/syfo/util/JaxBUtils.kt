package no.nav.syfo.util

import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import no.nav.helse.tss.samhandler.data.XMLTssSamhandlerData

// JAXBContext is thread-safe and expensive to create, so it is shared. Marshaller and Unmarshaller
// are NOT thread-safe and must never be shared between concurrent requests, so a fresh instance is
// created for every call.
val tssSamhandlerdataInputJaxBContext: JAXBContext =
    JAXBContext.newInstance(XMLTssSamhandlerData::class.java)

fun tssSamhandlerdataInputMarshaller(): Marshaller =
    tssSamhandlerdataInputJaxBContext.createMarshaller()

fun tssSamhandlerdataUnmarshaller(): Unmarshaller =
    tssSamhandlerdataInputJaxBContext.createUnmarshaller()

fun Marshaller.toString(input: Any): String =
    StringWriter().use {
        marshal(input, it)
        it.toString()
    }
