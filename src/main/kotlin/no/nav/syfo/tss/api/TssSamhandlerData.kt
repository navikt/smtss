package no.nav.syfo.tss.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.tss.service.findBestTssIdEmottak
import no.nav.syfo.tss.service.findBestTssInfotrygdId
import javax.jms.MessageProducer
import javax.jms.Session

fun Route.getTssId(
    tssProducer: MessageProducer,
    session: Session,
) {
    route("/api/v1") {
        get("samhandler/emottak/{samhandlerFnr}") {
            val samhandlerfnr = call.parameters["samhandlerFnr"]

            if (samhandlerfnr == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerFnr")
            } else {
                findBestTssIdEmottak(samhandlerfnr, tssProducer, session)
                call.respond("tssid")
            }
        }
        get("samhandler/infotrygd/{samhandlerFnr}") {
            val samhandlerfnr = call.parameters["samhandlerFnr"]

            if (samhandlerfnr == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerFnr")
            } else {
                findBestTssInfotrygdId(samhandlerfnr, tssProducer, session)
                call.respond("tssid")
            }
        }
    }
}
