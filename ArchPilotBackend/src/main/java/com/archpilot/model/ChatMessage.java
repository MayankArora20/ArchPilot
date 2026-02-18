package com.archpilot.model;

import java.time.LocalDateTime;

public class ChatMessage {
    private String message;
    private String response;
    private LocalDateTime timestamp;
    private String sender; // "user" or "assistant"
    
    public ChatMessage() {}
    
    public ChatMessage(String message, String sender) {
        this.message = message;
        this.sender = sender;
        this.timestamp = LocalDateTime.now();
    }
    
    public ChatMessage(String message, String response, String sender) {
        this.message = message;
        this.response = response;
        this.sender = sender;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
}