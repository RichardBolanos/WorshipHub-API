package com.worshiphub.security.oauth2

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.InetSocketAddress
import java.util.Date
import java.util.UUID

/**
 * Unit tests for [GoogleIdTokenVerifier].
 *
 * Strategy: spin up a local HTTP server that serves a JWKS document containing
 * a public key we generate in-test. Tokens are signed with the matching private
 * key. This lets us exercise the full signature-verification path without
 * touching real Google infrastructure or stubbing nimbus internals.
 *
 * Each `@Nested` group covers a distinct security property of the verifier:
 *   - happy path with valid token
 *   - rejection on invalid signature (different key)
 *   - rejection on issuer mismatch
 *   - rejection on audience mismatch
 *   - rejection on expired token
 *   - rejection on missing required claims
 *   - rejection on malformed JWT
 *   - rejection when configuration is empty
 *   - support for multiple allowed client IDs
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("GoogleIdTokenVerifier — security validation")
class GoogleIdTokenVerifierTest {

    private lateinit var jwksServer: HttpServer
    private lateinit var jwksUrl: String

    private lateinit var validRsaKey: RSAKey
    private lateinit var anotherRsaKey: RSAKey

    private val androidClientId = "111111111111-aaa.apps.googleusercontent.com"
    private val iosClientId = "111111111111-bbb.apps.googleusercontent.com"
    private val webClientId = "111111111111-ccc.apps.googleusercontent.com"

    private val allowedClientIdsCsv = "$androidClientId,$iosClientId,$webClientId"

    @BeforeAll
    fun startJwksServer() {
        // Generate two RSA keys: one published in JWKS (valid signer),
        // one NOT published (used to forge invalid-signature tokens).
        validRsaKey = RSAKeyGenerator(2048)
            .keyID("test-key-1")
            .generate()
        anotherRsaKey = RSAKeyGenerator(2048)
            .keyID("rogue-key")
            .generate()

        val jwks = JWKSet(validRsaKey.toPublicJWK()).toString()

        jwksServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/certs") { exchange ->
                val bytes = jwks.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }
        jwksUrl = "http://127.0.0.1:${jwksServer.address.port}/certs"
    }

    @AfterAll
    fun stopJwksServer() {
        jwksServer.stop(0)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun newVerifier(
        allowedClientIds: String = allowedClientIdsCsv,
        jwksUriOverride: String = jwksUrl
    ): GoogleIdTokenVerifier =
        GoogleIdTokenVerifier(allowedClientIds, jwksUriOverride)

    private data class TokenSpec(
        val keyId: String = "test-key-1",
        val issuer: String = "https://accounts.google.com",
        val audience: String? = null, // null => caller fills in default
        val subject: String = "google-user-fixed",
        val email: String = "user@example.com",
        val emailVerified: Boolean? = true,
        val name: String? = "Jane Doe",
        val givenName: String? = "Jane",
        val familyName: String? = "Doe",
        val expiresInSeconds: Long = 3600,
        val notBeforeSeconds: Long = 0,
        val omitClaims: Set<String> = emptySet(),
        val signWithRogueKey: Boolean = false
    )

    private fun signToken(spec: TokenSpec = TokenSpec()): String {
        val now = System.currentTimeMillis()
        val aud = spec.audience ?: androidClientId
        val signingKey = if (spec.signWithRogueKey) anotherRsaKey else validRsaKey

        val builder = JWTClaimsSet.Builder()
            .subject("${spec.subject}-${UUID.randomUUID()}")
            .issuer(spec.issuer)
            .audience(aud)
            .expirationTime(Date(now + spec.expiresInSeconds * 1000))
            .issueTime(Date(now - spec.notBeforeSeconds * 1000))

        if ("email" !in spec.omitClaims) builder.claim("email", spec.email)
        if (spec.emailVerified != null && "email_verified" !in spec.omitClaims) {
            builder.claim("email_verified", spec.emailVerified)
        }
        spec.name?.let { builder.claim("name", it) }
        spec.givenName?.let { builder.claim("given_name", it) }
        spec.familyName?.let { builder.claim("family_name", it) }

        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(spec.keyId)
            .build()

        val signedJwt = SignedJWT(header, builder.build())
        signedJwt.sign(RSASSASigner(signingKey.toPrivateKey()))
        return signedJwt.serialize()
    }

    // ── Happy path ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Valid tokens")
    inner class ValidTokens {

        @Test
        fun `accepts a properly signed token with the Android client ID as audience`() {
            val token = signToken(TokenSpec(audience = androidClientId))

            val claims = newVerifier().verify(token)

            assertNotNull(claims)
            assertEquals("user@example.com", claims!!.getStringClaim("email"))
            assertEquals(true, claims.getBooleanClaim("email_verified"))
            assertEquals("https://accounts.google.com", claims.issuer)
            assertEquals(listOf(androidClientId), claims.audience)
        }

        @Test
        fun `accepts a token with the iOS client ID as audience`() {
            val token = signToken(TokenSpec(audience = iosClientId))

            val claims = newVerifier().verify(token)

            assertNotNull(claims)
            assertEquals(listOf(iosClientId), claims!!.audience)
        }

        @Test
        fun `accepts a token with the Web client ID as audience`() {
            val token = signToken(TokenSpec(audience = webClientId))

            val claims = newVerifier().verify(token)

            assertNotNull(claims)
        }

        @Test
        fun `accepts the legacy issuer 'accounts dot google dot com' (without scheme)`() {
            val token = signToken(TokenSpec(issuer = "accounts.google.com"))

            val claims = newVerifier().verify(token)

            assertNotNull(claims)
            assertEquals("accounts.google.com", claims!!.issuer)
        }
    }

    // ── Signature ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Signature verification")
    inner class SignatureVerification {

        @Test
        fun `rejects a token signed with a key not present in JWKS (forged)`() {
            // Token is signed with a key the JWKS endpoint does not publish.
            // The kid points to a published key ID, but the actual signing key is different.
            val token = signToken(
                TokenSpec(signWithRogueKey = true, keyId = "test-key-1")
            )

            val claims = newVerifier().verify(token)

            assertNull(claims, "Forged token must be rejected")
        }

        @Test
        fun `rejects a token whose kid points to an unknown key`() {
            val token = signToken(
                TokenSpec(signWithRogueKey = true, keyId = "unknown-kid")
            )

            val claims = newVerifier().verify(token)

            assertNull(claims)
        }

        @Test
        fun `rejects a tampered token (claims modified after signing)`() {
            val original = signToken(TokenSpec(email = "victim@example.com"))
            // Replace the payload section with a tampered one. The signature won't match.
            val parts = original.split(".")
            val tamperedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                """{"iss":"https://accounts.google.com","aud":"$androidClientId","email":"attacker@example.com","exp":${(System.currentTimeMillis() / 1000) + 3600},"sub":"x","email_verified":true}"""
                    .toByteArray()
            )
            val tampered = "${parts[0]}.$tamperedPayload.${parts[2]}"

            val claims = newVerifier().verify(tampered)

            assertNull(claims, "Token with mismatched signature must be rejected")
        }
    }

    // ── Issuer ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Issuer validation")
    inner class IssuerValidation {

        @Test
        fun `rejects a token with a non-Google issuer`() {
            val token = signToken(TokenSpec(issuer = "https://evil.example.com"))

            val claims = newVerifier().verify(token)

            assertNull(claims)
        }

        @Test
        fun `rejects a token with empty issuer`() {
            // We can't easily create a JWT with no iss using our builder (it would still set one),
            // so instead use an explicit empty string.
            val token = signToken(TokenSpec(issuer = ""))

            val claims = newVerifier().verify(token)

            assertNull(claims)
        }
    }

    // ── Audience ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Audience validation")
    inner class AudienceValidation {

        @Test
        fun `rejects a token whose audience is not in the allowed list`() {
            val unknownClientId = "999999999999-zzz.apps.googleusercontent.com"
            val token = signToken(TokenSpec(audience = unknownClientId))

            val claims = newVerifier().verify(token)

            assertNull(claims, "Token from a different OAuth client must be rejected")
        }

        @Test
        fun `accepts a token when at least one of multiple audiences is allowed`() {
            // Some Google ID tokens carry multiple audiences. Build one manually.
            val now = System.currentTimeMillis()
            val claims = JWTClaimsSet.Builder()
                .subject("user-123")
                .issuer("https://accounts.google.com")
                .audience(listOf("https://other-aud.example.com", androidClientId))
                .claim("email", "user@example.com")
                .claim("email_verified", true)
                .expirationTime(Date(now + 3_600_000))
                .build()

            val header = JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key-1").build()
            val jwt = SignedJWT(header, claims)
            jwt.sign(RSASSASigner(validRsaKey.toPrivateKey()))

            val verified = newVerifier().verify(jwt.serialize())

            assertNotNull(verified)
        }
    }

    // ── Expiration ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Expiration validation")
    inner class ExpirationValidation {

        @Test
        fun `rejects a token whose exp is in the past`() {
            val token = signToken(TokenSpec(expiresInSeconds = -60))

            val claims = newVerifier().verify(token)

            assertNull(claims, "Expired token must be rejected")
        }
    }

    // ── Required claims ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Required claims")
    inner class RequiredClaims {

        @Test
        fun `rejects a token missing the email claim`() {
            val token = signToken(TokenSpec(omitClaims = setOf("email")))

            val claims = newVerifier().verify(token)

            assertNull(claims)
        }
    }

    // ── Malformed input ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Malformed input")
    inner class MalformedInput {

        @Test
        fun `rejects an obviously malformed JWT string`() {
            val claims = newVerifier().verify("not.a.valid.jwt")
            assertNull(claims)
        }

        @Test
        fun `rejects an empty string`() {
            val claims = newVerifier().verify("")
            assertNull(claims)
        }

        @Test
        fun `rejects a string with the wrong number of segments`() {
            val claims = newVerifier().verify("one.two")
            assertNull(claims)
        }
    }

    // ── Configuration ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Configuration safety")
    inner class ConfigurationSafety {

        @Test
        fun `rejects ALL tokens when no client IDs are configured (fail-closed)`() {
            val token = signToken(TokenSpec())

            val verifier = newVerifier(allowedClientIds = "")
            val claims = verifier.verify(token)

            assertNull(claims, "An unconfigured verifier must NEVER accept tokens")
        }

        @Test
        fun `rejects tokens when configuration is only whitespace and commas`() {
            val token = signToken(TokenSpec())

            val verifier = newVerifier(allowedClientIds = " , , , ")
            val claims = verifier.verify(token)

            assertNull(claims)
        }
    }
}
