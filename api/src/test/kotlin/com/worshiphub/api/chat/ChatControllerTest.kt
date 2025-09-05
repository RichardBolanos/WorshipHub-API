package com.worshiphub.api.chat

import com.worshiphub.application.chat.ChatApplicationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(ChatController::class)
class ChatControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @MockBean
    private lateinit var chatApplicationService: ChatApplicationService
    
    @MockBean
    private lateinit var messagingTemplate: SimpMessagingTemplate
    
    @Test
    @WithMockUser(roles = ["TEAM_MEMBER"])
    fun `should get chat history`() {
        val teamId = UUID.randomUUID()
        val messages = listOf(
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "teamId" to teamId.toString(),
                "userId" to UUID.randomUUID().toString(),
                "content" to "Hello team!",
                "createdAt" to LocalDateTime.now().toString()
            )
        )
        
        whenever(chatApplicationService.getTeamChatHistory(teamId, 50)).thenReturn(messages)
        
        mockMvc.perform(get("/api/v1/teams/{teamId}/chat/history", teamId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].content").value("Hello team!"))
    }
}