package com.archpilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for IntentAnalyzerService
 */
class IntentAnalyzerServiceTest {

    private IntentAnalyzerService intentAnalyzerService;

    @BeforeEach
    void setUp() {
        intentAnalyzerService = new IntentAnalyzerService();
    }

    @Test
    void testAnalyzeUserIntent_JiraTicketCreation() {
        // Test JIRA ticket creation intents
        assertEquals("CREATE_JIRA_TICKET", intentAnalyzerService.analyzeUserIntent("create a new ticket"));
        assertEquals("CREATE_JIRA_TICKET", intentAnalyzerService.analyzeUserIntent("we need to implement new feature"));
        assertEquals("CREATE_JIRA_TICKET", intentAnalyzerService.analyzeUserIntent("add support for authentication"));
    }

    @Test
    void testAnalyzeUserIntent_CodeFlowExplanation() {
        // Test code flow explanation intents
        assertEquals("EXPLAIN_CODE_FLOW", intentAnalyzerService.analyzeUserIntent("explain the flow of UserService"));
        assertEquals("EXPLAIN_CODE_FLOW", intentAnalyzerService.analyzeUserIntent("how does the login method work"));
        assertEquals("EXPLAIN_CODE_FLOW", intentAnalyzerService.analyzeUserIntent("trace execution path"));
    }

    @Test
    void testAnalyzeUserIntent_ArchitecturalAdvice() {
        // Test architectural advice intents
        assertEquals("ARCHITECTURAL_ADVICE", intentAnalyzerService.analyzeUserIntent("what design pattern should I use"));
        assertEquals("ARCHITECTURAL_ADVICE", intentAnalyzerService.analyzeUserIntent("architecture best practices"));
        assertEquals("ARCHITECTURAL_ADVICE", intentAnalyzerService.analyzeUserIntent("refactor this code"));
    }

    @Test
    void testAnalyzeUserIntent_GeneralDiscussion() {
        // Test general discussion intent
        assertEquals("GENERAL_DISCUSSION", intentAnalyzerService.analyzeUserIntent("hello there"));
        assertEquals("GENERAL_DISCUSSION", intentAnalyzerService.analyzeUserIntent("what is this project about"));
    }

    @Test
    void testExtractClassAndMethod() {
        // Test basic class and method extraction
        String[] result1 = intentAnalyzerService.extractClassAndMethod("UserService.login");
        assertEquals("UserService", result1[0]);
        assertEquals("login", result1[1]);

        // Test class only
        String[] result3 = intentAnalyzerService.extractClassAndMethod("OrderController");
        assertEquals("OrderController", result3[0]);
        assertNull(result3[1]);
    }

    @Test
    void testContainsClassName() {
        // Test class name detection
        assertTrue(intentAnalyzerService.containsClassName("UserService handles authentication"));
        assertTrue(intentAnalyzerService.containsClassName("PaymentController processes payments"));
        assertFalse(intentAnalyzerService.containsClassName("this is just a regular message"));
    }

    @Test
    void testIsGeneralFlowRequest() {
        // Test general flow request detection
        assertTrue(intentAnalyzerService.isGeneralFlowRequest("I want to understand the flow"));
        assertFalse(intentAnalyzerService.isGeneralFlowRequest("UserService.login flow"));
        assertFalse(intentAnalyzerService.isGeneralFlowRequest("PaymentController handles this"));
    }
}