package no.nav.syfo.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

const val METRICS_NS = "smtss"

val HTTP_HISTOGRAM: Histogram =
    Histogram.Builder()
        .namespace(METRICS_NS)
        .labelNames("path")
        .name("requests_duration_seconds")
        .help("http requests durations for incoming requests in seconds")
        .register()
val AUTH_AZP_NAME: Counter =
    Counter.build()
        .namespace(METRICS_NS)
        .name("auth_azp_name")
        .labelNames("name")
        .help("Counts the the Human-readable client name of azp_name claim")
        .register()
