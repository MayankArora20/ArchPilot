package com.archpilot.dto;

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
    
    public static class RepositoryInfo {
        private String name;
        private String fullName;
        private String description;
        private String defaultBranch;
        private boolean isPrivate;
        private String language;
        private String platform; // "GitHub" or "GitLab"
        
        public RepositoryInfo() {}
        
        public RepositoryInfo(String name, String fullName, String description, 
                            String defaultBranch, boolean isPrivate, String language, String platform) {
            this.name = name;
            this.fullName = fullName;
            this.description = description;
            this.defaultBranch = defaultBranch;
            this.isPrivate = isPrivate;
            this.language = language;
            this.platform = platform;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getDefaultBranch() { return defaultBranch; }
        public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
        
        public boolean isPrivate() { return isPrivate; }
        public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }
        
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
    }
}