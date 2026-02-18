package com.archpilot.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatSession {
    private String projectName;
    private String umlContent;
    private Map<String, Object> jsonData;
    private List<ChatMessage> messages;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    
    public ChatSession() {
        this.messages = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }
    
    public ChatSession(String projectName, String umlContent, Map<String, Object> jsonData) {
        this();
        this.projectName = projectName;
        this.umlContent = umlContent;
        this.jsonData = jsonData;
    }
    
    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.lastActivity = LocalDateTime.now();
    }
    
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    
    public String getUmlContent() { return umlContent; }
    public void setUmlContent(String umlContent) { this.umlContent = umlContent; }
    
    public Map<String, Object> getJsonData() { return jsonData; }
    public void setJsonData(Map<String, Object> jsonData) { this.jsonData = jsonData; }
    
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
}