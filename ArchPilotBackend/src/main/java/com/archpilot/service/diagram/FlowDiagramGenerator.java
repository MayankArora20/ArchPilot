package com.archpilot.service.diagram;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.archpilot.service.diagram.FlowAnalysisParser.FlowLogicElement;

/**
 * Service responsible for generating PlantUML flow diagrams from analysis content
 */
@Component
public class FlowDiagramGenerator {

    @Autowired
    private FlowAnalysisParser flowAnalysisParser;

    /**
     * Generate PlantUML flow diagram from analysis content with proper activity diagram syntax
     */
    public String generateFlowDiagram(String className, String methodName, String analysisContent) {
        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("title Flow Diagram - ").append(className);
        if (methodName != null) {
            puml.append(".").append(methodName);
        }
        puml.append("\n\n");
        
        puml.append("start\n");
        
        // Try to extract structured flow logic from analysis
        List<FlowLogicElement> flowElements = flowAnalysisParser.extractFlowLogic(analysisContent);
        
        if (!flowElements.isEmpty()) {
            generateFlowFromElements(puml, flowElements);
        } else {
            // Fallback: Generate flow from execution steps
            List<String> steps = flowAnalysisParser.extractExecutionSteps(analysisContent);
            generateFlowFromSteps(puml, steps, className, methodName);
        }
        
        puml.append("stop\n");
        puml.append("\n@enduml");
        return puml.toString();
    }

    /**
     * Generate flow from structured flow elements with specific exception handling
     */
    private void generateFlowFromElements(StringBuilder puml, List<FlowLogicElement> elements) {
        for (FlowLogicElement element : elements) {
            switch (element.getType()) {
                case "START" -> puml.append(":").append(element.getDescription()).append(";\n");
                case "DECISION" -> {
                    puml.append("if (").append(element.getDescription()).append("?) then (yes)\n");
                    // If there's a specific exception, add it to the no path
                    if (element.getExceptionName() != null) {
                        puml.append("else (no)\n");
                        puml.append(":Throw ").append(element.getExceptionName()).append(";\n");
                        puml.append("stop\n");
                        puml.append("endif\n");
                    }
                }
                case "LOOP" -> {
                    puml.append("repeat\n");
                    puml.append(":").append(element.getDescription()).append(";\n");
                }
                case "PROCESS" -> puml.append(":").append(element.getDescription()).append(";\n");
                case "END" -> puml.append(":").append(element.getDescription()).append(";\n");
            }
        }
    }

    /**
     * Generate flow from execution steps with intelligent decision point detection and exception extraction
     */
    private void generateFlowFromSteps(StringBuilder puml, List<String> steps, String className, String methodName) {
        if (steps.isEmpty()) {
            // Generate a basic flow based on method name and class
            generateBasicFlow(puml, className, methodName);
            return;
        }
        
        for (int i = 0; i < steps.size(); i++) {
            String step = steps.get(i);
            String lowerStep = step.toLowerCase();
            
            // Detect decision points
            if (lowerStep.contains("check") || lowerStep.contains("validate") || 
                lowerStep.contains("verify") || lowerStep.contains("exists") ||
                lowerStep.contains("if") || lowerStep.contains("condition")) {
                
                puml.append("if (").append(step).append("?) then (yes)\n");
                puml.append("else (no)\n");
                
                // Try to extract specific exception name from step
                String exceptionName = extractExceptionFromStep(step);
                if (exceptionName != null) {
                    puml.append(":Throw ").append(exceptionName).append(";\n");
                    puml.append("stop\n");
                } else {
                    puml.append(":Handle error or alternative path;\n");
                }
                puml.append("endif\n");
                
            } else if (lowerStep.contains("loop") || lowerStep.contains("iterate") || 
                      lowerStep.contains("repeat") || lowerStep.contains("for each") ||
                      lowerStep.contains("while")) {
                
                puml.append("repeat\n");
                puml.append(":").append(step).append(";\n");
                
                // Add next step as part of the loop if available
                if (i + 1 < steps.size()) {
                    puml.append(":").append(steps.get(i + 1)).append(";\n");
                    i++; // Skip the next step as it's part of the loop
                }
                
                puml.append("repeat while (More items?)\n");
                
            } else {
                // Regular process step
                puml.append(":").append(step).append(";\n");
            }
        }
    }
    
    /**
     * Extract exception name from step description
     */
    private String extractExceptionFromStep(String step) {
        // Look for "throw ExceptionName" pattern
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("throw\\s+([A-Z][a-zA-Z0-9]*Exception)");
        java.util.regex.Matcher matcher = pattern.matcher(step);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Generate a basic flow when no detailed steps are available
     */
    private void generateBasicFlow(StringBuilder puml, String className, String methodName) {
        if (methodName != null) {
            String lowerMethod = methodName.toLowerCase();
            
            if (lowerMethod.contains("process") || lowerMethod.contains("handle")) {
                puml.append(":Receive input parameters;\n");
                puml.append("if (Input valid?) then (yes)\n");
                puml.append(":Process request;\n");
                puml.append(":Execute business logic;\n");
                puml.append(":Return result;\n");
                puml.append("else (no)\n");
                puml.append(":Return error;\n");
                puml.append("endif\n");
                
            } else if (lowerMethod.contains("create") || lowerMethod.contains("add")) {
                puml.append(":Receive creation parameters;\n");
                puml.append(":Validate input data;\n");
                puml.append("if (Data valid?) then (yes)\n");
                puml.append(":Create new entity;\n");
                puml.append(":Save to database;\n");
                puml.append(":Return created entity;\n");
                puml.append("else (no)\n");
                puml.append(":Return validation error;\n");
                puml.append("endif\n");
                
            } else if (lowerMethod.contains("update") || lowerMethod.contains("modify")) {
                puml.append(":Receive update parameters;\n");
                puml.append("if (Entity exists?) then (yes)\n");
                puml.append(":Validate update data;\n");
                puml.append(":Apply changes;\n");
                puml.append(":Save changes;\n");
                puml.append(":Return updated entity;\n");
                puml.append("else (no)\n");
                puml.append(":Return not found error;\n");
                puml.append("endif\n");
                
            } else if (lowerMethod.contains("delete") || lowerMethod.contains("remove")) {
                puml.append(":Receive entity identifier;\n");
                puml.append("if (Entity exists?) then (yes)\n");
                puml.append(":Check dependencies;\n");
                puml.append("if (Safe to delete?) then (yes)\n");
                puml.append(":Delete entity;\n");
                puml.append(":Return success;\n");
                puml.append("else (no)\n");
                puml.append(":Return dependency error;\n");
                puml.append("endif\n");
                puml.append("else (no)\n");
                puml.append(":Return not found error;\n");
                puml.append("endif\n");
                
            } else if (lowerMethod.contains("get") || lowerMethod.contains("find") || lowerMethod.contains("retrieve")) {
                puml.append(":Receive search parameters;\n");
                puml.append(":Query database;\n");
                puml.append("if (Results found?) then (yes)\n");
                puml.append(":Format results;\n");
                puml.append(":Return data;\n");
                puml.append("else (no)\n");
                puml.append(":Return empty result;\n");
                puml.append("endif\n");
                
            } else {
                // Generic flow
                puml.append(":Initialize method;\n");
                puml.append(":Execute main logic;\n");
                puml.append(":Process results;\n");
                puml.append(":Return response;\n");
            }
        } else {
            // Class-level flow
            puml.append(":Initialize ").append(className).append(";\n");
            puml.append(":Execute main functionality;\n");
            puml.append(":Process business logic;\n");
            puml.append(":Return results;\n");
        }
    }
}