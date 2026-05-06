package com.worshiphub.infrastructure.email

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.Properties

/**
 * Explicit JavaMailSender bean.
 *
 * Why a dedicated `@Configuration` instead of relying on Spring Boot's
 * MailSenderAutoConfiguration:
 *
 * 1. **GraalVM native images sometimes drop nested property paths** during
 *    AOT processing. By configuring the sender programmatically we guarantee
 *    that timeouts (mail.smtp.connectiontimeout, etc.) are actually applied
 *    on the JavaMail Session, regardless of whether running on JVM or native.
 *
 * 2. **Diagnostic logging on startup**: we log the resolved SMTP host:port,
 *    so if the deployment can't reach Brevo we see it immediately in Render
 *    logs instead of inferring it from a generic timeout.
 *
 * 3. **Sane defaults if env vars are missing**: connection/read/write timeouts
 *    fall back to 3-5 seconds even if the corresponding properties are unset.
 *
 * The `@Profile("!h2")` guard makes sure tests using the in-memory H2 profile
 * do not need a real mail server.
 */
@Configuration
@Profile("!h2")
class MailConfig(
    @Value("\${spring.mail.host:}") private val host: String,
    @Value("\${spring.mail.port:587}") private val port: Int,
    @Value("\${spring.mail.username:}") private val username: String,
    @Value("\${spring.mail.password:}") private val password: String,
    @Value("\${spring.mail.properties.mail.smtp.auth:true}") private val smtpAuth: Boolean,
    @Value("\${spring.mail.properties.mail.smtp.starttls.enable:true}") private val starttlsEnable: Boolean,
    @Value("\${spring.mail.properties.mail.smtp.starttls.required:true}") private val starttlsRequired: Boolean,
    @Value("\${spring.mail.properties.mail.smtp.connectiontimeout:3000}") private val connectionTimeoutMs: Int,
    @Value("\${spring.mail.properties.mail.smtp.timeout:5000}") private val readTimeoutMs: Int,
    @Value("\${spring.mail.properties.mail.smtp.writetimeout:5000}") private val writeTimeoutMs: Int
) {
    private val logger = LoggerFactory.getLogger(MailConfig::class.java)

    @Bean
    fun javaMailSender(): JavaMailSender {
        if (host.isBlank()) {
            logger.warn(
                "spring.mail.host is empty — emails will fail to send. " +
                    "Set SPRING_MAIL_HOST + SPRING_MAIL_PORT + SPRING_MAIL_USERNAME + SPRING_MAIL_PASSWORD."
            )
        } else {
            logger.info(
                "Configuring JavaMailSender — host={}, port={}, username={}, " +
                    "auth={}, starttls={}, connectTimeout={}ms, readTimeout={}ms",
                host, port, mask(username), smtpAuth, starttlsEnable,
                connectionTimeoutMs, readTimeoutMs
            )
        }

        val sender = JavaMailSenderImpl()
        sender.host = host
        sender.port = port
        sender.username = username
        sender.password = password
        sender.defaultEncoding = "UTF-8"

        val props: Properties = sender.javaMailProperties
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = smtpAuth.toString()
        props["mail.smtp.starttls.enable"] = starttlsEnable.toString()
        props["mail.smtp.starttls.required"] = starttlsRequired.toString()
        // Aggressive timeouts so a misconfigured/blocked SMTP fails fast and
        // the @Async email thread becomes available again for the next send.
        props["mail.smtp.connectiontimeout"] = connectionTimeoutMs.toString()
        props["mail.smtp.timeout"] = readTimeoutMs.toString()
        props["mail.smtp.writetimeout"] = writeTimeoutMs.toString()

        return sender
    }

    private fun mask(value: String): String =
        if (value.length <= 4) "***" else "${value.take(2)}***${value.takeLast(2)}"
}
