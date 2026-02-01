package com.archpilot.model;

import java.time.LocalDateTime;

public class BranchInfo {
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