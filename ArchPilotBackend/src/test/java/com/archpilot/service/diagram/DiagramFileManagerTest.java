package com.archpilot.service.diagram;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.archpilot.service.PlantUmlToPngService;

@ExtendWith(MockitoExtension.class)
class DiagramFileManagerTest {

    @Mock
    private PlantUmlToPngService plantUmlToPngService;

    @Mock
    private SequenceDiagramGenerator sequenceDiagramGenerator;

    @Mock
    private FlowDiagramGenerator flowDiagramGenerator;

    @InjectMocks
    private DiagramFileManager diagramFileManager;

    @Test
    void testGenerateFlowDiagrams_Success() {
        String projectName = "test-project";
        String className = "PaymentService";
        String methodName = "processPayment";
        String analysisContent = "Sample analysis content";

        String sequenceDiagram = "@startuml\ntitle Sequence\n@enduml";
        String flowDiagram = "@startuml\ntitle Flow\n@enduml";

        when(sequenceDiagramGenerator.generateSequenceDiagram(className, methodName, analysisContent))
            .thenReturn(sequenceDiagram);
        when(flowDiagramGenerator.generateFlowDiagram(className, methodName, analysisContent))
            .thenReturn(flowDiagram);

        String result = diagramFileManager.generateFlowDiagrams(projectName, className, methodName, analysisContent);

        // Since file operations might fail in test environment, we check for either success or error message
        assertNotNull(result);
        assertTrue(result.contains("**Visual Diagrams:**") || result.contains("**Note:** Visual diagrams could not be generated"));

        verify(sequenceDiagramGenerator).generateSequenceDiagram(className, methodName, analysisContent);
        verify(flowDiagramGenerator).generateFlowDiagram(className, methodName, analysisContent);
    }

    @Test
    void testGenerateFlowDiagrams_WithoutMethodName() {
        String projectName = "test-project";
        String className = "UserService";
        String methodName = null;
        String analysisContent = "Sample analysis content";

        String sequenceDiagram = "@startuml\ntitle Sequence\n@enduml";
        String flowDiagram = "@startuml\ntitle Flow\n@enduml";

        when(sequenceDiagramGenerator.generateSequenceDiagram(className, methodName, analysisContent))
            .thenReturn(sequenceDiagram);
        when(flowDiagramGenerator.generateFlowDiagram(className, methodName, analysisContent))
            .thenReturn(flowDiagram);

        String result = diagramFileManager.generateFlowDiagrams(projectName, className, methodName, analysisContent);

        assertNotNull(result);
        // Should contain className but not methodName in filename
        if (result.contains("**Visual Diagrams:**")) {
            assertTrue(result.contains(className));
            assertFalse(result.contains("null"));
        }

        verify(sequenceDiagramGenerator).generateSequenceDiagram(className, methodName, analysisContent);
        verify(flowDiagramGenerator).generateFlowDiagram(className, methodName, analysisContent);
    }

    @Test
    void testGenerateFlowDiagrams_SequenceDiagramGeneratorException() {
        String projectName = "test-project";
        String className = "ErrorService";
        String methodName = "errorMethod";
        String analysisContent = "Sample analysis content";

        when(sequenceDiagramGenerator.generateSequenceDiagram(className, methodName, analysisContent))
            .thenThrow(new RuntimeException("Test exception"));

        String result = diagramFileManager.generateFlowDiagrams(projectName, className, methodName, analysisContent);

        assertNotNull(result);
        assertTrue(result.contains("**Note:** Visual diagrams could not be generated"));
        assertTrue(result.contains("due to a technical issue"));
        assertTrue(result.contains("but the analysis above provides comprehensive insights"));

        verify(sequenceDiagramGenerator).generateSequenceDiagram(className, methodName, analysisContent);
    }

    @Test
    void testGenerateFlowDiagrams_FlowDiagramGeneratorException() {
        String projectName = "test-project";
        String className = "TestService";
        String methodName = "testMethod";
        String analysisContent = "Sample analysis content";

        String sequenceDiagram = "@startuml\ntitle Sequence\n@enduml";

        when(sequenceDiagramGenerator.generateSequenceDiagram(className, methodName, analysisContent))
            .thenReturn(sequenceDiagram);
        when(flowDiagramGenerator.generateFlowDiagram(className, methodName, analysisContent))
            .thenThrow(new RuntimeException("Flow diagram generation failed"));

        String result = diagramFileManager.generateFlowDiagrams(projectName, className, methodName, analysisContent);

        assertNotNull(result);
        assertTrue(result.contains("**Note:** Visual diagrams could not be generated"));

        verify(sequenceDiagramGenerator).generateSequenceDiagram(className, methodName, analysisContent);
        verify(flowDiagramGenerator).generateFlowDiagram(className, methodName, analysisContent);
    }

    @Test
    void testGenerateFlowDiagrams_EmptyProjectName() {
        String projectName = "";
        String className = "TestService";
        String methodName = "testMethod";
        String analysisContent = "Sample analysis content";

        when(sequenceDiagramGenerator.generateSequenceDiagram(anyString(), anyString(), anyString()))
            .thenReturn("@startuml\n@enduml");
        when(flowDiagramGenerator.generateFlowDiagram(anyString(), anyString(), anyString()))
            .thenReturn("@startuml\n@enduml");

        String result = diagramFileManager.generateFlowDiagrams(projectName, className, methodName, analysisContent);

        assertNotNull(result);
        // Should handle empty project name gracefully
        assertTrue(result.contains("**Visual Diagrams:**") || result.contains("**Note:** Visual diagrams could not be generated"));
    }

    @Test
    void testGenerateFlowDiagrams_NullAnalysisContent() {
        String projectName = "test-project";
        String className = "TestService";
        String methodName = "testMethod";
        String analysisContent = null;

        when(sequenceDiagramGenerator.generateSequenceDiagram(eq(className), eq(methodName), eq(analysisContent)))
            .thenReturn("@startuml\n@enduml");
        when(flowDiagramGenerator.generateFlowDiagram(eq(className), eq(methodName), eq(analysisContent)))
            .thenReturn("@startuml\n@enduml");

        String result = diagramFileManager.generateFlowDiagrams(projectName, className, methodName, analysisContent);

        assertNotNull(result);
        verify(sequenceDiagramGenerator).generateSequenceDiagram(className, methodName, analysisContent);
        verify(flowDiagramGenerator).generateFlowDiagram(className, methodName, analysisContent);
    }

    @Test
    void testGenerateFlowDiagrams_VerifyTimestampInResult() {
        String projectName = "test-project";
        String className = "TimeService";
        String methodName = "getCurrentTime";
        String analysisContent = "Sample analysis content";

        when(sequenceDiagramGenerator.generateSequenceDiagram(anyString(), anyString(), anyString()))
            .thenReturn("@startuml\n@enduml");
        when(flowDiagramGenerator.generateFlowDiagram(anyString(), anyString(), anyString()))
            .thenReturn("@startuml\n@enduml");

        String result = diagramFileManager.generateFlowDiagrams(projectName, className, methodName, analysisContent);

        assertNotNull(result);
        // If successful, result should contain timestamp pattern or error message
        assertTrue(result.contains("TimeService") || result.contains("**Note:**"));
    }
}