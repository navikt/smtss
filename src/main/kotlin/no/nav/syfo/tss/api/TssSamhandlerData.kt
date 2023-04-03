package no.nav.syfo.tss.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.log
import no.nav.syfo.tss.service.TSSident
import no.nav.syfo.tss.service.findBestTssIdEmottak
import no.nav.syfo.tss.service.findBestTssInfotrygdId
import javax.jms.Connection

fun Route.getTssId(
    connection: Connection,
    tssQueue: String,
) {
    route("/api/v1") {
        get("samhandler/emottak") {

            val tssidentRequest = call.receive<TSSidentRequest>()
            val samhandlerfnr = tssidentRequest.samhandlerFnr
            val samhandlerOrgName = tssidentRequest.samhandlerOrgName

            val tssIdent: TSSident? = findBestTssIdEmottak(samhandlerfnr, samhandlerOrgName, connection, tssQueue)
            if (tssIdent != null) {
                log.info("tssIdent is: $tssIdent")
                call.respond(HttpStatusCode.OK, tssIdent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }


        }
        get("samhandler/infotrygd") {
            val tssidentRequest = call.receive<TSSidentRequest>()

            val tssIdent: TSSident? = findBestTssInfotrygdId(tssidentRequest.samhandlerFnr, connection, tssQueue)
            if (tssIdent != null) {
                call.respond(HttpStatusCode.OK, tssIdent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

data class TSSidentRequest(
    val samhandlerFnr: String,
    val samhandlerOrgName: String
)