package no.nav.syfo.tss.api

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.text.ParseException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import no.nav.syfo.logger
import no.nav.syfo.texas.client.TexasClient
import no.nav.syfo.texas.client.TexasIntrospectionResponse
import no.nav.syfo.tss.service.TSSident
import no.nav.syfo.tss.service.TssService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class TssSamhandlerDataTest {

    private val tssService =
        mockk<TssService> {
            coEvery { findBestTssIdEmottak(any(), any(), any(), any()) } returns
                TSSident(tssid = "232313311")
        }
    private val texasClient =
        mockk<TexasClient> {
            coEvery { introspection(any(), any()) } returns
                TexasIntrospectionResponse(
                    active = true,
                    error = "",
                    other = mutableMapOf("azp_name" to "sadasd")
                )
        }

    private val keyId = "localhost-signer"

    @Test
    internal fun `Should return ok status`() {

        testApplication {
            application {
                routing { getTssId(tssService, texasClient) }

                install(ContentNegotiation) {
                    jackson {
                        registerKotlinModule()
                        registerModule(JavaTimeModule())
                        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    }
                }
                install(StatusPages) {
                    exception<Throwable> { call, cause ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            cause.message ?: "Unknown error",
                        )
                        logger.error("Caught exception", cause)
                        throw cause
                    }
                }
            }
            val response =
                client.get("/api/v1/samhandler/emottak") {
                    headers {
                        append("Accept", "application/json")
                        append("Content-Type", "application/json")
                        append("samhandlerFnr", "2313123123")
                        append("samhandlerOrgName", "Norsk Arbeidshelse – Østfold &amp; Akershus")
                        append("requestId", "1")
                        append(HttpHeaders.Authorization, "Bearer ${generateJWT("2", "clientId")}")
                    }
                }

            Assertions.assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    private fun generateJWT(
        consumerClientId: String,
        audience: String,
        customClaim: Claim? = Claim("fnr", "12313145"),
        expiry: LocalDateTime? = LocalDateTime.now().plusHours(1),
        subject: String = "subject",
        issuer: String = "https://sts.issuer.net/myid",
    ): String? {
        val now = Date()
        val key = getDefaultRSAKey()
        val alg = Algorithm.RSA256(key.toRSAPublicKey(), key.toRSAPrivateKey())

        return JWT.create()
            .withKeyId(keyId)
            .withSubject(subject)
            .withIssuer(issuer)
            .withAudience(audience)
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("ver", "1.0")
            .withClaim("nonce", "myNonce")
            .withClaim("auth_time", now)
            .withClaim(customClaim!!.name, customClaim.value)
            .withClaim("nbf", now)
            .withClaim("azp", consumerClientId)
            .withClaim("iat", now)
            .withClaim("exp", Date.from(expiry?.atZone(ZoneId.systemDefault())?.toInstant()))
            .sign(alg)
    }

    private fun getDefaultRSAKey(): RSAKey {
        return getJWKSet().getKeyByKeyId(keyId) as RSAKey
    }

    data class Claim(
        val name: String,
        val value: String,
    )

    private fun getJWKSet(): JWKSet {
        try {
            return JWKSet.parse(getFileAsString("src/test/resources/jwkset.json"))
        } catch (io: IOException) {
            throw RuntimeException(io)
        } catch (io: ParseException) {
            throw RuntimeException(io)
        }
    }

    private fun getFileAsString(filePath: String) =
        String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
}
