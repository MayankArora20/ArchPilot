package com.archpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RepositoryVerificationRequest {
    
    @NotBlank(message = "Repository URL is required")
    @Pattern(
        regexp = "^https://github\\.com/[\\w.-]+/[\\w.-]+/?$|^https://gitlab\\.com/[\\w.-]+/[\\w.-]+/?$",
        message = "Invalid repository URL. Only GitHub and GitLab URLs are supported"
    )
    private String repositoryUrl;
    
    private String accessToken; // Optional for private repositories
    
    public RepositoryVerificationRequest() {}
    
    public RepositoryVerificationRequest(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }
    
    public RepositoryVerificationRequest(String repositoryUrl, String accessToken) {
        this.repositoryUrl = repositoryUrl;
        this.accessToken = accessToken;
    }
    
    public String getRepositoryUrl() {
        return repositoryUrl;
    }
    
    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    @Override
    public String toString() {
        return "RepositoryVerificationRequest{" +
                "repositoryUrl='" + repositoryUrl + '\'' +
                ", hasAccessToken=" + (accessToken != null && !accessToken.isEmpty()) +
                '}';
    }
}