package no.nav.syfo.tss.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
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

            val samhandlerfnr = call.parameters["samhandlerFnr"]
            val samhandlerOrgName = call.parameters["samhandlerOrgName"]
            if (samhandlerfnr == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerFnr")
            } else if (samhandlerOrgName == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerOrgName")
            } else {

                val tssIdent: TSSident? = findBestTssIdEmottak(samhandlerfnr, samhandlerOrgName, connection, tssQueue)
                if (tssIdent != null) {
                    call.respond(HttpStatusCode.OK, tssIdent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }


        }
        get("samhandler/infotrygd") {


            val samhandlerfnr = call.parameters["samhandlerFnr"]
            val samhandlerOrgName = call.parameters["samhandlerOrgName"]
            if (samhandlerfnr == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerFnr")
            } else if (samhandlerOrgName == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerOrgName")
            } else {
                val tssIdent: TSSident? = findBestTssInfotrygdId(samhandlerfnr, connection, tssQueue)
                if (tssIdent != null) {
                    call.respond(HttpStatusCode.OK, tssIdent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
