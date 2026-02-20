package com.archpilot.service.diagram;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Service responsible for parsing flow analysis content and extracting structured information
 */
@Component
public class FlowAnalysisParser {

    /**
     * Extract involved classes from analysis content with enhanced pattern matching
     */
    public List<String> extractInvolvedClasses(String analysisContent) {
        List<String> classes = new ArrayList<>();
        
        // Look for "Involved Classes:" section first
        Pattern pattern = Pattern.compile("\\*\\*Involved Classes:\\*\\*\\s*\\[([^\\]]+)\\]");
        Matcher matcher = pattern.matcher(analysisContent);
        
        if (matcher.find()) {
            String classesStr = matcher.group(1);
            String[] classArray = classesStr.split(",");
            for (String clazz : classArray) {
                String cleanClass = clazz.trim();
                if (!cleanClass.isEmpty() && !classes.contains(cleanClass)) {
                    classes.add(cleanClass);
                }
            }
        } else {
            // Enhanced fallback: look for class names in various patterns
            // Pattern 1: Standard Java class names (PascalCase ending with common suffixes)
            Pattern classPattern1 = Pattern.compile("\\b([A-Z][a-zA-Z0-9_]*(?:Service|Controller|Repository|Component|Manager|Handler|DAO|Entity|DTO|Model|Facade|Factory|Builder|Validator|Processor|Gateway|Client|Provider|Adapter))\\b");
            Matcher classMatcher1 = classPattern1.matcher(analysisContent);
            
            while (classMatcher1.find() && classes.size() < 8) {
                String className = classMatcher1.group(1);
                if (!classes.contains(className)) {
                    classes.add(className);
                }
            }
            
            // Pattern 2: Class.method patterns
            Pattern classMethodPattern = Pattern.compile("\\b([A-Z][a-zA-Z0-9_]+)\\.([a-z][a-zA-Z0-9_]*)\\(");
            Matcher classMethodMatcher = classMethodPattern.matcher(analysisContent);
            
            while (classMethodMatcher.find() && classes.size() < 8) {
                String className = classMethodMatcher.group(1);
                if (!classes.contains(className)) {
                    classes.add(className);
                }
            }
            
            // Pattern 3: Generic class names in text
            Pattern genericClassPattern = Pattern.compile("\\b([A-Z][a-zA-Z0-9_]{2,})\\b");
            Matcher genericMatcher = genericClassPattern.matcher(analysisContent);
            
            while (genericMatcher.find() && classes.size() < 6) {
                String className = genericMatcher.group(1);
                // Filter out common words that aren't class names
                if (!className.matches("(The|This|That|When|Where|What|How|Why|Project|Method|Class|Input|Output|Return|Data|Flow|Logic|Step|Process|Start|End|Decision|Loop)") 
                    && !classes.contains(className)) {
                    classes.add(className);
                }
            }
        }
        
        return classes;
    }

    /**
     * Extract execution steps from analysis content with enhanced pattern matching
     */
    public List<String> extractExecutionSteps(String analysisContent) {
        List<String> steps = new ArrayList<>();
        
        // Look for numbered steps in "Execution Steps:" section
        Pattern pattern = Pattern.compile("\\*\\*Execution Steps:\\*\\*\\s*\\n((?:\\d+\\..*\\n?)+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(analysisContent);
        
        if (matcher.find()) {
            String stepsSection = matcher.group(1);
            String[] stepLines = stepsSection.split("\\n");
            
            for (String line : stepLines) {
                line = line.trim();
                if (line.matches("\\d+\\..*")) {
                    String step = line.replaceFirst("\\d+\\.\\s*", "").trim();
                    if (!step.isEmpty()) {
                        // Clean up the step text and ensure it contains meaningful information
                        step = cleanStepText(step);
                        steps.add(step);
                    }
                }
            }
        } else {
            // Fallback: look for step-like patterns in the content
            Pattern stepPattern = Pattern.compile("(?:^|\\n)\\s*[-•]\\s*([^\\n]+)", Pattern.MULTILINE);
            Matcher stepMatcher = stepPattern.matcher(analysisContent);
            
            while (stepMatcher.find() && steps.size() < 10) {
                String step = stepMatcher.group(1).trim();
                if (!step.isEmpty() && step.length() > 10) {
                    step = cleanStepText(step);
                    steps.add(step);
                }
            }
        }
        
        return steps;
    }
    
    /**
     * Clean and normalize step text for better diagram generation
     */
    private String cleanStepText(String step) {
        // Remove markdown formatting
        step = step.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        step = step.replaceAll("\\*([^*]+)\\*", "$1");
        
        // Don't modify step text - return as is to match test expectations
        // The tests expect exact text without additional periods
        
        // Limit length for diagram readability
        if (step.length() > 80) {
            step = step.substring(0, 77) + "...";
        }
        
        return step;
    }

    /**
     * Extract flow logic elements from analysis content with exception extraction
     */
    public List<FlowLogicElement> extractFlowLogic(String analysisContent) {
        List<FlowLogicElement> elements = new ArrayList<>();
        
        // Look for "Flow Logic:" section
        Pattern flowLogicPattern = Pattern.compile("\\*\\*Flow Logic:\\*\\*\\s*\\n(.*?)(?=\\*\\*|$)", Pattern.DOTALL);
        Matcher matcher = flowLogicPattern.matcher(analysisContent);
        
        if (matcher.find()) {
            String flowLogicSection = matcher.group(1);
            String[] lines = flowLogicSection.split("\\n");
            
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("- START:")) {
                    elements.add(new FlowLogicElement("START", line.substring(8).trim(), null));
                } else if (line.startsWith("- DECISION:")) {
                    String description = line.substring(11).trim();
                    String exceptionName = extractExceptionName(description);
                    elements.add(new FlowLogicElement("DECISION", description, exceptionName));
                } else if (line.startsWith("- LOOP:")) {
                    elements.add(new FlowLogicElement("LOOP", line.substring(7).trim(), null));
                } else if (line.startsWith("- PROCESS:")) {
                    elements.add(new FlowLogicElement("PROCESS", line.substring(10).trim(), null));
                } else if (line.startsWith("- END:")) {
                    elements.add(new FlowLogicElement("END", line.substring(6).trim(), null));
                }
            }
        }
        
        return elements;
    }
    
    /**
     * Extract exception name from decision description
     */
    private String extractExceptionName(String description) {
        // Look for "throw ExceptionName" pattern
        Pattern exceptionPattern = Pattern.compile("throw\\s+([A-Z][a-zA-Z0-9]*Exception)");
        Matcher matcher = exceptionPattern.matcher(description);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Extract sequence interactions from analysis content
     */
    public List<SequenceInteraction> extractSequenceInteractions(String analysisContent) {
        List<SequenceInteraction> interactions = new ArrayList<>();
        
        // Look for "Sequence Interactions:" section
        Pattern sequencePattern = Pattern.compile("\\*\\*Sequence Interactions:\\*\\*\\s*\\n(.*?)(?=\\*\\*|$)", Pattern.DOTALL);
        Matcher matcher = sequencePattern.matcher(analysisContent);
        
        if (matcher.find()) {
            String sequenceSection = matcher.group(1);
            String[] lines = sequenceSection.split("\\n");
            
            for (String line : lines) {
                line = line.trim();
                // Parse format: "1. SourceClass -> TargetClass.methodName(params)"
                Pattern interactionPattern = Pattern.compile("\\d+\\.\\s*([A-Za-z0-9_]+)\\s*->\\s*([A-Za-z0-9_]+)\\.([a-zA-Z0-9_]+)\\(([^)]*)\\)");
                Matcher interactionMatcher = interactionPattern.matcher(line);
                
                if (interactionMatcher.find()) {
                    String source = interactionMatcher.group(1);
                    String target = interactionMatcher.group(2);
                    String method = interactionMatcher.group(3);
                    String params = interactionMatcher.group(4);
                    interactions.add(new SequenceInteraction(source, target, method, params));
                }
            }
        }
        
        return interactions;
    }
    
    /**
     * Extract exception types from analysis content
     */
    public List<String> extractExceptionTypes(String analysisContent) {
        List<String> exceptions = new ArrayList<>();
        
        // Look for "Exception Handling:" section
        Pattern exceptionPattern = Pattern.compile("\\*\\*Exception Handling:\\*\\*\\s*\\n(.*?)(?=\\*\\*|$)", Pattern.DOTALL);
        Matcher matcher = exceptionPattern.matcher(analysisContent);
        
        if (matcher.find()) {
            String exceptionSection = matcher.group(1);
            // Extract exception names (words ending with "Exception")
            Pattern namePattern = Pattern.compile("\\b([A-Z][a-zA-Z0-9]*Exception)\\b");
            Matcher nameMatcher = namePattern.matcher(exceptionSection);
            
            while (nameMatcher.find()) {
                String exceptionName = nameMatcher.group(1);
                if (!exceptions.contains(exceptionName)) {
                    exceptions.add(exceptionName);
                }
            }
        }
        
        return exceptions;
    }

    /**
     * Represents a flow logic element extracted from analysis
     */
    public static class FlowLogicElement {
        private final String type;
        private final String description;
        private final String exceptionName;
        
        public FlowLogicElement(String type, String description, String exceptionName) {
            this.type = type;
            this.description = description;
            this.exceptionName = exceptionName;
        }
        
        public String getType() {
            return type;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getExceptionName() {
            return exceptionName;
        }
    }
    
    /**
     * Represents a sequence interaction extracted from analysis
     */
    public static class SequenceInteraction {
        private final String source;
        private final String target;
        private final String method;
        private final String parameters;
        
        public SequenceInteraction(String source, String target, String method, String parameters) {
            this.source = source;
            this.target = target;
            this.method = method;
            this.parameters = parameters;
        }
        
        public String getSource() {
            return source;
        }
        
        public String getTarget() {
            return target;
        }
        
        public String getMethod() {
            return method;
        }
        
        public String getParameters() {
            return parameters;
        }
    }
}