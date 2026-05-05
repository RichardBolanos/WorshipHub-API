package com.worshiphub.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openApiCustomizer(): OpenApiCustomizer {
        return OpenApiCustomizer { openApi ->
            // Ensure components is initialized
            if (openApi.components == null) {
                openApi.components = Components()
            }

            // Add common error responses
            val errorContent = Content().addMediaType("application/json",
                MediaType().schema(Schema<Any>().type("object")
                    .addProperty("message", Schema<Any>().type("string"))
                    .addProperty("timestamp", Schema<Any>().type("string"))
                    .addProperty("path", Schema<Any>().type("string"))
                )
            )

            val responses = linkedMapOf(
                "BadRequest" to ApiResponse().description("Bad Request").content(errorContent),
                "Unauthorized" to ApiResponse().description("Unauthorized").content(errorContent),
                "Forbidden" to ApiResponse().description("Forbidden").content(errorContent),
                "NotFound" to ApiResponse().description("Not Found").content(errorContent),
                "InternalServerError" to ApiResponse().description("Internal Server Error").content(errorContent)
            )

            openApi.components.responses = responses
        }
    }
}
