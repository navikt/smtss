package no.nav.syfo

import io.ktor.http.*
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.prometheus.client.hotspot.DefaultExports
import no.nav.syfo.metrics.monitorHttpRequests
import no.nav.syfo.mq.MqTlsUtils
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.nais.isalive.naisIsAliveRoute
import no.nav.syfo.nais.isready.naisIsReadyRoute
import no.nav.syfo.nais.prometheus.naisPrometheusRoute
import no.nav.syfo.texas.client.TexasClient
import no.nav.syfo.tss.api.getTssId
import no.nav.syfo.tss.service.TssService
import no.nav.syfo.util.createJedisPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

val logger: Logger = LoggerFactory.getLogger("no.nav.syfo.smtss")
val securelog: Logger = LoggerFactory.getLogger("securelog")

val jsonMapper: JsonMapper = jacksonMapperBuilder().build()

fun main() {
    val embeddedServer =
        embeddedServer(
            Netty,
            port = EnvironmentVariables().applicationPort,
            module = Application::module,
        )
    embeddedServer.start(true)
}

fun Application.configureRouting(
    applicationState: ApplicationState,
    tssService: TssService,
    texasClient: TexasClient,
) {
    routing {
        naisIsAliveRoute(applicationState)
        naisIsReadyRoute(applicationState)
        naisPrometheusRoute()
        getTssId(tssService, texasClient)
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    }

    install(ContentNegotiation) { jackson {} }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Caught exception ${cause.message}")
            securelog.error("Caught exception", cause)
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
            applicationState.alive = false
            applicationState.ready = false
        }
    }

    intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())
}

fun Application.module() {
    val applicationState = ApplicationState()
    val environmentVariables = EnvironmentVariables()
    val serviceUser = ServiceUser()

    MqTlsUtils.getMqTlsConfig().forEach { key, value ->
        System.setProperty(key as String, value as String)
    }

    val jedisPool = createJedisPool()

    val connection =
        connectionFactory(environmentVariables)
            .createConnection(serviceUser.serviceuserUsername, serviceUser.serviceuserPassword)

    val tssService = TssService(environmentVariables, jedisPool, connection)
    val texasClient = TexasClient(environmentVariables.texasIntrospectionEndpoint)

    monitor.subscribe(ApplicationStopPreparing) {
        logger.info("Got ApplicationStopped event from ktor")
        applicationState.ready = false
        connection?.close()
    }

    configureRouting(
        applicationState = applicationState,
        tssService = tssService,
        texasClient = texasClient,
    )

    connection.start()

    DefaultExports.initialize()
}

data class ApplicationState(var alive: Boolean = true, var ready: Boolean = true)
