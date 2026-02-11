package com.archpilot.dto;

import java.util.List;

public class RepositoryTreeResponse {
    private String status;
    private String message;
    private String repositoryUrl;
    private String branch;
    private String platform;
    private List<TreeItem> tree;
    
    public RepositoryTreeResponse() {}
    
    public RepositoryTreeResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public static RepositoryTreeResponse success(String repositoryUrl, String branch, 
                                               List<TreeItem> tree, String platform) {
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
    
    public List<TreeItem> getTree() { return tree; }
    public void setTree(List<TreeItem> tree) { this.tree = tree; }
    
    public static class TreeItem {
        private String name;
        private String path;
        private String type; // "file" or "dir"
        private String sha;
        private Long size;
        private String url;
        private String downloadUrl;
        private List<TreeItem> children; // For recursive tree structure
        
        public TreeItem() {}
        
        public TreeItem(String name, String path, String type, String sha, Long size, String url, String downloadUrl) {
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
        
        public List<TreeItem> getChildren() { return children; }
        public void setChildren(List<TreeItem> children) { this.children = children; }
    }
}