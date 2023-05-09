package no.nav.syfo.tss.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.log
import no.nav.syfo.tss.service.TSSident
import no.nav.syfo.tss.service.TssService

fun Route.getTssId(
    tssService: TssService,
) {
    route("/api/v1") {
        get("samhandler/emottak") {

            val samhandlerfnr = call.request.headers["samhandlerFnr"]
            val samhandlerOrgName = call.request.headers["samhandlerOrgName"]
            val requestid = call.request.headers["requestId"]

            if (samhandlerfnr == null) {
                log.warn("Missing samhandlerFnr in header for requestid $requestid")
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerFnr in header")
            } else if (samhandlerOrgName == null) {
                log.warn("Missing samhandlerOrgName in header for requestid $requestid")
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerOrgName in header")
            } else if (requestid == null) {
                log.warn("Missing requestId in header")
                call.respond(HttpStatusCode.BadRequest, "Missing requestId in header")
            }
            else {
                log.info("Trying to find best tssid for emottak samhandlerOrgName: $samhandlerOrgName and requestid: $requestid")
                val tssIdent: TSSident? = tssService.findBestTssIdEmottak(samhandlerfnr, samhandlerOrgName, requestid)
                if (tssIdent != null) {
                    call.respond(HttpStatusCode.OK, tssIdent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }


        }
        get("samhandler/infotrygd") {

            val samhandlerfnr = call.request.headers["samhandlerFnr"]
            val samhandlerOrgName = call.request.headers["samhandlerOrgName"]
            val requestid = call.request.headers["requestId"]

            if (samhandlerfnr == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerFnr in header")
            } else if (samhandlerOrgName == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerOrgName in header")
            } else if (requestid == null) {
                log.warn("Missing requestId in header")
                call.respond(HttpStatusCode.BadRequest, "Missing requestId in header")
            }
            else {
                log.info("Trying to find best tssid for infotrygd samhandlerOrgName: $samhandlerOrgName and requestid: $requestid")
                val tssIdent: TSSident? =  tssService.findBestTssInfotrygdId(samhandlerfnr, samhandlerOrgName, requestid)
                if (tssIdent != null) {
                    call.respond(HttpStatusCode.OK, tssIdent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
