package com.worshiphub.api.config

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component
import java.util.*

/**
 * WebSocket authentication interceptor.
 */
@Component
class WebSocketAuthInterceptor : ChannelInterceptor {
    
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        return try {
            val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
            
            if (accessor != null && StompCommand.CONNECT == accessor.command) {
                // For now, allow connections and set a test user ID
                // In production, validate JWT token here
                val testUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
                accessor.sessionAttributes = mutableMapOf("userId" to testUserId)
            }
            
            message
        } catch (e: Exception) {
            // Log error and reject connection
            null
        }
    }
}