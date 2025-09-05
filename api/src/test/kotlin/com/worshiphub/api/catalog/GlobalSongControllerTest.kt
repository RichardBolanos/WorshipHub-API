package com.worshiphub.api.catalog

import com.worshiphub.application.catalog.CatalogApplicationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.*

@WebMvcTest(GlobalSongController::class)
class GlobalSongControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @MockBean
    private lateinit var catalogApplicationService: CatalogApplicationService
    
    @Test
    fun `should search global songs`() {
        val globalSongs = listOf(
            mapOf("id" to UUID.randomUUID().toString(), "title" to "Amazing Grace", "isVerified" to true)
        )
        
        whenever(catalogApplicationService.searchGlobalSongs(any())).thenReturn(globalSongs)
        
        mockMvc.perform(get("/api/v1/global-songs/search").param("query", "grace"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value("Amazing Grace"))
    }
    
    @Test
    @WithMockUser(roles = ["CHURCH_ADMIN"])
    fun `should import song from global catalog`() {
        val globalSongId = UUID.randomUUID()
        val churchId = UUID.randomUUID()
        val importedSongId = UUID.randomUUID()
        
        whenever(catalogApplicationService.importFromGlobal(globalSongId, churchId)).thenReturn(importedSongId)
        
        mockMvc.perform(
            post("/api/v1/global-songs/{globalSongId}/import", globalSongId)
                .header("Church-Id", churchId.toString())
        )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.songId").value(importedSongId.toString()))
    }
}