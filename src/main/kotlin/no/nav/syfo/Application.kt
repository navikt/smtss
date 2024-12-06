package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.prometheus.client.hotspot.DefaultExports
import java.util.concurrent.TimeUnit
import no.nav.syfo.metrics.monitorHttpRequests
import no.nav.syfo.mq.MqTlsUtils
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.nais.isalive.naisIsAliveRoute
import no.nav.syfo.nais.isready.naisIsReadyRoute
import no.nav.syfo.nais.prometheus.naisPrometheusRoute
import no.nav.syfo.texas.auth.TexasAuth
import no.nav.syfo.texas.client.TexasClient
import no.nav.syfo.tss.api.getTssId
import no.nav.syfo.tss.service.TssService
import no.nav.syfo.util.createJedisPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("no.nav.syfo.smtss")
val securelog: Logger = LoggerFactory.getLogger("securelog")

val objectMapper: ObjectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

fun main() {
    val embeddedServer =
        embeddedServer(
            Netty,
            port = EnvironmentVariables().applicationPort,
            module = Application::module,
        )
    Runtime.getRuntime()
        .addShutdownHook(
            Thread {
                logger.info("Shutting down application from shutdown hook")
                embeddedServer.stop(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10))
            },
        )
    embeddedServer.start(true)
}

fun Application.configureRouting(
    applicationState: ApplicationState,
    tssService: TssService,
    texasClient: TexasClient
) {
    setupAuth(texasClient = texasClient)

    routing {
        naisIsAliveRoute(applicationState)
        naisIsReadyRoute(applicationState)
        naisPrometheusRoute()
        authenticate("TexasAuth") { getTssId(tssService) }
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
            logger.error("Caught exception ${cause.message}")
            securelog.error("Caught exception", cause)
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
            applicationState.alive = false
            applicationState.ready = false
        }
    }

    intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())
}

fun Application.setupAuth(texasClient: TexasClient) {
    install(TexasAuth) { client = texasClient }
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

    monitor.subscribe(ApplicationStopped) {
        logger.info("Got ApplicationStopped event from ktor")
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

data class ApplicationState(
    var alive: Boolean = true,
    var ready: Boolean = true,
)
