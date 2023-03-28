package no.nav.syfo

import com.ibm.mq.MQEnvironment
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.mq.MqTlsUtils
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.mq.producerForQueue
import no.nav.syfo.util.TrackableException
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

    MQEnvironment.channel = env.mqChannelName
    MQEnvironment.port = env.mqPort
    MQEnvironment.hostname = env.mqHostname
    MQEnvironment.userID = serviceUser.serviceuserUsername
    MQEnvironment.password = serviceUser.serviceuserPassword

    launchApplication(
        applicationState,
        env,
        serviceUser
    )
}

@OptIn(DelicateCoroutinesApi::class)
fun createApplication(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (e: TrackableException) {
            log.error(
                "An unhandled error occurred, the application restarts {}",
                StructuredArguments.fields(e.loggingMeta), e.cause
            )
        } finally {
            applicationState.alive = false
            applicationState.ready = false
        }
    }

fun launchApplication(
    applicationState: ApplicationState,
    env: Environment,
    serviceUser: ServiceUser,
) {
    createApplication(applicationState) {
        connectionFactory(env).createConnection(serviceUser.serviceuserUsername, serviceUser.serviceuserPassword)
            .use { connection ->
                connection.start()
                val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                val tssProducer = session.producerForQueue("queue:///${env.tssQueue}?targetClient=1")

                val applicationEngine = createApplicationEngine(
                    env,
                    applicationState,
                    tssProducer,
                    session
                )

                val applicationServer = ApplicationServer(applicationEngine, applicationState)
                applicationServer.start()
            }
    }
}
