package com.archpilot.dto;

import com.archpilot.model.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;

public class ChatResponse {
    private String sessionId;
    private String projectName;
    private String response;
    private LocalDateTime timestamp;
    private List<ChatMessage> chatHistory;
    private boolean sessionActive;
    
    public ChatResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ChatResponse(String response) {
        this();
        this.response = response;
    }
    
    public ChatResponse(String sessionId, String projectName, String response, boolean sessionActive) {
        this();
        this.sessionId = sessionId;
        this.projectName = projectName;
        this.response = response;
        this.sessionActive = sessionActive;
    }
    
    // Getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public List<ChatMessage> getChatHistory() { return chatHistory; }
    public void setChatHistory(List<ChatMessage> chatHistory) { this.chatHistory = chatHistory; }
    
    public boolean isSessionActive() { return sessionActive; }
    public void setSessionActive(boolean sessionActive) { this.sessionActive = sessionActive; }
}