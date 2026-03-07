package com.worshiphub.api.catalog

import com.fasterxml.jackson.databind.ObjectMapper
import com.worshiphub.application.catalog.CatalogApplicationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.*

@WebMvcTest(CategoryController::class)
class CategoryControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @MockBean
    private lateinit var catalogApplicationService: CatalogApplicationService
    
    @Test
    @WithMockUser(roles = ["WORSHIP_LEADER"])
    fun `should create category`() {
        val categoryId = UUID.randomUUID()
        val request = mapOf("name" to "Worship")
        
        whenever(catalogApplicationService.createCategory(any())).thenReturn(categoryId)
        
        mockMvc.perform(
            post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Church-Id", UUID.randomUUID().toString())
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
    }
    
    @Test
    @WithMockUser(roles = ["WORSHIP_LEADER"])
    fun `should create tag`() {
        val tagId = UUID.randomUUID()
        val request = mapOf("name" to "Christmas")
        
        whenever(catalogApplicationService.createTag(any(), any())).thenReturn(tagId)
        
        mockMvc.perform(
            post("/api/v1/categories/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Church-Id", UUID.randomUUID().toString())
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.tagId").value(tagId.toString()))
    }
}