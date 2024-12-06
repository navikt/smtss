package no.nav.syfo.texas.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.authorization
import io.ktor.server.response.respond
import no.nav.syfo.logger
import no.nav.syfo.metrics.AUTH_AZP_NAME
import no.nav.syfo.texas.client.TexasClient

class AuthPluginConfiguration(
    var client: TexasClient? = null,
)

val TexasAuth =
    createRouteScopedPlugin(
        name = "TexasAuth",
        createConfiguration = ::AuthPluginConfiguration,
    ) {
        val client =
            pluginConfig.client
                ?: throw IllegalArgumentException("TexasAuth plugin: client must be set")

        pluginConfig.apply {
            onCall { call ->
                val token = call.bearerToken()
                if (token == null) {
                    logger.warn("unauthenticated: no Bearer token found in Authorization header")
                    call.respond(HttpStatusCode.Unauthorized)
                    return@onCall
                }

                val introspectResponse =
                    try {
                        client.introspection("azuread", token)
                    } catch (e: Exception) {
                        logger.error("unauthenticated: introspect request failed: ${e.message}")
                        call.respond(HttpStatusCode.Unauthorized)
                        return@onCall
                    }

                if (introspectResponse.active == false) {
                    logger.warn("unauthenticated: ${introspectResponse.error}")
                    call.respond(HttpStatusCode.Unauthorized)
                    return@onCall
                }

                logger.info("authenticated - claims='${introspectResponse.other}'")

                if (introspectResponse.other["azp_name"]?.toString() != null) {
                    val azpName: String = introspectResponse.other["azp_name"].toString()
                    AUTH_AZP_NAME.labels(azpName).inc()
                }
            }
        }

        logger.info("TexasAuth plugin loaded.")
    }

fun ApplicationCall.bearerToken(): String? =
    request
        .authorization()
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.removePrefix("Bearer ")
        ?.removePrefix("bearer ")
