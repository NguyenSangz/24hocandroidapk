package com.example.domain.repository

import com.example.domain.model.AiChatMessage
import kotlinx.coroutines.flow.Flow

interface AiTutorRepository {
    fun getChatMessages(): Flow<List<AiChatMessage>>
    suspend fun sendMessage(userText: String, subjectId: String, selectedGrade: Int): AiChatMessage
    suspend fun clearHistory()
}
