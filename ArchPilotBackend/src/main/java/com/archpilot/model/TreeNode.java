package com.archpilot.model;

import java.util.List;

public class TreeNode {
    private String name;
    private String path;
    private String type; // "file" or "dir"
    private String sha;
    private Long size;
    private String url;
    private String downloadUrl;
    private List<TreeNode> children; // For recursive tree structure
    
    public TreeNode() {}
    
    public TreeNode(String name, String path, String type, String sha, Long size, String url, String downloadUrl) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.sha = sha;
        this.size = size;
        this.url = url;
        this.downloadUrl = downloadUrl;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }
    
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    
    public List<TreeNode> getChildren() { return children; }
    public void setChildren(List<TreeNode> children) { this.children = children; }
}