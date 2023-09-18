package no.nav.syfo

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.prometheus.client.hotspot.DefaultExports
import java.net.URL
import java.util.concurrent.TimeUnit
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.metrics.monitorHttpRequests
import no.nav.syfo.mq.MqTlsUtils
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.nais.isalive.naisIsAliveRoute
import no.nav.syfo.nais.isready.naisIsReadyRoute
import no.nav.syfo.nais.prometheus.naisPrometheusRoute
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
    environmentVariables: EnvironmentVariables,
    tssService: TssService,
    jwkProviderAadV2: JwkProvider,
) {
    setupAuth(
        environmentVariables = environmentVariables,
        jwkProviderAadV2 = jwkProviderAadV2,
    )

    routing {
        naisIsAliveRoute(applicationState)
        naisIsReadyRoute(applicationState)
        naisPrometheusRoute()
        authenticate("servicebrukerAAD") { getTssId(tssService) }
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

fun Application.setupAuth(
    environmentVariables: EnvironmentVariables,
    jwkProviderAadV2: JwkProvider,
) {
    install(Authentication) {
        jwt(name = "servicebrukerAAD") {
            verifier(jwkProviderAadV2, environmentVariables.jwtIssuer)
            validate { credentials ->
                when {
                    hasAccsess(credentials, environmentVariables.clientIdV2) ->
                        JWTPrincipal(credentials.payload)
                    else -> unauthorized(credentials)
                }
            }
        }
    }
}

fun hasAccsess(credentials: JWTCredential, clientId: String): Boolean {
    val appid: String = credentials.payload.getClaim("azp").asString()
    securelog.info("authorization attempt for $appid")
    return credentials.payload.audience.contains(clientId)
}

fun unauthorized(credentials: JWTCredential): Principal? {
    logger.error(
        "Auth: Unexpected audience for jwt {}, {}",
        StructuredArguments.keyValue("issuer", credentials.payload.issuer),
        StructuredArguments.keyValue("audience", credentials.payload.audience),
    )
    return null
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

    val jwkProviderAad =
        JwkProviderBuilder(URL(environmentVariables.jwkKeysUrl))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    environment.monitor.subscribe(ApplicationStopped) {
        logger.info("Got ApplicationStopped event from ktor")
        connection?.close()
    }

    configureRouting(
        applicationState = applicationState,
        environmentVariables = environmentVariables,
        jwkProviderAadV2 = jwkProviderAad,
        tssService = tssService,
    )

    connection.start()

    DefaultExports.initialize()
}

data class ApplicationState(
    var alive: Boolean = true,
    var ready: Boolean = true,
)
