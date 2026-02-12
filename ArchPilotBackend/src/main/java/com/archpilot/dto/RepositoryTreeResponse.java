package com.archpilot.dto;

import java.util.List;

import com.archpilot.model.TreeNode;

public class RepositoryTreeResponse {
    private String status;
    private String message;
    private String repositoryUrl;
    private String branch;
    private String platform;
    private List<TreeNode> tree;
    
    public RepositoryTreeResponse() {}
    
    public RepositoryTreeResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public static RepositoryTreeResponse success(String repositoryUrl, String branch, 
                                               List<TreeNode> tree, String platform) {
        RepositoryTreeResponse response = new RepositoryTreeResponse("Success", "Tree structure retrieved successfully");
        response.repositoryUrl = repositoryUrl;
        response.branch = branch;
        response.tree = tree;
        response.platform = platform;
        return response;
    }
    
    public static RepositoryTreeResponse error(String message) {
        return new RepositoryTreeResponse("Error", message);
    }
    
    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public List<TreeNode> getTree() { return tree; }
    public void setTree(List<TreeNode> tree) { this.tree = tree; }
}