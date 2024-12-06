package no.nav.syfo.texas.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.serialization.jackson.jackson
import no.nav.syfo.logger

class TexasClient(
    private val introspectionEndpointURL: String,
) {

    val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, _ ->
                when (exception) {
                    is SocketTimeoutException ->
                        throw ServiceUnavailableException(exception.message)
                }
            }
        }
        install(HttpRequestRetry) {
            constantDelay(50, 0, false)
            retryOnExceptionIf(3) { request, throwable ->
                logger.warn("Caught exception ${throwable.message}, for url ${request.url}")
                true
            }
            retryIf(maxRetries) { request, response ->
                if (response.status.value.let { it in 500..599 }) {
                    logger.warn(
                        "Retrying for statuscode ${response.status.value}, for url ${request.url}"
                    )
                    true
                } else {
                    false
                }
            }
        }
        install(HttpTimeout) {
            socketTimeoutMillis = 20_000
            connectTimeoutMillis = 20_000
            requestTimeoutMillis = 20_000
        }
    }

    val httpClient = HttpClient(Apache, config)

    suspend fun introspection(identityProvider: String, token: String): TexasIntrospectionResponse {
        val texasIntrospection = TexasIntrospectionRequest(identityProvider, token)

        val response = httpClient.post(introspectionEndpointURL) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(texasIntrospection)
        }

        logger.info("Texas introspection responded with statuscode: ${response.status}")

        return response.body<TexasIntrospectionResponse>()
    }
}

data class TexasIntrospectionRequest(
    @JsonProperty("identity_provider") val identityProvider: String,
    val token: String
)

data class TexasIntrospectionResponse(
    val active: Boolean,
    val error: String?,
    val other: Map<String, Any?> = mutableMapOf(),
)

class ServiceUnavailableException(message: String?) : Exception(message)
