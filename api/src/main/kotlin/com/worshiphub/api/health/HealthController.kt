package com.worshiphub.api.health

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Tag(name = "Health", description = "Application health check operations")
@RestController
@RequestMapping("/api/v1/health")
class HealthController {

    @Operation(
        summary = "Health check",
        description = "Returns the current health status of the application"
    )
    @ApiResponse(responseCode = "200", description = "Application is healthy")
    @GetMapping
    fun health(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "service" to "WorshipHub API"
        )
    }
}