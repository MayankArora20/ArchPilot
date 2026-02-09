package com.archpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RepositoryTreeRequest {
    
    @NotBlank(message = "Repository URL is required")
    @Pattern(
        regexp = "^https://github\\.com/[\\w.-]+/[\\w.-]+/?$|^https://gitlab\\.com/[\\w.-]+/[\\w.-]+/?$",
        message = "Invalid repository URL. Only GitHub and GitLab URLs are supported"
    )
    private String repositoryUrl;
    
    private String accessToken; // Optional for private repositories
    
    private String branch; // Optional, defaults to default branch
    
    private String path; // Optional, defaults to root directory
    
    private Boolean recursive; // Optional, defaults to false (only immediate children)
    
    public RepositoryTreeRequest() {}
    
    public RepositoryTreeRequest(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }
    
    public RepositoryTreeRequest(String repositoryUrl, String accessToken) {
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
    
    public String getBranch() {
        return branch;
    }
    
    public void setBranch(String branch) {
        this.branch = branch;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Boolean getRecursive() {
        return recursive;
    }
    
    public void setRecursive(Boolean recursive) {
        this.recursive = recursive;
    }
    
    @Override
    public String toString() {
        return "RepositoryTreeRequest{" +
                "repositoryUrl='" + repositoryUrl + '\'' +
                ", branch='" + branch + '\'' +
                ", path='" + path + '\'' +
                ", recursive=" + recursive +
                ", hasAccessToken=" + (accessToken != null && !accessToken.isEmpty()) +
                '}';
    }
}