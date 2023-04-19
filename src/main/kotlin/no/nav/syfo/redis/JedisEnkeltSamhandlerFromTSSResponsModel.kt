package no.nav.syfo.redis

import no.nav.helse.tss.samhandler.data.XMLSamhandler

data class JedisEnkeltSamhandlerFromTSSResponsModel(
    val enkeltSamhandlerFromTSSRespons: List<XMLSamhandler>?
)
