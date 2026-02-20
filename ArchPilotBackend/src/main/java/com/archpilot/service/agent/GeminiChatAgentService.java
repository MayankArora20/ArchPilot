package com.archpilot.service.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Gemini Chat Agent Service
 * 
 * Main Context:
 * - Provides basic chat functionality with Google's Gemini AI model
 * - Acts as a foundational service for other specialized agents
 * - Handles direct question-answer interactions with Gemini
 * - Manages the ChatClient configuration and communication
 * - Serves as the core AI communication layer for the application
 * 
 * Token Guardrail Protection:
 * - This agent is automatically protected when used through other services
 * - Token consumption is managed by the calling services (JavaArchitectAgentService, etc.)
 * - Rate limiting is handled at the endpoint level, not directly in this service
 * 
 * Usage:
 * - Used by JavaArchitectAgentService for architectural analysis
 * - Used by GeminiClassAnalyzerAgentService for code analysis
 * - Can be extended for other AI-powered features
 * - Provides a consistent interface to Gemini AI across the application
 */
@Service
public class GeminiChatAgentService {

    private final ChatClient chatClient;

    public GeminiChatAgentService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Send a question to Gemini AI and get a response
     * 
     * @param question The question or prompt to send to Gemini
     * @return The AI's response as a string
     */
    public String askQuestion(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}