package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import no.nav.syfo.Environment
import no.nav.syfo.application.api.registerNaisApi
import no.nav.syfo.application.authentication.setupAuth
import no.nav.syfo.log
import no.nav.syfo.tss.api.getTssId
import no.nav.syfo.tss.service.TssService

fun createApplicationEngine(
    env: Environment,
    applicationState: ApplicationState,
    tssService: TssService,
    jwkProviderAad: JwkProvider,
): ApplicationEngine =
    embeddedServer(Netty, env.applicationPort) {
        setupAuth(
            environment = env,
            jwkProviderAadV2 = jwkProviderAad,
        )
        routing {
            registerNaisApi(applicationState)
            authenticate("servicebrukerAAD") {
                getTssId(tssService)
            }
            swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        }
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                log.warn("Caught exception", cause)
                call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
                throw cause
            }
        }
    }
