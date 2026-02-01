package com.archpilot.model;

import java.util.List;

public class RepositoryBranchesData {
    private String repositoryUrl;
    private List<BranchInfo> branches;
    private Integer totalBranches;
    private String platform;
    
    public RepositoryBranchesData() {}
    
    public RepositoryBranchesData(String repositoryUrl, List<BranchInfo> branches, String platform) {
        this.repositoryUrl = repositoryUrl;
        this.branches = branches;
        this.totalBranches = branches != null ? branches.size() : 0;
        this.platform = platform;
    }
    
    // Getters and Setters
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    
    public List<BranchInfo> getBranches() { return branches; }
    public void setBranches(List<BranchInfo> branches) { 
        this.branches = branches;
        this.totalBranches = branches != null ? branches.size() : 0;
    }
    
    public Integer getTotalBranches() { return totalBranches; }
    public void setTotalBranches(Integer totalBranches) { this.totalBranches = totalBranches; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}