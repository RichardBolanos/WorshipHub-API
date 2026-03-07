package com.worshiphub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main Spring Boot application class for WorshipHub API.
 */
@SpringBootApplication(scanBasePackages = ["com.worshiphub"])
class WorshipHubApplication

fun main(args: Array<String>) {
    runApplication<WorshipHubApplication>(*args)
}