package no.nav.syfo.redis

import java.time.OffsetDateTime
import no.nav.helse.tssSamhandlerData.XMLTypeKomplett

data class JedisEnkeltSamhandlerModel(
    val timestamp: OffsetDateTime,
    val enkeltSamhandlerFromTSSRespons: List<XMLTypeKomplett>?
)
