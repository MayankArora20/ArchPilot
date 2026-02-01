package com.archpilot.model;

public class RepositoryInfo {
    private String name;
    private String fullName;
    private String description;
    private String defaultBranch;
    private boolean isPrivate;
    private String language;
    private String platform;
    private String repositoryUrl;
    
    public RepositoryInfo() {}
    
    public RepositoryInfo(String name, String fullName, String description, 
                         String defaultBranch, boolean isPrivate, String language, 
                         String platform, String repositoryUrl) {
        this.name = name;
        this.fullName = fullName;
        this.description = description;
        this.defaultBranch = defaultBranch;
        this.isPrivate = isPrivate;
        this.language = language;
        this.platform = platform;
        this.repositoryUrl = repositoryUrl;
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
    
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
}