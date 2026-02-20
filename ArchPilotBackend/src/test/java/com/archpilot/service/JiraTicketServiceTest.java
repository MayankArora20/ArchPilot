package com.archpilot.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.archpilot.dto.JiraTicketResponse;
import com.archpilot.model.ChatSession;
import com.archpilot.service.agent.GeminiChatAgentService;

@ExtendWith(MockitoExtension.class)
class JiraTicketServiceTest {

    @Mock
    private GeminiChatAgentService geminiChatAgentService;

    @InjectMocks
    private JiraTicketService jiraTicketService;

    private ChatSession testSession;

    @BeforeEach
    void setUp() {
        testSession = new ChatSession();
        testSession.setProjectName("test-project");
        testSession.setUmlContent("@startuml\nclass TestClass\n@enduml");
        
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("totalClasses", 5);
        jsonData.put("analyzedClasses", 3);
        jsonData.put("repositoryUrl", "https://github.com/test/repo");
        jsonData.put("branch", "main");
        testSession.setJsonData(jsonData);
    }

    @Test
    void testCreateJiraTicket_WithValidResponse() {
        // Given
        String userMessage = "Create a new user authentication feature";
        String mockGeminiResponse = """
            {
                "ticketId": "ARCH-1234",
                "heading": "Implement User Authentication",
                "description": "Create a comprehensive user authentication system",
                "storyPoints": 8,
                "classesToConsider": ["UserService", "AuthController"],
                "methodsToConsider": ["authenticate", "validateToken"],
                "designPatterns": ["Strategy Pattern", "Factory Pattern"],
                "unitTestCases": ["Test valid login", "Test invalid credentials"],
                "successCriteria": "Users can login and logout successfully",
                "priority": "High",
                "ticketType": "Story"
            }
            """;

        when(geminiChatAgentService.askQuestion(anyString())).thenReturn(mockGeminiResponse);

        // When
        JiraTicketResponse result = jiraTicketService.createJiraTicket(testSession, userMessage);

        // Then
        assertNotNull(result);
        assertEquals("ARCH-1234", result.getTicketId());
        assertEquals("Implement User Authentication", result.getHeading());
        assertEquals("Create a comprehensive user authentication system", result.getDescription());
        assertEquals(8, result.getStoryPoints());
        assertEquals("High", result.getPriority());
        assertEquals("Story", result.getTicketType());
        
        verify(geminiChatAgentService, times(1)).askQuestion(anyString());
    }

    @Test
    void testCreateJiraTicket_WithInvalidResponse() {
        // Given
        String userMessage = "Create a new feature";
        String mockGeminiResponse = "This is not a valid JSON response";

        when(geminiChatAgentService.askQuestion(anyString())).thenReturn(mockGeminiResponse);

        // When
        JiraTicketResponse result = jiraTicketService.createJiraTicket(testSession, userMessage);

        // Then
        assertNotNull(result);
        assertNotNull(result.getTicketId());
        assertTrue(result.getTicketId().startsWith("ARCH-"));
        assertNotNull(result.getHeading());
        assertEquals(5, result.getStoryPoints()); // Default value
        
        verify(geminiChatAgentService, times(1)).askQuestion(anyString());
    }

    @Test
    void testFormatJiraTicketResponse() {
        // Given
        JiraTicketResponse ticket = new JiraTicketResponse();
        ticket.setTicketId("ARCH-5678");
        ticket.setHeading("Test Ticket");
        ticket.setDescription("Test Description");
        ticket.setStoryPoints(3);
        ticket.setPriority("Medium");
        ticket.setTicketType("Task");

        // When
        String result = jiraTicketService.formatJiraTicketResponse(ticket, "test-project");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("ARCH-5678"));
        assertTrue(result.contains("Test Ticket"));
        assertTrue(result.contains("Test Description"));
    }
}