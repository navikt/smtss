package no.nav.syfo.tss.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helse.tss.samhandler.data.XMLSamhandler
import no.nav.syfo.logger
import no.nav.syfo.texas.auth.TexasAuth
import no.nav.syfo.texas.client.TexasClient
import no.nav.syfo.tss.service.TSSident
import no.nav.syfo.tss.service.TssService

val samhandlerMapper: ObjectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
    }

fun Route.getTssId(
    tssService: TssService,
    texasClient: TexasClient,
) {
    route("/api/v1") {
        install(TexasAuth) { client = texasClient }
        get("samhandler/emottak") {
            val samhandlerfnr = call.request.headers["samhandlerFnr"]
            val samhandlerOrgName = call.request.headers["samhandlerOrgName"]
            val requestid = call.request.headers["requestId"]
            val orgnummer = call.request.headers["samhandlerOrgnummer"]

            if (samhandlerfnr == null) {
                logger.warn("Missing samhandlerFnr in header for requestid $requestid")
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerFnr in header")
            } else if (samhandlerOrgName == null) {
                logger.warn("Missing samhandlerOrgName in header for requestid $requestid")
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerOrgName in header")
            } else if (requestid == null) {
                logger.warn("Missing requestId in header")
                call.respond(HttpStatusCode.BadRequest, "Missing requestId in header")
            } else {
                try {
                    logger.info(
                        "Trying to find best tssid for emottak samhandlerOrgName: $samhandlerOrgName, orgnummer: $orgnummer and requestid: $requestid"
                    )
                    val tssIdent: TSSident? =
                        tssService.findBestTssIdEmottak(
                            samhandlerfnr,
                            samhandlerOrgName,
                            requestid,
                            orgnummer
                        )
                    if (tssIdent != null) {
                        call.respond(HttpStatusCode.OK, tssIdent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } catch (exception: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                    throw exception
                }
            }
        }
        get("samhandler/infotrygd") {
            val samhandlerfnr = call.request.headers["samhandlerFnr"]
            val samhandlerOrgName = call.request.headers["samhandlerOrgName"]
            val requestid = call.request.headers["requestId"]

            if (samhandlerfnr == null) {
                logger.warn("Missing samhandlerFnr in header")
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerFnr in header")
            } else if (samhandlerOrgName == null) {
                logger.warn("Missing samhandlerOrgName in header")
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerOrgName in header")
            } else if (requestid == null) {
                logger.warn("Missing requestId in header")
                call.respond(HttpStatusCode.BadRequest, "Missing requestId in header")
            } else {
                try {
                    logger.info(
                        "Trying to find best tssid for infotrygd samhandlerOrgName: $samhandlerOrgName and requestid: $requestid"
                    )
                    val tssIdent: TSSident? =
                        tssService.findBestTssIdInfotrygd(
                            samhandlerfnr,
                            samhandlerOrgName,
                            requestid
                        )
                    if (tssIdent != null) {
                        call.respond(HttpStatusCode.OK, tssIdent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } catch (exception: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                    throw exception
                }
            }
        }
        get("samhandler/arena") {
            val samhandlerfnr = call.request.headers["samhandlerFnr"]
            val samhandlerOrgName = call.request.headers["samhandlerOrgName"]
            val requestid = call.request.headers["requestId"]
            val orgnummer = call.request.headers["samhandlerOrgnummer"]
            if (samhandlerfnr == null) {
                logger.warn("Missing samhandlerFnr in header")
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerFnr in header")
            } else if (samhandlerOrgName == null) {
                logger.warn("Missing samhandlerOrgName in header")
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerOrgName in header")
            } else if (requestid == null) {
                logger.warn("Missing requestId in header")
                call.respond(HttpStatusCode.BadRequest, "Missing requestId in header")
            } else {
                try {
                    logger.info(
                        "Trying to find best tssid for arena samhandlerOrgName: $samhandlerOrgName and requestid: $requestid"
                    )
                    val tssIdent: TSSident? =
                        tssService.findBestTssIdArena(
                            samhandlerfnr,
                            samhandlerOrgName,
                            requestid,
                            orgnummer
                        )
                    if (tssIdent != null) {
                        call.respond(HttpStatusCode.OK, tssIdent)
                    } else {
                        call.respond(HttpStatusCode.NotFound).also {
                            logger.info("Did not find tssIdent for requestid: $requestid")
                        }
                    }
                } catch (exception: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                    throw exception
                }
            }
        }
        get("samhandler/inst") {
            val requestid = call.request.headers["requestId"]
            val orgnummer = call.request.headers["samhandlerOrgnummer"]
            val samhandlerIdType = call.request.headers["samhandlerIdType"]
            if (orgnummer == null) {
                logger.warn("Missing samhandler orgnummer in header")
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerFnr in header")
            } else if (samhandlerIdType == null) {
                logger.warn("Missing samhandlerOrgName in header")
                call.respond(HttpStatusCode.BadRequest, "Missing samhandlerOrgName in header")
            } else if (requestid == null) {
                logger.warn("Missing requestId in header")
                call.respond(HttpStatusCode.BadRequest, "Missing requestId in header")
            } else {
                try {
                    logger.info(
                        "Trying to find best tssid for arena samhandlerOrg for request: $requestid"
                    )
                    val xmlSamhandlere: List<XMLSamhandler>? =
                        tssService.getSamhandlerInst(
                            orgnummer,
                            requestid,
                            samhandlerIdType,
                        )

                    call.respond(
                        HttpStatusCode.OK,
                        samhandlerMapper.writeValueAsString(xmlSamhandlere)
                    )
                } catch (exception: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                    throw exception
                }
            }
        }
    }
}
