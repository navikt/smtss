package no.nav.syfo.application

import io.ktor.server.engine.ApplicationEngine
import java.util.concurrent.TimeUnit
import javax.jms.Connection

class ApplicationServer(
    private val applicationServer: ApplicationEngine,
    private val applicationState: ApplicationState,
    private val connection: Connection
) {
    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                this.connection.close()
                this.applicationState.ready = false
                this.applicationState.alive = false
                this.applicationServer.stop(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10))
            },
        )
    }

    fun start() {
        connection.start()
        applicationState.alive = true
        applicationState.ready = true
        applicationServer.start(true)
    }
}
