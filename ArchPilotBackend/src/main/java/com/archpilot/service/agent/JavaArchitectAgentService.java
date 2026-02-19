package com.archpilot.service.agent;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Java Architect Agent Service
 * 
 * Main Context:
 * - Acts as a senior Java architect with deep understanding of enterprise patterns
 * - Analyzes project UML diagrams and JSON metadata to understand system architecture
 * - Creates detailed JIRA tickets for new requirements with technical specifications
 * - Explains existing code flows and architectural decisions
 * - Suggests design patterns, best practices, and architectural improvements
 * - Coordinates with Java SME agent for detailed code analysis when needed
 * 
 * Token Guardrail Protection:
 * - This agent is automatically protected by the TokenGuardrailInterceptor
 * - All requests through /api/chat/ endpoints are rate-limited
 * - Respects the dual bucket strategy (RPM + TPM) for Gemini API calls
 * - Token consumption is tracked and managed automatically
 * 
 * JIRA Ticket Storage:
 * - Tickets are saved to: Jira/ProjectName/JiraId.json
 * - Directory structure is created automatically
 * - JSON format for easy integration with external systems
 */
@Service
public class JavaArchitectAgentService {

    private static final Logger logger = LoggerFactory.getLogger(JavaArchitectAgentService.class);
    private final ObjectMapper objectMapper;

    public JavaArchitectAgentService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Autowired
    private GeminiAgentService geminiAgentService;

    // TODO: Will be implemented later
    // @Autowired
    // private JavaSMEAgentService javaSMEAgentService;

    /**
     * Process user message and determine the appropriate response type
     * 
     * @param session Current chat session with project context
     * @param userMessage User's message/requirement
     * @return Architect's response as a string
     */
    public String processArchitectRequest(ChatSession session, String userMessage) {
        logger.info("Processing architect request for project: {}", session.getProjectName());
        
        try {
            // Analyze the user message to determine intent
            String intent = analyzeUserIntent(userMessage);
            
            switch (intent) {
                case "CREATE_JIRA_TICKET":
                    return handleJiraTicketCreation(session, userMessage);
                    
                case "EXPLAIN_CODE_FLOW":
                    return handleCodeFlowExplanation(session, userMessage);
                    
                case "ARCHITECTURAL_ADVICE":
                    return handleArchitecturalAdvice(session, userMessage);
                    
                default:
                    return handleGeneralArchitecturalDiscussion(session, userMessage);
            }
            
        } catch (Exception e) {
            logger.error("Error processing architect request: {}", e.getMessage(), e);
            return "I apologize, but I encountered an error while processing your request. Please try rephrasing your question or provide more specific details.";
        }
    }

    /**
     * Analyze user message to determine intent
     */
    private String analyzeUserIntent(String userMessage) {
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
        
        // Keywords for code flow explanation
        if (lowerMessage.contains("explain") && (lowerMessage.contains("flow") || lowerMessage.contains("method") || 
            lowerMessage.contains("function") || lowerMessage.contains("class"))) {
            return "EXPLAIN_CODE_FLOW";
        }
        
        if (lowerMessage.contains("how does") || lowerMessage.contains("what happens when") || 
            lowerMessage.contains("trace") || lowerMessage.contains("analyze")) {
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
     * Handle JIRA ticket creation requests
     */
    private String handleJiraTicketCreation(ChatSession session, String userMessage) {
        logger.info("Creating JIRA ticket for requirement: {}", userMessage);
        
        try {
            JiraTicketResponse ticket = createJiraTicket(session, userMessage);
            String response = formatJiraTicketResponse(ticket, session.getProjectName());
            logger.info("Successfully created JIRA ticket: {}", ticket.getTicketId());
            return response;
            
        } catch (Exception e) {
            logger.error("Error creating JIRA ticket: {}", e.getMessage(), e);
            return "I encountered an error while creating the JIRA ticket. However, I can still provide architectural guidance for your requirement: '" + userMessage + "'. Please let me know if you'd like me to analyze the architectural implications or suggest design patterns for this feature.";
        }
    }

    /**
     * Handle code flow explanation requests
     */
    private String handleCodeFlowExplanation(ChatSession session, String userMessage) {
        logger.info("Explaining code flow for: {}", userMessage);
        
        try {
            // Check if specific class/method is mentioned
            String[] classAndMethod = extractClassAndMethod(userMessage);
            
            if (classAndMethod[0] != null) {
                // Specific class/method mentioned
                return explainSpecificCodeFlow(session, classAndMethod[0], classAndMethod[1], userMessage);
            } else {
                // General description - need to analyze and potentially call Java SME
                return explainGeneralCodeFlow(session, userMessage);
            }
            
        } catch (Exception e) {
            logger.error("Error explaining code flow: {}", e.getMessage(), e);
            return "I encountered an error while analyzing the code flow. Please provide more specific details about the class or method you'd like me to explain.";
        }
    }

    /**
     * Handle architectural advice requests
     */
    private String handleArchitecturalAdvice(ChatSession session, String userMessage) {
        logger.info("Providing architectural advice for: {}", userMessage);
        
        String prompt = buildArchitecturalAdvicePrompt(session, userMessage);
        return geminiAgentService.askQuestion(prompt);
    }

    /**
     * Handle general architectural discussions
     */
    private String handleGeneralArchitecturalDiscussion(ChatSession session, String userMessage) {
        logger.info("General architectural discussion: {}", userMessage);
        
        String prompt = buildGeneralDiscussionPrompt(session, userMessage);
        return geminiAgentService.askQuestion(prompt);
    }

    /**
     * Create a JIRA ticket based on user requirements
     */
    private JiraTicketResponse createJiraTicket(ChatSession session, String userMessage) {
        String prompt = buildJiraTicketPrompt(session, userMessage);
        logger.debug("Sending JIRA ticket prompt to Gemini: {}", prompt.substring(0, Math.min(200, prompt.length())));
        
        String geminiResponse = geminiAgentService.askQuestion(prompt);
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
     * Explain specific code flow when class/method is mentioned
     */
    private String explainSpecificCodeFlow(ChatSession session, String className, String methodName, String userMessage) {
        String prompt = String.format("""
            You are a Senior Java Architect analyzing a specific code flow in a software project.

            Project: %s
            
            UML Diagram:
            ```plantuml
            %s
            ```
            
            Project Metadata:
            %s
            
            User wants to understand: %s
            Target Class: %s
            Target Method: %s
            
            Please explain the code flow in a clear, structured manner:
            
            1. **Purpose**: What does this class/method do?
            2. **Input Parameters**: What inputs does it expect?
            3. **Processing Steps**: Step-by-step flow of execution
            4. **Dependencies**: What other classes/methods does it interact with?
            5. **Output/Return**: What does it return or produce?
            6. **Design Patterns**: Any design patterns used in this flow
            7. **Potential Issues**: Any potential problems or areas for improvement
            
            Focus on architectural aspects and provide insights that would be valuable for a developer working on this code.
            """,
            session.getProjectName(),
            session.getUmlContent(),
            formatProjectMetadata(session.getJsonData()),
            userMessage,
            className,
            methodName != null ? methodName : "Not specified"
        );
        
        return geminiAgentService.askQuestion(prompt);
    }

    /**
     * Explain general code flow when no specific class/method is mentioned
     */
    private String explainGeneralCodeFlow(ChatSession session, String userMessage) {
        // TODO: This is where we would call the Java SME agent in the future
        // For now, provide a general architectural analysis
        
        String prompt = String.format("""
            You are a Senior Java Architect analyzing a general code flow or feature in a software project.

            Project: %s
            
            UML Diagram:
            ```plantuml
            %s
            ```
            
            Project Metadata:
            %s
            
            User wants to understand: "%s"
            
            Since no specific class or method was mentioned, please:
            
            1. **Identify Relevant Components**: Based on the description, which classes/packages are likely involved?
            2. **High-Level Flow**: Describe the general flow at an architectural level
            3. **Key Interactions**: How do the main components interact?
            4. **Data Flow**: How does data move through the system for this scenario?
            5. **Design Patterns**: What architectural patterns are likely used?
            6. **Entry Points**: Where would this flow typically start?
            7. **Recommendations**: Suggest specific classes/methods to examine for detailed understanding
            
            Note: For detailed code-level analysis, I can coordinate with a Java SME agent if you provide specific class/method names.
            """,
            session.getProjectName(),
            session.getUmlContent(),
            formatProjectMetadata(session.getJsonData()),
            userMessage
        );
        
        return geminiAgentService.askQuestion(prompt);
    }

    /**
     * Build prompt for architectural advice
     */
    private String buildArchitecturalAdvicePrompt(ChatSession session, String userMessage) {
        return String.format("""
            You are a Senior Java Architect providing architectural guidance for a software project.

            Project: %s
            
            Current Architecture (UML):
            ```plantuml
            %s
            ```
            
            Project Details:
            %s
            
            Question/Request: "%s"
            
            Please provide architectural guidance considering:
            - Current system design and patterns
            - Best practices for enterprise Java applications
            - Scalability and maintainability concerns
            - Design patterns that would be appropriate
            - Potential refactoring opportunities
            - Performance implications
            - Testing strategies
            
            Provide specific, actionable recommendations with reasoning.
            """,
            session.getProjectName(),
            session.getUmlContent(),
            formatProjectMetadata(session.getJsonData()),
            userMessage
        );
    }

    /**
     * Build prompt for general architectural discussion
     */
    private String buildGeneralDiscussionPrompt(ChatSession session, String userMessage) {
        return String.format("""
            You are a Senior Java Architect having a discussion about a software project.

            Project: %s
            
            Current Architecture:
            ```plantuml
            %s
            ```
            
            Message: "%s"
            
            Please respond as an experienced Java architect, providing insights about:
            - The current system architecture
            - Best practices and recommendations
            - Potential improvements or considerations
            - Relevant design patterns or architectural concepts
            
            Keep the discussion technical but accessible, focusing on architectural aspects.
            """,
            session.getProjectName(),
            session.getUmlContent(),
            userMessage
        );
    }

    /**
     * Format JIRA ticket response as JSON string for frontend
     */
    private String formatJiraTicketResponse(JiraTicketResponse ticket, String projectName) {
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

    // Utility methods
    private String[] extractClassAndMethod(String message) {
        String[] result = new String[2]; // [className, methodName]
        
        // Pattern to match ClassName.methodName or ClassName::methodName
        Pattern classMethodPattern = Pattern.compile("([A-Z][a-zA-Z0-9_]*)[.:]([a-zA-Z][a-zA-Z0-9_]*)");
        Matcher matcher = classMethodPattern.matcher(message);
        
        if (matcher.find()) {
            result[0] = matcher.group(1);
            result[1] = matcher.group(2);
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

    /**
     * Load existing JIRA tickets for a project (utility method for future use)
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
     * Check if a JIRA ticket exists (utility method for future use)
     */
    public boolean jiraTicketExists(String projectName, String ticketId) {
        Path ticketPath = Paths.get("Jira", projectName, ticketId + ".json");
        return Files.exists(ticketPath);
    }
}