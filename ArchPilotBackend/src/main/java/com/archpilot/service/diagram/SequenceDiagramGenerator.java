package com.archpilot.service.diagram;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service responsible for generating PlantUML sequence diagrams from analysis content
 */
@Component
public class SequenceDiagramGenerator {

    @Autowired
    private FlowAnalysisParser flowAnalysisParser;

    /**
     * Generate PlantUML sequence diagram from analysis content with enhanced method extraction
     */
    public String generateSequenceDiagram(String className, String methodName, String analysisContent) {
        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("title Sequence Diagram - ").append(className);
        if (methodName != null) {
            puml.append(".").append(methodName);
        }
        puml.append("\n\n");
        
        // Try to extract explicit sequence interactions first
        List<FlowAnalysisParser.SequenceInteraction> explicitInteractions = 
            flowAnalysisParser.extractSequenceInteractions(analysisContent);
        
        if (!explicitInteractions.isEmpty()) {
            // Use explicit sequence interactions from analysis
            generateFromExplicitInteractions(puml, className, methodName, explicitInteractions);
        } else {
            // Fallback to class-based generation
            generateFromClasses(puml, className, methodName, analysisContent);
        }
        
        puml.append("\n@enduml");
        return puml.toString();
    }
    
    /**
     * Generate sequence diagram from explicit interactions defined in analysis
     */
    private void generateFromExplicitInteractions(StringBuilder puml, String className, String methodName, 
                                                   List<FlowAnalysisParser.SequenceInteraction> interactions) {
        // Collect all unique participants
        List<String> participants = new ArrayList<>();
        participants.add("Client");
        
        for (FlowAnalysisParser.SequenceInteraction interaction : interactions) {
            if (!participants.contains(interaction.getSource()) && !interaction.getSource().equals("Client")) {
                participants.add(interaction.getSource());
            }
            if (!participants.contains(interaction.getTarget())) {
                participants.add(interaction.getTarget());
            }
        }
        
        // Add participants
        for (String participant : participants) {
            String cleanName = participant.replaceAll("[^a-zA-Z0-9]", "");
            puml.append("participant \"").append(participant).append("\" as ").append(cleanName).append("\n");
        }
        puml.append("\n");
        
        // Generate interactions in the exact order specified
        String currentActive = null;
        for (FlowAnalysisParser.SequenceInteraction interaction : interactions) {
            String source = interaction.getSource().replaceAll("[^a-zA-Z0-9]", "");
            String target = interaction.getTarget().replaceAll("[^a-zA-Z0-9]", "");
            String method = interaction.getMethod();
            String params = interaction.getParameters();
            
            // Activate target if not already active
            if (currentActive == null || !currentActive.equals(target)) {
                if (currentActive != null && !currentActive.equals(source)) {
                    puml.append("deactivate ").append(currentActive).append("\n");
                }
                puml.append("activate ").append(target).append("\n");
                currentActive = target;
            }
            
            // Generate the call
            puml.append(source).append(" -> ").append(target).append(" : ").append(method).append("(");
            if (params != null && !params.isEmpty()) {
                puml.append(params);
            }
            puml.append(")\n");
            
            // Return response
            puml.append(target).append(" --> ").append(source).append(" : result\n");
        }
        
        // Deactivate last active participant
        if (currentActive != null) {
            puml.append("deactivate ").append(currentActive).append("\n");
        }
    }
    
    /**
     * Generate sequence diagram from classes (fallback method)
     */
    private void generateFromClasses(StringBuilder puml, String className, String methodName, String analysisContent) {
        // Extract involved classes from analysis
        List<String> originalClasses = flowAnalysisParser.extractInvolvedClasses(analysisContent);
        List<String> involvedClasses = new ArrayList<>(originalClasses);
        
        // Ensure the main class is first in the list
        if (!involvedClasses.isEmpty() && !involvedClasses.get(0).equals(className)) {
            involvedClasses.remove(className);
            involvedClasses.add(0, className);
        } else if (involvedClasses.isEmpty()) {
            involvedClasses.add(className);
        }
        
        // Add participants
        puml.append("participant \"Client\" as Client\n");
        for (String clazz : involvedClasses) {
            String cleanClass = clazz.replaceAll("[^a-zA-Z0-9]", "");
            puml.append("participant \"").append(clazz).append("\" as ").append(cleanClass).append("\n");
        }
        puml.append("\n");
        
        // Generate sequence interactions based on execution steps
        List<String> steps = flowAnalysisParser.extractExecutionSteps(analysisContent);
        
        if (!involvedClasses.isEmpty()) {
            String firstClass = involvedClasses.get(0).replaceAll("[^a-zA-Z0-9]", "");
            
            // Initial call
            puml.append("Client -> ").append(firstClass).append(" : ");
            if (methodName != null) {
                puml.append(methodName).append("()");
            } else {
                puml.append("request");
            }
            puml.append("\n");
            
            // Add activation
            puml.append("activate ").append(firstClass).append("\n");
            
            // Generate interactions based on steps and involved classes
            generateSequenceInteractionsFromSteps(puml, involvedClasses, steps, analysisContent);
            
            // Final return
            puml.append(firstClass).append(" --> Client : response\n");
            puml.append("deactivate ").append(firstClass).append("\n");
        }
    }
    
    /**
     * Generate sequence interactions based on steps and classes
     */
    private void generateSequenceInteractionsFromSteps(StringBuilder puml, List<String> classes, 
                                                       List<String> steps, String analysisContent) {
        if (classes.size() < 2) return;
        
        // Try to extract method calls from steps
        for (int i = 0; i < classes.size() - 1 && i < steps.size(); i++) {
            String currentClass = classes.get(i).replaceAll("[^a-zA-Z0-9]", "");
            String nextClass = classes.get(i + 1).replaceAll("[^a-zA-Z0-9]", "");
            
            // Extract method call from step if available
            String methodCall = extractMethodCallFromStep(steps.get(i), classes.get(i + 1));
            
            puml.append(currentClass).append(" -> ").append(nextClass).append(" : ").append(methodCall).append("\n");
            puml.append(nextClass).append(" --> ").append(currentClass).append(" : result\n");
        }
    }
    
    /**
     * Extract method call from execution step
     */
    private String extractMethodCallFromStep(String step, String targetClass) {
        // Look for ClassName.methodName() pattern
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            targetClass + "\\.([a-zA-Z0-9_]+)\\(([^)]*)\\)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(step);
        
        if (matcher.find()) {
            String method = matcher.group(1);
            String params = matcher.group(2);
            if (params != null && !params.isEmpty()) {
                return method + "(" + params + ")";
            }
            return method + "()";
        }
        
        // Look for just methodName() pattern
        pattern = java.util.regex.Pattern.compile("([a-z][a-zA-Z0-9_]*)\\(([^)]*)\\)");
        matcher = pattern.matcher(step);
        
        if (matcher.find()) {
            String method = matcher.group(1);
            String params = matcher.group(2);
            if (params != null && !params.isEmpty()) {
                return method + "(" + params + ")";
            }
            return method + "()";
        }
        
        // Infer method from class type
        return inferMethodFromClassName(targetClass);
    }
    
    /**
     * Infer likely method name from class type
     */
    private String inferMethodFromClassName(String className) {
        String lowerName = className.toLowerCase();
        
        if (lowerName.contains("validation") || lowerName.contains("validator")) {
            return "validate()";
        } else if (lowerName.contains("repository") || lowerName.contains("dao")) {
            return "findById()";
        } else if (lowerName.contains("notification")) {
            return "sendNotification()";
        } else if (lowerName.contains("inventory")) {
            return "checkAvailability()";
        } else if (lowerName.contains("payment")) {
            return "processPayment()";
        } else if (lowerName.contains("order")) {
            return "processOrder()";
        }
        
        return "process()";
    }
}