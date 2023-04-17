package no.nav.syfo.redis

import no.nav.helse.tssSamhandlerData.XMLSamhandler

data class JedisEnkeltSamhandlerFromTSSResponsModel(
    val enkeltSamhandlerFromTSSRespons: List<XMLSamhandler>?
)
