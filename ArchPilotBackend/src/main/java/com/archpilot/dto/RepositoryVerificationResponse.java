package com.archpilot.dto;

import com.archpilot.model.RepositoryInfo;
import java.time.LocalDateTime;

public class RepositoryVerificationResponse {
    
    private String status; // "Verified" or "Error"
    private String message;
    private String repositoryUrl;
    private RepositoryInfo repositoryInfo;
    private LocalDateTime timestamp;
    
    public RepositoryVerificationResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public RepositoryVerificationResponse(String status, String message) {
        this();
        this.status = status;
        this.message = message;
    }
    
    public static RepositoryVerificationResponse verified(String repositoryUrl, RepositoryInfo info) {
        RepositoryVerificationResponse response = new RepositoryVerificationResponse("Verified", "Repository is accessible and valid");
        response.setRepositoryUrl(repositoryUrl);
        response.setRepositoryInfo(info);
        return response;
    }
    
    public static RepositoryVerificationResponse error(String errorMessage) {
        return new RepositoryVerificationResponse("Error", errorMessage);
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getRepositoryUrl() {
        return repositoryUrl;
    }
    
    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }
    
    public RepositoryInfo getRepositoryInfo() {
        return repositoryInfo;
    }
    
    public void setRepositoryInfo(RepositoryInfo repositoryInfo) {
        this.repositoryInfo = repositoryInfo;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}