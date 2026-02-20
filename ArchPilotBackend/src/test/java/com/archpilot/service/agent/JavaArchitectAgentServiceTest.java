package com.archpilot.service.agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
import com.archpilot.service.IntentAnalyzerService;
import com.archpilot.service.JiraTicketService;
import com.archpilot.service.diagram.DiagramFileManager;

@ExtendWith(MockitoExtension.class)
class JavaArchitectAgentServiceTest {

    @Mock
    private GeminiChatAgentService geminiChatAgentService;

    @Mock
    private DiagramFileManager diagramFileManager;

    @Mock
    private IntentAnalyzerService intentAnalyzerService;

    @Mock
    private JiraTicketService jiraTicketService;

    @InjectMocks
    private JavaArchitectAgentService javaArchitectAgentService;

    private ChatSession testSession;

    @BeforeEach
    void setUp() {
        reset(geminiChatAgentService, diagramFileManager, intentAnalyzerService, jiraTicketService);
        
        testSession = new ChatSession();
        testSession.setProjectName("test-project");
        testSession.setUmlContent("@startuml\nclass TestClass\n@enduml");
        
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("totalClasses", 10);
        jsonData.put("analyzedClasses", 8);
        jsonData.put("repositoryUrl", "https://github.com/test/repo");
        jsonData.put("branch", "main");
        testSession.setJsonData(jsonData);
    }

    @Test
    void testProcessArchitectRequest_CreateJiraTicket() {
        String userMessage = "Create a new user authentication feature";
        String intent = "CREATE_JIRA_TICKET";
        
        JiraTicketResponse ticketResponse = new JiraTicketResponse();
        ticketResponse.setTicketId("ARCH-123");
        ticketResponse.setHeading("User Authentication Feature");
        
        when(intentAnalyzerService.analyzeUserIntent(userMessage)).thenReturn(intent);
        when(jiraTicketService.createJiraTicket(testSession, userMessage)).thenReturn(ticketResponse);
        when(jiraTicketService.formatJiraTicketResponse(ticketResponse, testSession.getProjectName()))
            .thenReturn("JIRA ticket ARCH-123 created successfully");

        String result = javaArchitectAgentService.processArchitectRequest(testSession, userMessage);

        assertNotNull(result);
        assertEquals("JIRA ticket ARCH-123 created successfully", result);
        
        verify(intentAnalyzerService).analyzeUserIntent(userMessage);
        verify(jiraTicketService).createJiraTicket(testSession, userMessage);
        verify(jiraTicketService).formatJiraTicketResponse(ticketResponse, testSession.getProjectName());
        verifyNoInteractions(geminiChatAgentService, diagramFileManager);
    }

    @Test
    void testProcessArchitectRequest_CreateJiraTicket_Exception() {
        String userMessage = "Create a new feature";
        String intent = "CREATE_JIRA_TICKET";
        
        when(intentAnalyzerService.analyzeUserIntent(userMessage)).thenReturn(intent);
        when(jiraTicketService.createJiraTicket(testSession, userMessage))
            .thenThrow(new RuntimeException("JIRA service error"));

        String result = javaArchitectAgentService.processArchitectRequest(testSession, userMessage);

        assertNotNull(result);
        assertTrue(result.contains("I encountered an error while creating the JIRA ticket"));
        assertTrue(result.contains("However, I can still provide architectural guidance"));
        assertTrue(result.contains(userMessage));
        
        verify(intentAnalyzerService).analyzeUserIntent(userMessage);
        verify(jiraTicketService).createJiraTicket(testSession, userMessage);
        verifyNoInteractions(geminiChatAgentService, diagramFileManager);
    }

    @Test
    void testProcessArchitectRequest_ExplainCodeFlow_WithClassAndMethod() {
        String userMessage = "Explain the flow of processPayment method in PaymentService";
        String intent = "EXPLAIN_CODE_FLOW";
        String[] classAndMethod = {"PaymentService", "processPayment"};
        
        when(intentAnalyzerService.analyzeUserIntent(userMessage)).thenReturn(intent);
        when(intentAnalyzerService.extractClassAndMethod(userMessage)).thenReturn(classAndMethod);
        when(geminiChatAgentService.askQuestion(anyString())).thenReturn("Detailed flow analysis");
        when(diagramFileManager.generateFlowDiagrams(anyString(), anyString(), anyString(), anyString()))
            .thenReturn("Diagram links");

        String result = javaArchitectAgentService.processArchitectRequest(testSession, userMessage);

        assertNotNull(result);
        assertTrue(result.contains("## Code Flow Analysis: PaymentService.processPayment"));
        assertTrue(result.contains("Detailed flow analysis"));
        assertTrue(result.contains("Diagram links"));
        
        verify(intentAnalyzerService).analyzeUserIntent(userMessage);
        verify(intentAnalyzerService).extractClassAndMethod(userMessage);
        verify(geminiChatAgentService).askQuestion(anyString());
        verify(diagramFileManager).generateFlowDiagrams(
            eq(testSession.getProjectName()), eq("PaymentService"), eq("processPayment"), anyString());
    }

    @Test
    void testProcessArchitectRequest_ExplainCodeFlow_WithoutSpecificClass() {
        String userMessage = "Explain how authentication works";
        String intent = "EXPLAIN_CODE_FLOW";
        String[] classAndMethod = {null, null};
        
        when(intentAnalyzerService.analyzeUserIntent(userMessage)).thenReturn(intent);
        when(intentAnalyzerService.extractClassAndMethod(userMessage)).thenReturn(classAndMethod);
        when(intentAnalyzerService.isGeneralFlowRequest(userMessage)).thenReturn(false);
        when(geminiChatAgentService.askQuestion(anyString())).thenReturn("General flow explanation");

        String result = javaArchitectAgentService.processArchitectRequest(testSession, userMessage);

        assertNotNull(result);
        assertEquals("General flow explanation", result);
        
        verify(intentAnalyzerService).analyzeUserIntent(userMessage);
        verify(intentAnalyzerService).extractClassAndMethod(userMessage);
        verify(intentAnalyzerService).isGeneralFlowRequest(userMessage);
        verify(geminiChatAgentService).askQuestion(anyString());
        verifyNoInteractions(diagramFileManager);
    }

    @Test
    void testProcessArchitectRequest_ExplainCodeFlow_GeneralFlowRequest() {
        String userMessage = "How do I understand code flows?";
        String intent = "EXPLAIN_CODE_FLOW";
        String[] classAndMethod = {null, null};
        
        when(intentAnalyzerService.analyzeUserIntent(userMessage)).thenReturn(intent);
        when(intentAnalyzerService.extractClassAndMethod(userMessage)).thenReturn(classAndMethod);
        when(intentAnalyzerService.isGeneralFlowRequest(userMessage)).thenReturn(true);

        String result = javaArchitectAgentService.processArchitectRequest(testSession, userMessage);

        assertNotNull(result);
        assertTrue(result.contains("I can help you understand the code flow!"));
        assertTrue(result.contains("To provide a detailed analysis with visual diagrams"));
        assertTrue(result.contains("**Class name**: Which class contains the method"));
        assertTrue(result.contains("**Examples:**"));
        
        verify(intentAnalyzerService).analyzeUserIntent(userMessage);
        verify(intentAnalyzerService).extractClassAndMethod(userMessage);
        verify(intentAnalyzerService).isGeneralFlowRequest(userMessage);
        verifyNoInteractions(geminiChatAgentService, diagramFileManager);
    }

    @Test
    void testProcessArchitectRequest_ArchitecturalAdvice() {
        String userMessage = "What design patterns should I use for this microservice?";
        String intent = "ARCHITECTURAL_ADVICE";
        
        when(intentAnalyzerService.analyzeUserIntent(userMessage)).thenReturn(intent);
        when(geminiChatAgentService.askQuestion(anyString())).thenReturn("Architectural advice response");

        String result = javaArchitectAgentService.processArchitectRequest(testSession, userMessage);

        assertNotNull(result);
        assertEquals("Architectural advice response", result);
        
        verify(intentAnalyzerService).analyzeUserIntent(userMessage);
        verify(geminiChatAgentService).askQuestion(argThat(prompt -> 
            prompt.contains("Senior Java Architect providing architectural guidance") &&
            prompt.contains(testSession.getProjectName()) &&
            prompt.contains(testSession.getUmlContent()) &&
            prompt.contains(userMessage)
        ));
        verifyNoInteractions(diagramFileManager, jiraTicketService);
    }

    @Test
    void testProcessArchitectRequest_GeneralDiscussion() {
        String userMessage = "Tell me about the current architecture";
        String intent = "GENERAL_DISCUSSION";
        
        when(intentAnalyzerService.analyzeUserIntent(userMessage)).thenReturn(intent);
        when(geminiChatAgentService.askQuestion(anyString())).thenReturn("General discussion response");

        String result = javaArchitectAgentService.processArchitectRequest(testSession, userMessage);

        assertNotNull(result);
        assertEquals("General discussion response", result);
        
        verify(intentAnalyzerService).analyzeUserIntent(userMessage);
        verify(geminiChatAgentService).askQuestion(argThat(prompt -> 
            prompt.contains("Senior Java Architect having a discussion") &&
            prompt.contains(testSession.getProjectName()) &&
            prompt.contains(testSession.getUmlContent()) &&
            prompt.contains(userMessage)
        ));
        verifyNoInteractions(diagramFileManager, jiraTicketService);
    }

    @Test
    void testProcessArchitectRequest_ExceptionHandling() {
        String userMessage = "Test message";
        
        when(intentAnalyzerService.analyzeUserIntent(userMessage))
            .thenThrow(new RuntimeException("Intent analysis failed"));

        String result = javaArchitectAgentService.processArchitectRequest(testSession, userMessage);

        assertNotNull(result);
        assertTrue(result.contains("I apologize, but I encountered an error"));
        assertTrue(result.contains("Please try rephrasing your question"));
        
        verify(intentAnalyzerService).analyzeUserIntent(userMessage);
        verifyNoInteractions(geminiChatAgentService, diagramFileManager, jiraTicketService);
    }

    @Test
    void testProcessArchitectRequest_ExplainCodeFlow_DiagramGenerationException() {
        String userMessage = "Explain PaymentService.processPayment";
        String intent = "EXPLAIN_CODE_FLOW";
        String[] classAndMethod = {"PaymentService", "processPayment"};
        
        when(intentAnalyzerService.analyzeUserIntent(userMessage)).thenReturn(intent);
        when(intentAnalyzerService.extractClassAndMethod(userMessage)).thenReturn(classAndMethod);
        when(geminiChatAgentService.askQuestion(anyString())).thenReturn("Flow analysis");
        when(diagramFileManager.generateFlowDiagrams(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Diagram generation failed"));

        String result = javaArchitectAgentService.processArchitectRequest(testSession, userMessage);

        assertNotNull(result);
        assertEquals("Flow analysis", result);
        
        verify(intentAnalyzerService).analyzeUserIntent(userMessage);
        verify(intentAnalyzerService).extractClassAndMethod(userMessage);
        verify(geminiChatAgentService, times(2)).askQuestion(anyString()); // Called twice: once for detailed analysis, once for fallback
        verify(diagramFileManager).generateFlowDiagrams(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testProcessArchitectRequest_WithNullSession() {
        String userMessage = "Test message";
        
        assertThrows(NullPointerException.class, () ->
            javaArchitectAgentService.processArchitectRequest(null, userMessage)
        );
    }

    @Test
    void testProcessArchitectRequest_WithEmptyMessage() {
        String userMessage = "";
        String intent = "GENERAL_DISCUSSION";
        
        when(intentAnalyzerService.analyzeUserIntent(userMessage)).thenReturn(intent);
        when(geminiChatAgentService.askQuestion(anyString())).thenReturn("Response to empty message");

        String result = javaArchitectAgentService.processArchitectRequest(testSession, userMessage);

        assertNotNull(result);
        assertEquals("Response to empty message", result);
        
        verify(intentAnalyzerService).analyzeUserIntent(userMessage);
        verify(geminiChatAgentService).askQuestion(anyString());
    }

    @Test
    void testProcessArchitectRequest_WithNullJsonData() {
        testSession.setJsonData(null);
        String userMessage = "Test architectural advice";
        String intent = "ARCHITECTURAL_ADVICE";
        
        when(intentAnalyzerService.analyzeUserIntent(userMessage)).thenReturn(intent);
        when(geminiChatAgentService.askQuestion(anyString())).thenReturn("Advice response");

        String result = javaArchitectAgentService.processArchitectRequest(testSession, userMessage);

        assertNotNull(result);
        assertEquals("Advice response", result);
        
        verify(geminiChatAgentService).askQuestion(argThat(prompt -> 
            prompt.contains("No metadata available")
        ));
    }
}