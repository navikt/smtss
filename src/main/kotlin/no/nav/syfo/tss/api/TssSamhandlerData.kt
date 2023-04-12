package no.nav.syfo.tss.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.tss.service.TSSident
import no.nav.syfo.tss.service.TssService

fun Route.getTssId(
    tssService: TssService,
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

                val tssIdent: TSSident? = tssService.findBestTssIdEmottak(samhandlerfnr, samhandlerOrgName, tssQueue)
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
                val tssIdent: TSSident? =  tssService.findBestTssInfotrygdId(samhandlerfnr, tssQueue)
                if (tssIdent != null) {
                    call.respond(HttpStatusCode.OK, tssIdent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
