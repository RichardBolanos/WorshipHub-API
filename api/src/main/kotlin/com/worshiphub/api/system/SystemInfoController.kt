package com.worshiphub.api.system

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Tag(name = "System", description = "System information and health monitoring endpoints")
@RestController
@RequestMapping("/api/v1/system")
class SystemInfoController {

    @Operation(
        summary = "Get system information",
        description = "Returns application metadata, version, and current server status for monitoring"
    )
    @ApiResponse(responseCode = "200", description = "System information retrieved successfully")
    @GetMapping("/info")
    fun getSystemInfo(): SystemInfoResponse {
        return SystemInfoResponse(
            applicationName = "WorshipHub API",
            version = "1.0.0",
            serverTime = LocalDateTime.now(),
            status = "operational"
        )
    }

    @Operation(
        summary = "Health check endpoint",
        description = "Lightweight health check endpoint for load balancers and monitoring systems"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy and responding")
    @GetMapping("/ping")
    fun ping(): Map<String, String> {
        return mapOf(
            "status" to "ok",
            "timestamp" to LocalDateTime.now().toString()
        )
    }
}

@Schema(description = "System information and status response")
data class SystemInfoResponse(
    @Schema(description = "Application name", example = "WorshipHub API")
    val applicationName: String,
    
    @Schema(description = "Current application version", example = "1.0.0")
    val version: String,
    
    @Schema(description = "Current server timestamp", example = "2024-01-07T15:30:00")
    val serverTime: LocalDateTime,
    
    @Schema(description = "System operational status", example = "operational", allowableValues = ["operational", "maintenance", "degraded"])
    val status: String
)