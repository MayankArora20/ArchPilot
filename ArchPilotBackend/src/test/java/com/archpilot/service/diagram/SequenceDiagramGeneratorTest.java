package com.archpilot.service.diagram;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SequenceDiagramGeneratorTest {

    @Mock
    private FlowAnalysisParser flowAnalysisParser;

    @InjectMocks
    private SequenceDiagramGenerator sequenceDiagramGenerator;

    @BeforeEach
    void setUp() {
        reset(flowAnalysisParser);
    }

    @Test
    void testGenerateSequenceDiagram_WithInvolvedClasses() {
        String className = "PaymentController";
        String methodName = "processPayment";
        String analysisContent = "Sample analysis content";

        List<String> involvedClasses = Arrays.asList(
            "PaymentService",
            "ValidationService", 
            "BankService",
            "NotificationService"
        );

        when(flowAnalysisParser.extractInvolvedClasses(analysisContent)).thenReturn(involvedClasses);

        String result = sequenceDiagramGenerator.generateSequenceDiagram(className, methodName, analysisContent);

        assertNotNull(result);
        assertTrue(result.contains("@startuml"));
        assertTrue(result.contains("@enduml"));
        assertTrue(result.contains("title Sequence Diagram - PaymentController.processPayment"));
        
        // Check participants
        assertTrue(result.contains("participant \"Client\" as Client"));
        assertTrue(result.contains("participant \"PaymentService\" as PaymentService"));
        assertTrue(result.contains("participant \"ValidationService\" as ValidationService"));
        assertTrue(result.contains("participant \"BankService\" as BankService"));
        assertTrue(result.contains("participant \"NotificationService\" as NotificationService"));
        
        // Check basic sequence flow
        assertTrue(result.contains("Client -> PaymentService : processPayment()"));
        assertTrue(result.contains("PaymentService -> ValidationService : process()"));
        assertTrue(result.contains("ValidationService --> PaymentService : result"));
        assertTrue(result.contains("ValidationService -> BankService : process()"));
        assertTrue(result.contains("BankService --> ValidationService : result"));
        assertTrue(result.contains("BankService -> NotificationService : process()"));
        assertTrue(result.contains("NotificationService --> BankService : result"));
        assertTrue(result.contains("PaymentService --> Client : response"));

        verify(flowAnalysisParser).extractInvolvedClasses(analysisContent);
    }

    @Test
    void testGenerateSequenceDiagram_WithoutMethodName() {
        String className = "UserController";
        String methodName = null;
        String analysisContent = "Sample analysis content";

        List<String> involvedClasses = Arrays.asList("UserService", "DatabaseService");

        when(flowAnalysisParser.extractInvolvedClasses(analysisContent)).thenReturn(involvedClasses);

        String result = sequenceDiagramGenerator.generateSequenceDiagram(className, methodName, analysisContent);

        assertNotNull(result);
        assertTrue(result.contains("title Sequence Diagram - UserController"));
        assertFalse(result.contains("UserController."));
        assertTrue(result.contains("Client -> UserService : request"));
        assertFalse(result.contains("processPayment()"));
    }

    @Test
    void testGenerateSequenceDiagram_WithNoInvolvedClasses() {
        String className = "SimpleService";
        String methodName = "simpleMethod";
        String analysisContent = "Sample analysis content";

        when(flowAnalysisParser.extractInvolvedClasses(analysisContent)).thenReturn(Collections.emptyList());

        String result = sequenceDiagramGenerator.generateSequenceDiagram(className, methodName, analysisContent);

        assertNotNull(result);
        assertTrue(result.contains("@startuml"));
        assertTrue(result.contains("@enduml"));
        assertTrue(result.contains("title Sequence Diagram - SimpleService.simpleMethod"));
        assertTrue(result.contains("participant \"Client\" as Client"));
        
        // Should not contain any sequence interactions since no classes are involved
        assertFalse(result.contains("Client ->"));
        assertFalse(result.contains("-> Client"));
    }

    @Test
    void testGenerateSequenceDiagram_WithSingleInvolvedClass() {
        String className = "AuthService";
        String methodName = "authenticate";
        String analysisContent = "Sample analysis content";

        List<String> involvedClasses = Arrays.asList("TokenService");

        when(flowAnalysisParser.extractInvolvedClasses(analysisContent)).thenReturn(involvedClasses);

        String result = sequenceDiagramGenerator.generateSequenceDiagram(className, methodName, analysisContent);

        assertNotNull(result);
        assertTrue(result.contains("participant \"TokenService\" as TokenService"));
        assertTrue(result.contains("Client -> TokenService : authenticate()"));
        assertTrue(result.contains("TokenService --> Client : response"));
        
        // Should not contain interactions between multiple services
        assertFalse(result.contains("TokenService -> "));
        assertFalse(result.contains(" --> TokenService"));
    }

    @Test
    void testGenerateSequenceDiagram_WithSpecialCharactersInClassNames() {
        String className = "Payment-Controller";
        String methodName = "process_payment";
        String analysisContent = "Sample analysis content";

        List<String> involvedClasses = Arrays.asList("Payment-Service", "Bank$Service");

        when(flowAnalysisParser.extractInvolvedClasses(analysisContent)).thenReturn(involvedClasses);

        String result = sequenceDiagramGenerator.generateSequenceDiagram(className, methodName, analysisContent);

        assertNotNull(result);
        // Check that special characters are cleaned for participant aliases
        assertTrue(result.contains("participant \"Payment-Service\" as PaymentService"));
        assertTrue(result.contains("participant \"Bank$Service\" as BankService"));
        assertTrue(result.contains("Client -> PaymentService : process_payment()"));
        assertTrue(result.contains("PaymentService -> BankService : process()"));
    }

    @Test
    void testGenerateSequenceDiagram_WithMultipleInvolvedClasses() {
        String className = "OrderController";
        String methodName = "createOrder";
        String analysisContent = "Sample analysis content";

        List<String> involvedClasses = Arrays.asList(
            "OrderService",
            "InventoryService",
            "PaymentService",
            "ShippingService",
            "EmailService"
        );

        when(flowAnalysisParser.extractInvolvedClasses(analysisContent)).thenReturn(involvedClasses);

        String result = sequenceDiagramGenerator.generateSequenceDiagram(className, methodName, analysisContent);

        assertNotNull(result);
        
        // Verify all participants are included
        for (String clazz : involvedClasses) {
            assertTrue(result.contains("participant \"" + clazz + "\""));
        }
        
        // Verify sequence flow between all classes
        assertTrue(result.contains("Client -> OrderService : createOrder()"));
        assertTrue(result.contains("OrderService -> InventoryService : process()"));
        assertTrue(result.contains("InventoryService -> PaymentService : process()"));
        assertTrue(result.contains("PaymentService -> ShippingService : process()"));
        assertTrue(result.contains("ShippingService -> EmailService : process()"));
        assertTrue(result.contains("OrderService --> Client : response"));
    }

    @Test
    void testGenerateSequenceDiagram_EmptyAnalysisContent() {
        String className = "TestService";
        String methodName = "testMethod";
        String analysisContent = "";

        when(flowAnalysisParser.extractInvolvedClasses(analysisContent)).thenReturn(Collections.emptyList());

        String result = sequenceDiagramGenerator.generateSequenceDiagram(className, methodName, analysisContent);

        assertNotNull(result);
        assertTrue(result.contains("@startuml"));
        assertTrue(result.contains("@enduml"));
        assertTrue(result.contains("title Sequence Diagram - TestService.testMethod"));
        assertTrue(result.contains("participant \"Client\" as Client"));
    }
}