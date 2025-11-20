package com.worshiphub.api.health

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.management.ManagementFactory
import java.time.LocalDateTime
import javax.sql.DataSource

@Tag(name = "System Health", description = "Advanced system health monitoring")
@RestController
@RequestMapping("/api/v1/system")
class SystemHealthController(
    private val dataSource: DataSource
) {

    @Operation(
        summary = "Complete system health check",
        description = "Returns detailed system health including database, memory, and performance metrics"
    )
    @ApiResponse(responseCode = "200", description = "System health information")
    @GetMapping("/health")
    fun systemHealth(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        val memoryBean = ManagementFactory.getMemoryMXBean()
        val heapMemory = memoryBean.heapMemoryUsage
        val nonHeapMemory = memoryBean.nonHeapMemoryUsage
        
        return mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "service" to "WorshipHub API",
            "version" to "1.0.0",
            "environment" to System.getProperty("spring.profiles.active", "default"),
            "system" to mapOf(
                "processors" to runtime.availableProcessors(),
                "uptime" to ManagementFactory.getRuntimeMXBean().uptime,
                "jvm" to mapOf(
                    "version" to System.getProperty("java.version"),
                    "vendor" to System.getProperty("java.vendor"),
                    "name" to System.getProperty("java.vm.name")
                )
            ),
            "memory" to mapOf(
                "heap" to mapOf(
                    "used" to "${heapMemory.used / 1024 / 1024} MB",
                    "committed" to "${heapMemory.committed / 1024 / 1024} MB",
                    "max" to "${heapMemory.max / 1024 / 1024} MB",
                    "usage" to "${(heapMemory.used.toDouble() / heapMemory.max * 100).toInt()}%"
                ),
                "nonHeap" to mapOf(
                    "used" to "${nonHeapMemory.used / 1024 / 1024} MB",
                    "committed" to "${nonHeapMemory.committed / 1024 / 1024} MB"
                ),
                "total" to mapOf(
                    "free" to "${runtime.freeMemory() / 1024 / 1024} MB",
                    "total" to "${runtime.totalMemory() / 1024 / 1024} MB",
                    "max" to "${runtime.maxMemory() / 1024 / 1024} MB"
                )
            ),
            "database" to getDatabaseHealth(),
            "performance" to mapOf(
                "gcCount" to ManagementFactory.getGarbageCollectorMXBeans().sumOf { it.collectionCount },
                "gcTime" to "${ManagementFactory.getGarbageCollectorMXBeans().sumOf { it.collectionTime }} ms",
                "threadCount" to ManagementFactory.getThreadMXBean().threadCount,
                "peakThreadCount" to ManagementFactory.getThreadMXBean().peakThreadCount
            )
        )
    }

    @GetMapping("/metrics")
    fun metrics(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        return mapOf(
            "memory" to mapOf(
                "used" to runtime.totalMemory() - runtime.freeMemory(),
                "free" to runtime.freeMemory(),
                "total" to runtime.totalMemory(),
                "max" to runtime.maxMemory()
            ),
            "threads" to ManagementFactory.getThreadMXBean().threadCount,
            "uptime" to ManagementFactory.getRuntimeMXBean().uptime
        )
    }



    private fun getDatabaseHealth(): Map<String, Any?> {
        return try {
            dataSource.connection.use { connection ->
                val isValid = connection.isValid(5)
                val metadata = connection.metaData
                
                mapOf(
                    "status" to if (isValid) "UP" else "DOWN",
                    "database" to mapOf(
                        "name" to metadata.databaseProductName,
                        "version" to metadata.databaseProductVersion,
                        "url" to metadata.url.substringBefore("?"),
                        "driver" to metadata.driverName,
                        "driverVersion" to metadata.driverVersion
                    ),
                    "connection" to mapOf(
                        "valid" to isValid,
                        "autoCommit" to connection.autoCommit,
                        "readOnly" to connection.isReadOnly,
                        "catalog" to (connection.catalog ?: "default")
                    ),
                    "lastChecked" to LocalDateTime.now()
                )
            }
        } catch (e: Exception) {
            mapOf(
                "status" to "DOWN",
                "error" to e.message,
                "lastChecked" to LocalDateTime.now()
            )
        }
    }
}