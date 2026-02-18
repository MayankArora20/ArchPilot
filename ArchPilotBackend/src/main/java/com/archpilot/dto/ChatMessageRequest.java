package com.archpilot.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatMessageRequest {
    
    @NotBlank(message = "Message is required")
    private String message;
    
    public ChatMessageRequest() {}
    
    public ChatMessageRequest(String message) {
        this.message = message;
    }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}