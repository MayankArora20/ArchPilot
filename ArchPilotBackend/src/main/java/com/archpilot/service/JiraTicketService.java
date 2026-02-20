package com.archpilot.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.archpilot.dto.JiraTicketResponse;
import com.archpilot.model.ChatSession;
import com.archpilot.service.agent.GeminiChatAgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Service for managing JIRA ticket creation and operations
 * 
 * Responsibilities:
 * - Create JIRA tickets based on user requirements
 * - Parse and validate ticket responses from AI agents
 * - Save tickets to file system in structured format
 * - Load existing tickets for project management
 * - Format ticket responses for frontend consumption
 * 
 * File Storage:
 * - Tickets are saved to: Jira/ProjectName/JiraId.json
 * - Directory structure is created automatically
 * - JSON format for easy integration with external systems
 */
@Service
public class JiraTicketService {

    private static final Logger logger = LoggerFactory.getLogger(JiraTicketService.class);
    private final ObjectMapper objectMapper;

    @Autowired
    private GeminiChatAgentService geminiChatAgentService;

    public JiraTicketService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Create a JIRA ticket based on user requirements
     */
    public JiraTicketResponse createJiraTicket(ChatSession session, String userMessage) {
        String prompt = buildJiraTicketPrompt(session, userMessage);
        logger.debug("Sending JIRA ticket prompt to Gemini: {}", prompt.substring(0, Math.min(200, prompt.length())));
        
        String geminiResponse = geminiChatAgentService.askQuestion(prompt);
        logger.info("Received Gemini response for JIRA ticket (length: {}): {}", 
                   geminiResponse.length(), 
                   geminiResponse.substring(0, Math.min(100, geminiResponse.length())));
        
        JiraTicketResponse ticket = parseJiraTicketFromResponse(geminiResponse);
        
        // Save JIRA ticket to file system
        try {
            saveJiraTicketToFile(ticket, session.getProjectName());
            logger.info("JIRA ticket {} saved successfully for project {}", ticket.getTicketId(), session.getProjectName());
        } catch (Exception e) {
            logger.error("Error saving JIRA ticket to file: {}", e.getMessage(), e);
            // Continue even if file saving fails
        }
        
        return ticket;
    }

    /**
     * Format JIRA ticket response as JSON string for frontend
     */
    public String formatJiraTicketResponse(JiraTicketResponse ticket, String projectName) {
        try {
            // Return the JSON content as a string for the frontend
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ticket);
        } catch (Exception e) {
            logger.error("Error formatting JIRA ticket response as JSON: {}", e.getMessage(), e);
            // Fallback to a simple JSON structure
            return String.format("""
                {
                    "ticketId": "%s",
                    "heading": "%s",
                    "description": "%s",
                    "storyPoints": %d,
                    "priority": "%s",
                    "ticketType": "%s",
                    "fileLocation": "Jira/%s/%s.json"
                }
                """, 
                ticket.getTicketId() != null ? ticket.getTicketId() : "ARCH-0000",
                ticket.getHeading() != null ? ticket.getHeading().replace("\"", "\\\"") : "New Ticket",
                ticket.getDescription() != null ? ticket.getDescription().replace("\"", "\\\"").replace("\n", "\\n") : "No description",
                ticket.getStoryPoints(),
                ticket.getPriority() != null ? ticket.getPriority() : "Medium",
                ticket.getTicketType() != null ? ticket.getTicketType() : "Story",
                projectName,
                ticket.getTicketId() != null ? ticket.getTicketId() : "ARCH-0000"
            );
        }
    }

    /**
     * Load existing JIRA tickets for a project
     */
    public List<JiraTicketResponse> loadProjectJiraTickets(String projectName) {
        List<JiraTicketResponse> tickets = new ArrayList<>();
        
        try {
            Path projectPath = Paths.get("Jira", projectName);
            
            if (!Files.exists(projectPath)) {
                logger.info("No JIRA tickets directory found for project: {}", projectName);
                return tickets;
            }
            
            Files.list(projectPath)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(ticketFile -> {
                    try {
                        JiraTicketResponse ticket = objectMapper.readValue(ticketFile.toFile(), JiraTicketResponse.class);
                        tickets.add(ticket);
                        logger.debug("Loaded JIRA ticket: {}", ticket.getTicketId());
                    } catch (Exception e) {
                        logger.warn("Error loading JIRA ticket from {}: {}", ticketFile.getFileName(), e.getMessage());
                    }
                });
                
            logger.info("Loaded {} JIRA tickets for project: {}", tickets.size(), projectName);
            
        } catch (Exception e) {
            logger.error("Error loading JIRA tickets for project {}: {}", projectName, e.getMessage());
        }
        
        return tickets;
    }

    /**
     * Build prompt for JIRA ticket creation
     */
    private String buildJiraTicketPrompt(ChatSession session, String userMessage) {
        return String.format("""
            You are a Senior Java Architect analyzing a software project. Based on the project's UML diagram and the user's requirement, create a detailed JIRA ticket.

            Project: %s
            
            UML Diagram:
            ```plantuml
            %s
            ```
            
            Project Metadata:
            %s
            
            User Requirement:
            "%s"
            
            IMPORTANT: You MUST respond with ONLY a valid JSON object in the exact format below. Do not include any explanatory text before or after the JSON.
            
            {
                "ticketId": "ARCH-[random 4 digit number]",
                "heading": "Clear, concise title for the ticket",
                "description": "Detailed description of what needs to be implemented",
                "storyPoints": 5,
                "classesToConsider": ["List of existing classes that might need modification"],
                "methodsToConsider": ["List of methods that might need changes or new methods to create"],
                "designPatterns": ["Suggested design patterns to use"],
                "unitTestCases": ["List of unit test scenarios to consider"],
                "successCriteria": "Clear criteria for when this ticket is considered complete",
                "priority": "High",
                "ticketType": "Story"
            }
            
            Consider:
            - Existing architecture and how the new requirement fits
            - Impact on existing classes and methods
            - Appropriate design patterns for the solution
            - Comprehensive testing strategy
            - Clear acceptance criteria
            
            Respond with ONLY the JSON object, no additional text.
            """, 
            session.getProjectName(),
            session.getUmlContent(),
            formatProjectMetadata(session.getJsonData()),
            userMessage
        );
    }

    /**
     * Parse JIRA ticket from Gemini response
     */
    private JiraTicketResponse parseJiraTicketFromResponse(String response) {
        try {
            // Extract JSON from response
            String jsonPart = extractJsonFromResponse(response);
            
            // Check if we actually got JSON
            if (jsonPart == null || jsonPart.trim().isEmpty() || !jsonPart.trim().startsWith("{")) {
                logger.warn("No valid JSON found in Gemini response, creating fallback ticket");
                return createFallbackTicketFromText(response);
            }
            
            // Parse the JSON manually (simplified approach)
            JiraTicketResponse ticket = new JiraTicketResponse();
            
            ticket.setTicketId(extractJsonValue(jsonPart, "ticketId"));
            ticket.setHeading(extractJsonValue(jsonPart, "heading"));
            ticket.setDescription(extractJsonValue(jsonPart, "description"));
            
            String storyPointsStr = extractJsonValue(jsonPart, "storyPoints");
            if (storyPointsStr != null) {
                try {
                    ticket.setStoryPoints(Integer.parseInt(storyPointsStr));
                } catch (NumberFormatException e) {
                    ticket.setStoryPoints(5); // Default
                }
            }
            
            ticket.setClassesToConsider(extractJsonArray(jsonPart, "classesToConsider"));
            ticket.setMethodsToConsider(extractJsonArray(jsonPart, "methodsToConsider"));
            ticket.setDesignPatterns(extractJsonArray(jsonPart, "designPatterns"));
            ticket.setUnitTestCases(extractJsonArray(jsonPart, "unitTestCases"));
            ticket.setSuccessCriteria(extractJsonValue(jsonPart, "successCriteria"));
            ticket.setPriority(extractJsonValue(jsonPart, "priority"));
            ticket.setTicketType(extractJsonValue(jsonPart, "ticketType"));
            
            // Validate that we got essential fields
            if (ticket.getTicketId() == null || ticket.getHeading() == null) {
                logger.warn("Essential fields missing from parsed JSON, creating fallback ticket");
                return createFallbackTicketFromText(response);
            }
            
            return ticket;
            
        } catch (Exception e) {
            logger.error("Error parsing JIRA ticket response: {}", e.getMessage());
            return createFallbackTicketFromText(response);
        }
    }
    
    /**
     * Create a fallback ticket when JSON parsing fails
     */
    private JiraTicketResponse createFallbackTicketFromText(String response) {
        JiraTicketResponse fallback = new JiraTicketResponse();
        fallback.setTicketId("ARCH-" + String.format("%04d", (int)(Math.random() * 10000)));
        
        // Try to extract a meaningful title from the response
        String title = extractTitleFromText(response);
        fallback.setHeading(title != null ? title : "New Feature Implementation");
        
        // Use the full response as description
        fallback.setDescription("Based on the architectural analysis:\n\n" + response);
        fallback.setStoryPoints(5);
        fallback.setPriority("Medium");
        fallback.setTicketType("Story");
        
        // Add some default suggestions
        List<String> defaultClasses = new ArrayList<>();
        defaultClasses.add("Please analyze the existing architecture");
        fallback.setClassesToConsider(defaultClasses);
        
        List<String> defaultPatterns = new ArrayList<>();
        defaultPatterns.add("Strategy Pattern");
        defaultPatterns.add("Factory Pattern");
        fallback.setDesignPatterns(defaultPatterns);
        
        fallback.setSuccessCriteria("Implementation should follow the architectural recommendations provided in the description.");
        
        return fallback;
    }
    
    /**
     * Extract a title from natural language text
     */
    private String extractTitleFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        // Look for the first sentence or first 100 characters
        String[] sentences = text.split("[.!?]");
        if (sentences.length > 0) {
            String firstSentence = sentences[0].trim();
            if (firstSentence.length() > 100) {
                return firstSentence.substring(0, 97) + "...";
            }
            return firstSentence;
        }
        
        // Fallback to first 100 characters
        if (text.length() > 100) {
            return text.substring(0, 97) + "...";
        }
        
        return text.trim();
    }

    /**
     * Save JIRA ticket to file system
     * Directory structure: Jira/ProjectName/JiraId.json
     */
    private void saveJiraTicketToFile(JiraTicketResponse ticket, String projectName) throws IOException {
        // Create directory structure: Jira/ProjectName/
        Path jiraBasePath = Paths.get("Jira");
        Path projectPath = jiraBasePath.resolve(projectName);
        
        // Create directories if they don't exist
        if (!Files.exists(jiraBasePath)) {
            Files.createDirectories(jiraBasePath);
            logger.info("Created Jira directory");
        }
        
        if (!Files.exists(projectPath)) {
            Files.createDirectories(projectPath);
            logger.info("Created project directory: {}", projectPath);
        }
        
        // Create filename: JiraId.json
        String fileName = ticket.getTicketId() + ".json";
        Path ticketFilePath = projectPath.resolve(fileName);
        
        // Save ticket as JSON
        logger.info("Saving JIRA ticket to: {}", ticketFilePath.toAbsolutePath());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(ticketFilePath.toFile(), ticket);
        logger.info("Successfully saved JIRA ticket: {}", fileName);
    }

    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        
        // Find JSON block in the response
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");
        
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            String jsonCandidate = response.substring(jsonStart, jsonEnd + 1);
            
            // Basic validation - check if it looks like JSON
            if (jsonCandidate.contains("\"ticketId\"") || jsonCandidate.contains("\"heading\"")) {
                return jsonCandidate;
            }
        }
        
        // If no valid JSON structure found, return null
        logger.warn("No valid JSON structure found in response: {}", response.substring(0, Math.min(100, response.length())));
        return null;
    }

    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Try without quotes for numbers
        pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*([^,}]+)");
        matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return null;
    }

    private List<String> extractJsonArray(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            String arrayContent = matcher.group(1);
            String[] items = arrayContent.split(",");
            List<String> result = new ArrayList<>();
            
            for (String item : items) {
                String cleanItem = item.replaceAll("[\"\n\r\t\\s]", "").trim();
                if (!cleanItem.isEmpty()) {
                    result.add(cleanItem);
                }
            }
            
            return result;
        }
        
        return new ArrayList<>();
    }

    private String formatProjectMetadata(Map<String, Object> jsonData) {
        if (jsonData == null) return "No metadata available";
        
        StringBuilder metadata = new StringBuilder();
        metadata.append("Total Classes: ").append(jsonData.getOrDefault("totalClasses", "Unknown")).append("\n");
        metadata.append("Analyzed Classes: ").append(jsonData.getOrDefault("analyzedClasses", "Unknown")).append("\n");
        metadata.append("Repository: ").append(jsonData.getOrDefault("repositoryUrl", "Unknown")).append("\n");
        metadata.append("Branch: ").append(jsonData.getOrDefault("branch", "Unknown"));
        
        return metadata.toString();
    }
}