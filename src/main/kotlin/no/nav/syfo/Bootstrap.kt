package no.nav.syfo

import io.prometheus.client.hotspot.DefaultExports
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.mq.connectionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smtss")

fun main() {
    val env = Environment()
    DefaultExports.initialize()
    val applicationState = ApplicationState()
    val serviceUser = ServiceUser()

    val connection = connectionFactory(env).apply {
        sslSocketFactory = null
        sslCipherSuite = null
    }.createConnection(serviceUser.serviceuserUsername, serviceUser.serviceuserPassword)

    connection.start()

    val applicationEngine = createApplicationEngine(
        env,
        applicationState,
        connection,
    )

    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
}
