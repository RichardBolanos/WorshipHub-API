package com.worshiphub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Main Spring Boot application class for WorshipHub API.
 */
@SpringBootApplication(scanBasePackages = ["com.worshiphub"])
@EnableScheduling
class WorshipHubApplication

fun main(args: Array<String>) {
    runApplication<WorshipHubApplication>(*args)
}