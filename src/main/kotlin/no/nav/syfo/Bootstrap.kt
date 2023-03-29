package no.nav.syfo

import io.prometheus.client.hotspot.DefaultExports
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.mq.MqTlsUtils
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.mq.producerForQueue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.jms.Session

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smtss")

fun main() {
    val env = Environment()
    DefaultExports.initialize()
    val applicationState = ApplicationState()
    val serviceUser = ServiceUser()
    MqTlsUtils.getMqTlsConfig().forEach { key, value -> System.setProperty(key as String, value as String) }

    connectionFactory(env).createConnection(serviceUser.serviceuserUsername, serviceUser.serviceuserPassword)
        .use { connection ->
            connection.start()
            val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val tssProducer = session.producerForQueue("queue:///${env.tssQueue}?targetClient=1")

            val applicationEngine = createApplicationEngine(
                env,
                applicationState,
                tssProducer,
                session,
            )

            val applicationServer = ApplicationServer(applicationEngine, applicationState)
            log.info("ApplicationServer ready to start")
            applicationServer.start()
        }
}
