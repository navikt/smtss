package no.nav.syfo.application.metrics

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import io.ktor.util.pipeline.PipelineContext
import no.nav.syfo.securelog

val REGEX_FNR = """[0-9]{11}""".toRegex()

fun monitorHttpRequests(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return {
        val path = context.request.path()
        securelog.info("path is $path")
        val label = REGEX_FNR.replace(path, ":samhandlerFnr")
        val timer = HTTP_HISTOGRAM.labels(label).startTimer()
        proceed()
        timer.observeDuration()
    }
}
