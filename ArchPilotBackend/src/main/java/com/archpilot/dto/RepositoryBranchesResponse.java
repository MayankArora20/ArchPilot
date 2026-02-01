package com.archpilot.dto;

import java.time.LocalDateTime;
import java.util.List;

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
    
    public static class BranchInfo {
        private String name;
        private String sha;
        private boolean isDefault;
        private boolean isProtected;
        private String lastCommitMessage;
        private String lastCommitAuthor;
        private LocalDateTime lastCommitDate;
        
        public BranchInfo() {}
        
        public BranchInfo(String name, String sha, boolean isDefault) {
            this.name = name;
            this.sha = sha;
            this.isDefault = isDefault;
        }
        
        public BranchInfo(String name, String sha, boolean isDefault, boolean isProtected, 
                         String lastCommitMessage, String lastCommitAuthor, LocalDateTime lastCommitDate) {
            this.name = name;
            this.sha = sha;
            this.isDefault = isDefault;
            this.isProtected = isProtected;
            this.lastCommitMessage = lastCommitMessage;
            this.lastCommitAuthor = lastCommitAuthor;
            this.lastCommitDate = lastCommitDate;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getSha() { return sha; }
        public void setSha(String sha) { this.sha = sha; }
        
        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
        
        public boolean isProtected() { return isProtected; }
        public void setProtected(boolean isProtected) { this.isProtected = isProtected; }
        
        public String getLastCommitMessage() { return lastCommitMessage; }
        public void setLastCommitMessage(String lastCommitMessage) { this.lastCommitMessage = lastCommitMessage; }
        
        public String getLastCommitAuthor() { return lastCommitAuthor; }
        public void setLastCommitAuthor(String lastCommitAuthor) { this.lastCommitAuthor = lastCommitAuthor; }
        
        public LocalDateTime getLastCommitDate() { return lastCommitDate; }
        public void setLastCommitDate(LocalDateTime lastCommitDate) { this.lastCommitDate = lastCommitDate; }
    }
}