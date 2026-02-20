package com.archpilot.service.agent;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.archpilot.dto.JiraTicketResponse;
import com.archpilot.model.ChatSession;
import com.archpilot.service.IntentAnalyzerService;
import com.archpilot.service.JiraTicketService;
import com.archpilot.service.diagram.DiagramFileManager;
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
 * - Tickets are managed by JiraTicketService
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
    private GeminiChatAgentService geminiChatAgentService;
    
    @Autowired
    private DiagramFileManager diagramFileManager;

    @Autowired
    private IntentAnalyzerService intentAnalyzerService;

    @Autowired
    private JiraTicketService jiraTicketService;

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
            String intent = intentAnalyzerService.analyzeUserIntent(userMessage);
            
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
     * Handle JIRA ticket creation requests
     */
    private String handleJiraTicketCreation(ChatSession session, String userMessage) {
        logger.info("Creating JIRA ticket for requirement: {}", userMessage);
        
        try {
            JiraTicketResponse ticket = jiraTicketService.createJiraTicket(session, userMessage);
            String response = jiraTicketService.formatJiraTicketResponse(ticket, session.getProjectName());
            logger.info("Successfully created JIRA ticket: {}", ticket.getTicketId());
            return response;
            
        } catch (Exception e) {
            logger.error("Error creating JIRA ticket: {}", e.getMessage(), e);
            return "I encountered an error while creating the JIRA ticket. However, I can still provide architectural guidance for your requirement: '" + userMessage + "'. Please let me know if you'd like me to analyze the architectural implications or suggest design patterns for this feature.";
        }
    }
    /**
     * Handle code flow explanation requests with enhanced visual diagram generation
     */
    private String handleCodeFlowExplanation(ChatSession session, String userMessage) {
        logger.info("Explaining code flow for: {}", userMessage);
        
        try {
            // Check if specific class/method is mentioned
            String[] classAndMethod = intentAnalyzerService.extractClassAndMethod(userMessage);
            
            if (classAndMethod[0] != null) {
                // Specific class/method mentioned - generate detailed analysis with diagrams
                return explainSpecificCodeFlowWithDiagrams(session, classAndMethod[0], classAndMethod[1], userMessage);
            } else {
                // General description - ask for more details or provide general analysis
                return handleGeneralFlowRequest(session, userMessage);
            }
            
        } catch (Exception e) {
            logger.error("Error explaining code flow: {}", e.getMessage(), e);
            return "I encountered an error while analyzing the code flow. Please provide more specific details about the class or method you'd like me to explain.";
        }
    }
    
    /**
     * Handle general flow requests when no specific class/method is mentioned
     */
    private String handleGeneralFlowRequest(ChatSession session, String userMessage) {
        // Check if user is asking for help understanding flows in general
        if (intentAnalyzerService.isGeneralFlowRequest(userMessage)) {
            
            return "I can help you understand the code flow! To provide a detailed analysis with visual diagrams, please specify:\n\n" +
                   "- **Class name**: Which class contains the method you want to analyze?\n" +
                   "- **Method name**: Which specific method should I analyze?\n\n" +
                   "**Examples:**\n" +
                   "- \"Analyze the flow of processPayment method in PaymentService class\"\n" +
                   "- \"Explain how UserService.createUser works\"\n" +
                   "- \"Show me the execution path of OrderController.processOrder\"\n\n" +
                   "I'll then provide:\n" +
                   "✓ Step-by-step execution flow\n" +
                   "✓ Visual sequence diagrams\n" +
                   "✓ Flow charts\n" +
                   "✓ Design patterns analysis\n" +
                   "✓ Improvement suggestions";
        }
        
        // Otherwise, provide general architectural analysis
        return explainGeneralCodeFlow(session, userMessage);
    }
    /**
     * Explain specific code flow with enhanced visual diagrams
     */
    private String explainSpecificCodeFlowWithDiagrams(ChatSession session, String className, String methodName, String userMessage) {
        logger.info("Generating detailed flow analysis with diagrams for {}.{}", className, methodName);
        
        try {
            // Generate comprehensive flow analysis
            String flowAnalysis = generateDetailedFlowAnalysis(session, className, methodName, userMessage);
            
            // Generate visual diagrams
            String diagramLinks = diagramFileManager.generateFlowDiagrams(session.getProjectName(), className, methodName, flowAnalysis);
            
            // Combine analysis with diagram links
            StringBuilder response = new StringBuilder();
            response.append("## Code Flow Analysis: ").append(className);
            if (methodName != null) {
                response.append(".").append(methodName);
            }
            response.append("\n\n");
            
            response.append(flowAnalysis);
            response.append("\n\n");
            response.append(diagramLinks);
            
            return response.toString();
            
        } catch (Exception e) {
            logger.error("Error generating flow analysis with diagrams: {}", e.getMessage(), e);
            // Fallback to basic analysis without diagrams
            return explainSpecificCodeFlow(session, className, methodName, userMessage);
        }
    }
    /**
     * Generate detailed flow analysis directly from class and method names without calling APIs
     * This creates a comprehensive description that includes proper class and method names for diagram generation
     */
    private String generateDetailedFlowAnalysis(ChatSession session, String className, String methodName, String userMessage) {
        String prompt = String.format("""
            You are a Senior Java Architect creating a detailed code flow analysis based on class and method names in a software project.

            Project: %s
            
            UML Diagram Context:
            ```plantuml
            %s
            ```
            
            Project Metadata:
            %s
            
            User Request: %s
            Target Class: %s
            Target Method: %s
            
            CRITICAL REQUIREMENTS FOR ACCURATE DIAGRAM GENERATION:
            1. Use SPECIFIC method names (e.g., findById(), validateOrder(), save()) NOT generic process() calls
            2. List classes in the EXACT ORDER they are called in the execution flow
            3. Use SPECIFIC exception names (e.g., ValidationException, NotFoundException) NOT generic "error" or "alternative path"
            4. Include actual method signatures with parameters where relevant
            
            Please provide a comprehensive code flow analysis in the following structured format:
            
            **Flow Description:**
            [Based on the class name '%s' and method name '%s', describe what this method likely does and its role in the system. Consider common enterprise patterns.]
            
            **Complexity:** [Low/Medium/High - estimate based on method name and typical enterprise patterns]
            
            **Involved Classes:** [List classes in EXACT EXECUTION ORDER - the order they are called during runtime. Start with %s, then list each subsequent class as it's invoked. Example: OrderService, OrderValidationService, OrderRepository, InventoryService, NotificationService]
            
            **Design Patterns:** [Identify likely design patterns based on class name and method - e.g., Repository, Service Layer, DTO, Factory, etc.]
            
            **Execution Steps:** (CRITICAL - Use SPECIFIC method names, not generic process() calls)
            1. %s.%s receives input parameters (e.g., orderId, userId)
            2. Call ValidationService.validateInput(parameters) to verify input data
            3. Call OrderRepository.findById(orderId) to retrieve order entity
            4. Call BusinessRuleService.checkBusinessRules(order) to validate business logic
            5. Call DataService.save(processedData) to persist changes
            6. Call NotificationService.sendNotification(result) to notify stakeholders
            7. Return processed result or throw specific exception (ValidationException, NotFoundException, etc.)
            
            **Sequence Interactions:** (CRITICAL - This defines the EXACT ORDER and METHOD NAMES for sequence diagrams)
            1. Client -> %s.%s(parameters)
            2. %s -> ValidationService.validateInput(parameters)
            3. %s -> Repository.findById(id)
            4. %s -> BusinessService.processBusinessLogic(data)
            5. %s -> DataService.save(result)
            6. %s -> NotificationService.notify(event)
            [Add more interactions in the exact order they occur, using specific method names]
            
            **Flow Logic:** (CRITICAL - Use SPECIFIC exception names for error handling)
            - START: Method %s.%s is called with parameters
            - DECISION: Validate input parameters - if invalid, throw ValidationException with specific error message
            - PROCESS: Retrieve data from repository using findById() or similar specific method
            - DECISION: Check if entity exists - if not found, throw NotFoundException
            - PROCESS: Apply business rules and transformations
            - DECISION: Validate business rules - if violated, throw BusinessRuleException with rule details
            - PROCESS: Persist changes to database
            - DECISION: Check save success - if failed, throw DataAccessException
            - PROCESS: Send notifications or trigger events
            - END: Return success response with processed data
            
            **Exception Handling:** (CRITICAL - List SPECIFIC exception types)
            - ValidationException: Thrown when input validation fails (e.g., null parameters, invalid format)
            - NotFoundException: Thrown when requested entity is not found in database
            - BusinessRuleException: Thrown when business rules are violated
            - DataAccessException: Thrown when database operations fail
            - [Add other specific exceptions relevant to this operation]
            
            **Data Flow:**
            - Input: [Describe realistic input parameters based on method name with types]
            - Processing: [Describe data transformation and business logic with specific operations]
            - Output: [Describe return type and success/error responses with specific types]
            
            **Dependencies:**
            - [List 3-4 realistic external dependencies like Database, External APIs, Validation services, etc.]
            
            **Potential Improvements:**
            - Add comprehensive error handling and logging
            - Implement caching for better performance
            - Add input validation and sanitization
            - Consider async processing for long-running operations
            
            REMEMBER: 
            - Use SPECIFIC method names (findById, save, validate) NOT generic process()
            - List classes in EXECUTION ORDER (the order they're called at runtime)
            - Use SPECIFIC exception names (ValidationException, NotFoundException) NOT generic "error"
            """,
            session.getProjectName(),
            session.getUmlContent(),
            formatProjectMetadata(session.getJsonData()),
            userMessage,
            className,
            methodName != null ? methodName : "execute",
            className,
            methodName != null ? methodName : "execute",
            className,
            className,
            methodName != null ? methodName : "execute",
            className,
            className,
            className,
            className,
            className,
            className,
            className,
            className,
            methodName != null ? methodName : "execute"
        );
        
        return geminiChatAgentService.askQuestion(prompt);
    }
    /**
     * Handle architectural advice requests
     */
    private String handleArchitecturalAdvice(ChatSession session, String userMessage) {
        logger.info("Providing architectural advice for: {}", userMessage);
        
        String prompt = buildArchitecturalAdvicePrompt(session, userMessage);
        return geminiChatAgentService.askQuestion(prompt);
    }

    /**
     * Handle general architectural discussions
     */
    private String handleGeneralArchitecturalDiscussion(ChatSession session, String userMessage) {
        logger.info("General architectural discussion: {}", userMessage);
        
        String prompt = buildGeneralDiscussionPrompt(session, userMessage);
        return geminiChatAgentService.askQuestion(prompt);
    }

    /**
     * Explain specific code flow when class/method is mentioned - generate description directly
     */
    private String explainSpecificCodeFlow(ChatSession session, String className, String methodName, String userMessage) {
        String prompt = String.format("""
            You are a Senior Java Architect creating a detailed code flow explanation based on class and method names.

            Project: %s
            
            UML Diagram Context:
            ```plantuml
            %s
            ```
            
            Project Metadata:
            %s
            
            User wants to understand: %s
            Target Class: %s
            Target Method: %s
            
            IMPORTANT: Generate a realistic explanation based on the class and method names provided. 
            Use common Java enterprise patterns and best practices to create a comprehensive description.
            
            Please explain the code flow in a clear, structured manner:
            
            1. **Purpose**: Based on the class name '%s' and method name '%s', describe what this method likely does in an enterprise Java application.
            
            2. **Input Parameters**: Describe realistic input parameters this method would expect based on its name and common patterns.
            
            3. **Processing Steps**: Provide a step-by-step flow of execution with realistic class and method interactions:
               - Input validation using ValidationService or similar
               - Business logic processing
               - Data access through Repository or DAO patterns
               - External service calls if applicable
               - Response formatting and return
            
            4. **Dependencies**: List realistic dependencies this method would have:
               - Database repositories
               - External services
               - Validation components
               - Logging and monitoring
            
            5. **Output/Return**: Describe what this method would typically return and possible exception scenarios.
            
            6. **Design Patterns**: Identify likely design patterns used (Repository, Service Layer, DTO, etc.).
            
            7. **Potential Issues**: Common issues that might occur in this type of method and how to handle them.
            
            Focus on architectural aspects and provide insights that would be valuable for a developer working on this code.
            Use realistic Java class names and method calls throughout the explanation.
            """,
            session.getProjectName(),
            session.getUmlContent(),
            formatProjectMetadata(session.getJsonData()),
            userMessage,
            className,
            methodName != null ? methodName : "execute",
            className,
            methodName != null ? methodName : "execute"
        );
        
        return geminiChatAgentService.askQuestion(prompt);
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
        
        return geminiChatAgentService.askQuestion(prompt);
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