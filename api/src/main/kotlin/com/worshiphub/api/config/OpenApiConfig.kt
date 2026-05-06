package com.worshiphub.api.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.Components
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("WorshipHub API")
                    .version("1.0.5")
                    .description(
                        "API for WorshipHub — Worship Team Management Platform.\n\n" +
                        "**Authentication:** Most endpoints require a JWT bearer token obtained from " +
                        "`POST /api/v1/auth/login`. Some endpoints (notifications) also require a " +
                        "`User-Id` HTTP header that the Flutter `AuthInterceptor` adds automatically.\n\n" +
                        "**Push notifications:** Many domain actions (creating songs, scheduling services, " +
                        "cancelling, sending chat messages, etc.) trigger push events that produce " +
                        "in-app notifications retrievable via `GET /api/v1/notifications`. The actor " +
                        "of each action is excluded from the recipients list — they don't get notified " +
                        "of their own actions."
                    )
                    .contact(
                        Contact()
                            .name("WorshipHub Team")
                            .email("richarandres1998@gmail.com")
                    )
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT token obtained from /api/v1/auth/login")
                    )
            )
    }
}