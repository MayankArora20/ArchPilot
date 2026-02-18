package com.archpilot.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatInitRequest {
    
    @NotBlank(message = "Project name is required")
    private String projectName;
    
    public ChatInitRequest() {}
    
    public ChatInitRequest(String projectName) {
        this.projectName = projectName;
    }
    
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
}