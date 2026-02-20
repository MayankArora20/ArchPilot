package com.archpilot.service.diagram;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.archpilot.service.PlantUmlToPngService;

/**
 * Service responsible for managing diagram files and generating HTML links
 */
@Component
public class DiagramFileManager {

    private static final Logger logger = LoggerFactory.getLogger(DiagramFileManager.class);

    @Autowired
    private PlantUmlToPngService plantUmlToPngService;

    @Autowired
    private SequenceDiagramGenerator sequenceDiagramGenerator;

    @Autowired
    private FlowDiagramGenerator flowDiagramGenerator;

    /**
     * Generate visual flow diagrams and return HTML links
     */
    public String generateFlowDiagrams(String projectName, String className, String methodName, String analysisContent) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String baseFileName = className + (methodName != null ? methodName : "") + "_" + timestamp;
            
            // Create ArchpilotResource directory structure
            Path resourcePath = Paths.get("ArchpilotResource", projectName);
            Files.createDirectories(resourcePath);
            
            // Generate sequence diagram
            String sequenceDiagram = sequenceDiagramGenerator.generateSequenceDiagram(className, methodName, analysisContent);
            Path sequenceFile = resourcePath.resolve(baseFileName + "-sequence.puml");
            Files.writeString(sequenceFile, sequenceDiagram);
            
            // Generate flow diagram
            String flowDiagram = flowDiagramGenerator.generateFlowDiagram(className, methodName, analysisContent);
            Path flowFile = resourcePath.resolve(baseFileName + "-flow.puml");
            Files.writeString(flowFile, flowDiagram);
            
            // Convert PUML files to PNG
            plantUmlToPngService.convertPumlFileToPng(sequenceFile.toString());
            plantUmlToPngService.convertPumlFileToPng(flowFile.toString());
            
            // Generate HTML links
            StringBuilder links = new StringBuilder();
            links.append("**Visual Diagrams:**\n");
            links.append("Have a look at:\n");
            links.append("<a href=\"/api/flow/diagram/").append(projectName)
                 .append("/").append(baseFileName).append("-sequence.png\">Sequence Diagram</a>\n");
            links.append("<a href=\"/api/flow/diagram/").append(projectName)
                 .append("/").append(baseFileName).append("-flow.png\">Flow Diagram</a>");
            
            logger.info("Generated flow diagrams for {}.{}", className, methodName);
            return links.toString();
            
        } catch (Exception e) {
            logger.error("Error generating flow diagrams: {}", e.getMessage(), e);
            return "**Note:** Visual diagrams could not be generated due to a technical issue, but the analysis above provides comprehensive insights.";
        }
    }
}