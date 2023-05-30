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
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.mq.MqTlsUtils
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.redis.EnkeltSamhandlerFromTSSResponsRedis
import no.nav.syfo.tss.service.TssService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig


val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smtss")
val securelog: Logger = LoggerFactory.getLogger("securelog")

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

fun main() {
    val env = Environment()
    DefaultExports.initialize()
    val applicationState = ApplicationState()
    val serviceUser = ServiceUser()

    MqTlsUtils.getMqTlsConfig().forEach { key, value -> System.setProperty(key as String, value as String) }

    val jedisPool = JedisPool(JedisPoolConfig(), env.redisHost, env.redisPort)

    val enkeltSamhandlerFromTSSResponsRedis = EnkeltSamhandlerFromTSSResponsRedis(jedisPool, env.redisSecret)

    val connection =
        connectionFactory(env).createConnection(serviceUser.serviceuserUsername, serviceUser.serviceuserPassword)

    val tssService = TssService(env, enkeltSamhandlerFromTSSResponsRedis, connection)

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

    val applicationServer = ApplicationServer(applicationEngine, applicationState, connection)
    applicationServer.start()
}
