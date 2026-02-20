package com.archpilot.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * Intent Analyzer Service
 * 
 * Analyzes user messages to determine their intent and extract relevant information.
 * This service helps route user requests to appropriate handlers based on detected patterns.
 */
@Service
public class IntentAnalyzerService {

    /**
     * Analyze user message to determine intent
     * 
     * @param userMessage The user's input message
     * @return Intent type as string
     */
    public String analyzeUserIntent(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();
        
        // Keywords for JIRA ticket creation
        if (lowerMessage.contains("create") && (lowerMessage.contains("ticket") || lowerMessage.contains("jira") || 
            lowerMessage.contains("story") || lowerMessage.contains("requirement"))) {
            return "CREATE_JIRA_TICKET";
        }
        
        if (lowerMessage.contains("new feature") || lowerMessage.contains("implement") || 
            lowerMessage.contains("add functionality") || lowerMessage.contains("user story")) {
            return "CREATE_JIRA_TICKET";
        }
        
        // Check if the message describes a new requirement or feature request
        if (lowerMessage.contains("we need") || lowerMessage.contains("we should") || 
            lowerMessage.contains("support") || lowerMessage.contains("add support for")) {
            return "CREATE_JIRA_TICKET";
        }
        
        // Enhanced keywords for code flow explanation with visual diagrams
        if (lowerMessage.contains("explain") && (lowerMessage.contains("flow") || lowerMessage.contains("method") || 
            lowerMessage.contains("function") || lowerMessage.contains("class"))) {
            return "EXPLAIN_CODE_FLOW";
        }
        
        if (lowerMessage.contains("how does") || lowerMessage.contains("what happens when") || 
            lowerMessage.contains("trace") || lowerMessage.contains("analyze")) {
            return "EXPLAIN_CODE_FLOW";
        }
        
        // Additional flow analysis keywords
        if (lowerMessage.matches(".*understand.*flow.*") || lowerMessage.matches(".*show.*flow.*") ||
            lowerMessage.matches(".*trace.*execution.*") || lowerMessage.matches(".*walk.*through.*") ||
            lowerMessage.matches(".*step.*by.*step.*") || lowerMessage.matches(".*execution.*path.*") ||
            lowerMessage.matches(".*call.*sequence.*")) {
            return "EXPLAIN_CODE_FLOW";
        }
        
        // Keywords for architectural advice
        if (lowerMessage.contains("design pattern") || lowerMessage.contains("architecture") || 
            lowerMessage.contains("best practice") || lowerMessage.contains("refactor")) {
            return "ARCHITECTURAL_ADVICE";
        }
        
        return "GENERAL_DISCUSSION";
    }

    /**
     * Extract class and method names from user message
     * 
     * @param message The user's input message
     * @return Array with [className, methodName], null values if not found
     */
    public String[] extractClassAndMethod(String message) {
        String[] result = new String[2]; // [className, methodName]
        
        // Pattern to match ClassName.methodName or ClassName::methodName
        Pattern classMethodPattern1 = Pattern.compile("([A-Z][a-zA-Z0-9_]*)[.:]([a-zA-Z][a-zA-Z0-9_]*)");
        Matcher matcher = classMethodPattern1.matcher(message);
        
        if (matcher.find()) {
            result[0] = matcher.group(1);
            result[1] = matcher.group(2);
            return result;
        }
        
        // Pattern to match "methodName in ClassName" or "methodName from ClassName"
        Pattern classMethodPattern2 = Pattern.compile("([a-zA-Z][a-zA-Z0-9_]*)\\s+(?:in|from|of)\\s+([A-Z][a-zA-Z0-9_]*)");
        matcher = classMethodPattern2.matcher(message);
        
        if (matcher.find()) {
            result[1] = matcher.group(1);
            result[0] = matcher.group(2);
            return result;
        }
        
        // Pattern to match "methodName method in ClassName"
        Pattern classMethodPattern3 = Pattern.compile("([a-zA-Z][a-zA-Z0-9_]*)\\s+method\\s+(?:in|from|of)\\s+([A-Z][a-zA-Z0-9_]*)");
        matcher = classMethodPattern3.matcher(message);
        
        if (matcher.find()) {
            result[1] = matcher.group(1);
            result[0] = matcher.group(2);
            return result;
        }
        
        // Pattern to match just class name
        Pattern classPattern = Pattern.compile("\\b([A-Z][a-zA-Z0-9_]*(?:Service|Controller|Repository|Component|Manager|Handler))\\b");
        matcher = classPattern.matcher(message);
        
        if (matcher.find()) {
            result[0] = matcher.group(1);
            return result;
        }
        
        return result;
    }

    /**
     * Check if the message contains a class name pattern
     * 
     * @param message The user's input message
     * @return true if a class name pattern is found
     */
    public boolean containsClassName(String message) {
        Pattern classPattern = Pattern.compile("\\b([A-Z][a-zA-Z0-9_]*(?:Service|Controller|Repository|Component|Manager|Handler))\\b");
        return classPattern.matcher(message).find();
    }

    /**
     * Check if message indicates a request for general flow understanding
     * 
     * @param message The user's input message
     * @return true if it's a general flow request without specific class/method
     */
    public boolean isGeneralFlowRequest(String message) {
        String lowerMessage = message.toLowerCase();
        
        return lowerMessage.contains("understand") && lowerMessage.contains("flow") && 
               !lowerMessage.contains(".") && !containsClassName(message);
    }

    /**
     * Determine if the message is asking for code flow explanation
     * 
     * @param userMessage The user's input message
     * @return true if the intent is to explain code flow
     */
    public boolean isCodeFlowExplanationRequest(String userMessage) {
        return "EXPLAIN_CODE_FLOW".equals(analyzeUserIntent(userMessage));
    }

    /**
     * Determine if the message is asking for JIRA ticket creation
     * 
     * @param userMessage The user's input message
     * @return true if the intent is to create a JIRA ticket
     */
    public boolean isJiraTicketRequest(String userMessage) {
        return "CREATE_JIRA_TICKET".equals(analyzeUserIntent(userMessage));
    }

    /**
     * Determine if the message is asking for architectural advice
     * 
     * @param userMessage The user's input message
     * @return true if the intent is to get architectural advice
     */
    public boolean isArchitecturalAdviceRequest(String userMessage) {
        return "ARCHITECTURAL_ADVICE".equals(analyzeUserIntent(userMessage));
    }
}