package com.archpilot.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.archpilot.model.BranchInfo;

public class RepositoryBranchesResponse {
    
    private String status; // "Success" or "Error"
    private String message;
    private String repositoryUrl;
    private List<BranchInfo> branches;
    private Integer totalBranches;
    private String platform; // "GitHub" or "GitLab"
    private LocalDateTime timestamp;
    
    public RepositoryBranchesResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public RepositoryBranchesResponse(String status, String message) {
        this();
        this.status = status;
        this.message = message;
    }
    
    public static RepositoryBranchesResponse success(String repositoryUrl, List<BranchInfo> branches, String platform) {
        RepositoryBranchesResponse response = new RepositoryBranchesResponse("Success", "Branches retrieved successfully");
        response.setRepositoryUrl(repositoryUrl);
        response.setBranches(branches);
        response.setTotalBranches(branches.size());
        response.setPlatform(platform);
        return response;
    }
    
    public static RepositoryBranchesResponse error(String errorMessage) {
        return new RepositoryBranchesResponse("Error", errorMessage);
    }
    
    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    
    public List<BranchInfo> getBranches() { return branches; }
    public void setBranches(List<BranchInfo> branches) { this.branches = branches; }
    
    public Integer getTotalBranches() { return totalBranches; }
    public void setTotalBranches(Integer totalBranches) { this.totalBranches = totalBranches; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}