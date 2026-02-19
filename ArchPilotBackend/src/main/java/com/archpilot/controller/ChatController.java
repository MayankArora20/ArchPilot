package com.archpilot.controller;

import com.archpilot.dto.ChatInitRequest;
import com.archpilot.dto.ChatMessageRequest;
import com.archpilot.dto.ChatResponse;
import com.archpilot.model.ApiResponse;
import com.archpilot.model.ChatSession;
import com.archpilot.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat Management", description = "APIs for managing chat sessions with project context")
@Validated
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private static final String CHAT_SESSION_KEY = "chatSession";
    
    @Autowired
    private ChatService chatService;
    
    @PostMapping("/init")
    @Operation(summary = "Initialize chat session", description = "Initialize a new chat session for a specific project")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Chat session initialized successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<ChatResponse>> initializeChat(
            @Valid @RequestBody ChatInitRequest request, 
            HttpSession httpSession) {
        
        try {
            logger.info("Initializing chat session for project: {}", request.getProjectName());
            
            // Initialize chat session with project data
            ChatSession chatSession = chatService.initializeSession(request.getProjectName());
            
            // Store session in HTTP session
            httpSession.setAttribute(CHAT_SESSION_KEY, chatSession);
            
            // Create response
            ChatResponse response = new ChatResponse(
                httpSession.getId(),
                chatSession.getProjectName(),
                "Chat session initialized successfully for project: " + chatSession.getProjectName(),
                true
            );
            response.setChatHistory(chatSession.getMessages());
            
            return ResponseEntity.ok(ApiResponse.success("Chat session initialized", response));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Project not found: {}", request.getProjectName());
            ChatResponse response = new ChatResponse("Project not found: " + request.getProjectName());
            response.setSessionActive(false);
            return ResponseEntity.ok(ApiResponse.error("Project not found", response));
            
        } catch (Exception e) {
            logger.error("Error initializing chat session: {}", e.getMessage(), e);
            ChatResponse response = new ChatResponse("Failed to initialize chat session");
            response.setSessionActive(false);
            return ResponseEntity.ok(ApiResponse.error("Internal server error", response));
        }
    }
    
    @PostMapping("/message")
    @Operation(summary = "Send chat message", description = "Send a message in the current chat session and receive a structured JSON response")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Message processed successfully with JSON response"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No active session or invalid message")
    })
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            @Valid @RequestBody ChatMessageRequest request, 
            HttpSession httpSession) {
        
        try {
            // Get chat session from HTTP session
            ChatSession chatSession = (ChatSession) httpSession.getAttribute(CHAT_SESSION_KEY);
            
            if (chatSession == null) {
                logger.warn("No active chat session found");
                ChatResponse errorResponse = new ChatResponse("No active chat session. Please initialize a session first.");
                errorResponse.setSessionActive(false);
                return ResponseEntity.badRequest().body(ApiResponse.error("No active session", errorResponse));
            }
            
            logger.info("Processing message for project: {}", chatSession.getProjectName());
            
            // Process the message
            String assistantResponse = chatService.processMessage(chatSession, request.getMessage());
            
            // Update session in HTTP session
            httpSession.setAttribute(CHAT_SESSION_KEY, chatSession);
            
            // Create proper JSON response
            ChatResponse response = new ChatResponse(
                httpSession.getId(),
                chatSession.getProjectName(),
                assistantResponse,
                true
            );
            response.setChatHistory(chatSession.getMessages());
            
            return ResponseEntity.ok(ApiResponse.success("Message processed successfully", response));
            
        } catch (Exception e) {
            logger.error("Error processing chat message: {}", e.getMessage(), e);
            ChatResponse errorResponse = new ChatResponse("Failed to process message");
            errorResponse.setSessionActive(false);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error", errorResponse));
        }
    }
    
    @GetMapping("/session")
    @Operation(summary = "Get current session info", description = "Get information about the current chat session")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session info retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No active session")
    })
    public ResponseEntity<ApiResponse<ChatResponse>> getSessionInfo(HttpSession httpSession) {
        
        ChatSession chatSession = (ChatSession) httpSession.getAttribute(CHAT_SESSION_KEY);
        
        if (chatSession == null) {
            ChatResponse response = new ChatResponse("No active chat session");
            response.setSessionActive(false);
            return ResponseEntity.ok(ApiResponse.error("No active session", response));
        }
        
        ChatResponse response = new ChatResponse(
            httpSession.getId(),
            chatSession.getProjectName(),
            "Session is active",
            true
        );
        response.setChatHistory(chatSession.getMessages());
        
        return ResponseEntity.ok(ApiResponse.success("Session info retrieved", response));
    }
    
    @DeleteMapping("/session")
    @Operation(summary = "End chat session", description = "End the current chat session")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session ended successfully")
    })
    public ResponseEntity<ApiResponse<ChatResponse>> endSession(HttpSession httpSession) {
        
        ChatSession chatSession = (ChatSession) httpSession.getAttribute(CHAT_SESSION_KEY);
        
        if (chatSession != null) {
            logger.info("Ending chat session for project: {}", chatSession.getProjectName());
            httpSession.removeAttribute(CHAT_SESSION_KEY);
        }
        
        // Invalidate the session
        httpSession.invalidate();
        
        ChatResponse response = new ChatResponse("Chat session ended");
        response.setSessionActive(false);
        
        return ResponseEntity.ok(ApiResponse.success("Session ended", response));
    }
    
    @GetMapping("/health")
    @Operation(summary = "Chat service health check", description = "Check if the chat service is operational")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Chat service is operational");
    }
}