package com.worshiphub.api.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Abstract base class for all E2E integration tests.
 *
 * Provides:
 * - Spring Boot test context with H2 in-memory database
 * - MockMvc for HTTP request simulation
 * - ObjectMapper for JSON serialization
 * - [TestDataHelper] for creating prerequisite data through the API
 * - Extension functions for common assertions and value extraction
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Import(TestEmailConfig::class)
@Transactional
abstract class BaseE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    lateinit var testData: TestDataHelper

    @BeforeEach
    fun baseSetUp() {
        testData = TestDataHelper(mockMvc, objectMapper)
    }

    // ── Assertion extension functions ──

    fun ResultActions.expectCreated(): ResultActions = andExpect(status().isCreated)

    fun ResultActions.expectOk(): ResultActions = andExpect(status().isOk)

    fun ResultActions.expectNoContent(): ResultActions = andExpect(status().isNoContent)

    fun ResultActions.expectBadRequest(): ResultActions = andExpect(status().isBadRequest)

    fun ResultActions.expectForbidden(): ResultActions = andExpect(status().isForbidden)

    fun ResultActions.expectConflict(): ResultActions = andExpect(status().isConflict)

    fun ResultActions.expectNotFound(): ResultActions = andExpect(status().isNotFound)

    // ── Value extraction helpers ──

    /**
     * Extracts a UUID value from the JSON response at the given JsonPath expression.
     */
    fun ResultActions.extractUUID(jsonPath: String): UUID {
        val result = this.andReturn()
        val value: String = JsonPath.read(result.response.contentAsString, jsonPath)
        return UUID.fromString(value)
    }

    /**
     * Extracts a String value from the JSON response at the given JsonPath expression.
     */
    fun ResultActions.extractString(jsonPath: String): String {
        val result = this.andReturn()
        return JsonPath.read(result.response.contentAsString, jsonPath)
    }
}
