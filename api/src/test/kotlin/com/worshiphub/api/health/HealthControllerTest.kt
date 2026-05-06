package com.worshiphub.api.health

import com.worshiphub.api.support.WebMvcSecurityTestConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(HealthController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSecurityTestConfig::class)
class HealthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `debe retornar 200 cuando solicitud es valida`() {
        mockMvc.perform(get("/api/v1/health")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("WorshipHub API"))
            .andExpect(jsonPath("$.version").exists())
            .andExpect(jsonPath("$.environment").value("production"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.uptime").value("running"))
    }
}