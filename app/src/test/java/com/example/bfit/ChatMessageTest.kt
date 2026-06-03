package com.example.bfit

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for ChatMessage model and chat utility logic.
 */
class ChatMessageTest {

    @Test
    fun chatMessage_userMessage_isUser() {
        val msg = ChatMessage("Hello coach!", isUser = true)
        assertEquals("Hello coach!", msg.text)
        assertTrue(msg.isUser)
    }

    @Test
    fun chatMessage_botMessage_isNotUser() {
        val msg = ChatMessage("Here's your workout plan...", isUser = false)
        assertFalse(msg.isUser)
    }

    @Test
    fun chatMessage_emptyText() {
        val msg = ChatMessage("", isUser = true)
        assertTrue(msg.text.isEmpty())
    }

    @Test
    fun chatMessage_longText_preserved() {
        val longText = "A".repeat(5000)
        val msg = ChatMessage(longText, isUser = false)
        assertEquals(5000, msg.text.length)
    }
}
