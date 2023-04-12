package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.prometheus.client.hotspot.DefaultExports
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.jms.Session
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.mq.MqTlsUtils
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.tss.service.TssService
import no.nav.syfo.util.TrackableException
import org.slf4j.Logger
import org.slf4j.LoggerFactory


val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smtss")
val securelog: Logger = LoggerFactory.getLogger("securelog")

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    val env = Environment()
    DefaultExports.initialize()
    val applicationState = ApplicationState()
    val serviceUser = ServiceUser()

    MqTlsUtils.getMqTlsConfig().forEach { key, value -> System.setProperty(key as String, value as String) }

    GlobalScope.launch {
        try {
            connectionFactory(env).createConnection(serviceUser.serviceuserUsername, serviceUser.serviceuserPassword)
                .use { connection ->
                    connection.start()
                    val session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE)

                    val tssService = TssService(session)

                    val jwkProviderAad = JwkProviderBuilder(URL(env.jwkKeysUrl))
                        .cached(10, 24, TimeUnit.HOURS)
                        .rateLimited(10, 1, TimeUnit.MINUTES)
                        .build()

                    val applicationEngine = createApplicationEngine(
                        env,
                        applicationState,
                        tssService,
                        jwkProviderAad,
                    )

                    val applicationServer = ApplicationServer(applicationEngine, applicationState)
                    applicationServer.start()


                }
        } catch (e: TrackableException) {
            log.error("An unhandled error occurred, the application restarts", e.cause)
        } finally {
            applicationState.ready = false
            applicationState.alive = false
        }
    }


}
