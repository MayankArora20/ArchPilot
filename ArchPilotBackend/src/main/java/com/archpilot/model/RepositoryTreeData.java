package com.archpilot.model;

import java.util.List;

public class RepositoryTreeData {
    private String repositoryUrl;
    private String branch;
    private String path;
    private String platform;
    private List<TreeNode> tree;
    
    public RepositoryTreeData() {}
    
    public RepositoryTreeData(String repositoryUrl, String branch, String path, 
                             List<TreeNode> tree, String platform) {
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.path = path;
        this.tree = tree;
        this.platform = platform;
    }
    
    // Getters and Setters
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public List<TreeNode> getTree() { return tree; }
    public void setTree(List<TreeNode> tree) { this.tree = tree; }
}