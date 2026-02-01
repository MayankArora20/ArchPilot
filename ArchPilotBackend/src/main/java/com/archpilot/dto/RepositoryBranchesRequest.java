package com.archpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RepositoryBranchesRequest {
    
    @NotBlank(message = "Repository URL is required")
    @Pattern(
        regexp = "^https://github\\.com/[\\w.-]+/[\\w.-]+/?$|^https://gitlab\\.com/[\\w.-]+/[\\w.-]+/?$",
        message = "Invalid repository URL. Only GitHub and GitLab URLs are supported"
    )
    private String repositoryUrl;
    
    private String accessToken; // Optional for private repositories
    private Integer limit = 50; // Default limit for branches
    
    public RepositoryBranchesRequest() {}
    
    public RepositoryBranchesRequest(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }
    
    public RepositoryBranchesRequest(String repositoryUrl, String accessToken) {
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
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit != null && limit > 0 && limit <= 100 ? limit : 50;
    }
    
    @Override
    public String toString() {
        return "RepositoryBranchesRequest{" +
                "repositoryUrl='" + repositoryUrl + '\'' +
                ", hasAccessToken=" + (accessToken != null && !accessToken.isEmpty()) +
                ", limit=" + limit +
                '}';
    }
}