package com.archpilot.service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.archpilot.model.RepositoryTreeData;
import com.archpilot.model.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ClassDiagramGeneratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(ClassDiagramGeneratorService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;
    private final ExecutorService executorService;
    
    public ClassDiagramGeneratorService() {
        this.webClient = WebClient.builder().build();
        this.executorService = Executors.newFixedThreadPool(7);
    }
    
    public Map<String, Object> generateClassDiagram(RepositoryTreeData treeData) {
        return generateClassDiagram(treeData, null, null);
    }
    
    public Map<String, Object> generateClassDiagram(RepositoryTreeData treeData, String accessToken, String branch) {
        logger.info("Generating class diagram for repository: {}", treeData.getRepositoryUrl());
        
        try {
            // Extract Java classes from tree data
            List<JavaClassInfo> javaClasses = extractJavaClasses(treeData);
            
            // Process Java classes in parallel using ExecutorService
            processJavaClassesInParallel(javaClasses, treeData.getRepositoryUrl(), accessToken, 
                                       branch != null ? branch : treeData.getBranch());
            
            // Generate PlantUML diagram
            String plantUmlContent = generatePlantUML(javaClasses, treeData);
            
            // Generate JSON representation
            Map<String, Object> jsonData = generateJsonRepresentation(javaClasses, treeData);
            
            // Create umlDigr directory and save files
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String repositoryName = extractRepositoryName(treeData.getRepositoryUrl());
            
            saveToFiles(plantUmlContent, jsonData, repositoryName, timestamp);
            
            // Return response
            Map<String, Object> response = new HashMap<>();
            response.put("plantUmlGenerated", true);
            response.put("jsonGenerated", true);
            response.put("classCount", javaClasses.size());
            response.put("timestamp", timestamp);
            response.put("repositoryName", repositoryName);
            response.put("plantUmlContent", plantUmlContent);
            response.put("jsonData", jsonData);
            
            return response;
            
        } catch (IOException e) {
            logger.error("Error generating class diagram: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate class diagram: " + e.getMessage());
            return errorResponse;
        } catch (Exception e) {
            logger.error("Unexpected error generating class diagram: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate class diagram: " + e.getMessage());
            return errorResponse;
        }
    }
    
    private void processJavaClassesInParallel(List<JavaClassInfo> javaClasses, String repositoryUrl, 
                                             String accessToken, String branch) {
        if (javaClasses == null || javaClasses.isEmpty()) {
            logger.info("No Java classes to process");
            return;
        }
        
        logger.info("Starting parallel processing of {} Java classes with 7 threads", javaClasses.size());
        
        // Create CompletableFuture tasks for each JavaClassInfo
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (JavaClassInfo javaClass : javaClasses) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                fetchAndProcessJavaClassContent(javaClass, repositoryUrl, accessToken, branch);
            }, executorService);
            futures.add(future);
        }
        
        // Wait for all tasks to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            // Wait for completion with timeout
            allFutures.get(30, TimeUnit.SECONDS);
            logger.info("All Java classes processed successfully");
        } catch (Exception e) {
            logger.error("Error processing Java classes in parallel: {}", e.getMessage(), e);
        }
    }
    
    private void fetchAndProcessJavaClassContent(JavaClassInfo javaClass, String repositoryUrl, 
                                               String accessToken, String branch) {
        try {
            // Construct GitHub API URL for file content
            String fileContentUrl = buildGitHubFileContentUrl(repositoryUrl, javaClass.getFullPath(), branch);
            
            var webClientBuilder = webClient.get().uri(fileContentUrl);
            
            // Add authorization header if token is provided
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                webClientBuilder = webClientBuilder.header("Authorization", "token " + accessToken);
            }
            
            // GitHub API returns JSON with base64 encoded content
            Map<String, Object> response = webClientBuilder
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (response != null && response.containsKey("content")) {
                String base64Content = (String) response.get("content");
                // Remove newlines from base64 content
                base64Content = base64Content.replaceAll("\\s", "");
                
                // Decode base64 content
                byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
                String content = new String(decodedBytes, StandardCharsets.UTF_8);
                
                int characterCount = content.length();
                System.out.println(String.format("Thread: %s | File: %s | Characters: %d", 
                    Thread.currentThread().getName(), 
                    javaClass.getClassName() + ".java", 
                    characterCount));
                logger.info("Processed Java class: {} with {} characters", javaClass.getClassName(), characterCount);
            } else {
                logger.warn("No content received for Java class: {}", javaClass.getClassName());
            }
        } catch (Exception e) {
            logger.error("Error fetching content for Java class {}: {}", javaClass.getClassName(), e.getMessage(), e);
        }
    }
    
    private String buildGitHubFileContentUrl(String repositoryUrl, String filePath, String branch) {
        // Extract owner and repo from GitHub URL
        // Example: https://github.com/owner/repo -> owner/repo
        String[] urlParts = repositoryUrl.replace("https://github.com/", "").split("/");
        if (urlParts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub repository URL: " + repositoryUrl);
        }
        
        String owner = urlParts[0];
        String repo = urlParts[1];
        
        // GitHub API URL format: https://api.github.com/repos/{owner}/{repo}/contents/{path}?ref={branch}
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, repo, filePath);
        
        if (branch != null && !branch.trim().isEmpty()) {
            apiUrl += "?ref=" + branch;
        }
        
        return apiUrl;
    }
    
    private List<JavaClassInfo> extractJavaClasses(RepositoryTreeData treeData) {
        List<JavaClassInfo> javaClasses = new ArrayList<>();
        
        if (treeData.getTree() != null) {
            extractJavaClassesFromNodes(treeData.getTree(), javaClasses);
        }
        
        return javaClasses;
    }
    
    private void extractJavaClassesFromNodes(List<TreeNode> nodes, List<JavaClassInfo> javaClasses) {
        for (TreeNode node : nodes) {
            if ("file".equals(node.getType()) && node.getName().endsWith(".java")) {
                JavaClassInfo classInfo = new JavaClassInfo();
                classInfo.setClassName(extractClassName(node.getName()));
                classInfo.setFullPath(node.getPath());
                classInfo.setPackageName(extractPackageName(node.getPath()));
                classInfo.setSha(node.getSha());
                classInfo.setSize(node.getSize());
                classInfo.setUrl(node.getUrl());
                classInfo.setDownloadUrl(node.getDownloadUrl());
                
                javaClasses.add(classInfo);
            }
            
            if (node.getChildren() != null) {
                extractJavaClassesFromNodes(node.getChildren(), javaClasses);
            }
        }
    }
    
    private String extractClassName(String fileName) {
        return fileName.replace(".java", "");
    }
    
    private String extractPackageName(String filePath) {
        // Extract package name from file path
        // Example: src/main/java/com/archpilot/service/MyClass.java -> com.archpilot.service
        String[] pathParts = filePath.split("/");
        List<String> packageParts = new ArrayList<>();
        
        boolean foundJava = false;
        for (String part : pathParts) {
            if (foundJava && !part.endsWith(".java")) {
                packageParts.add(part);
            } else if ("java".equals(part)) {
                foundJava = true;
            }
        }
        
        return String.join(".", packageParts);
    }
    
    private String generatePlantUML(List<JavaClassInfo> javaClasses, RepositoryTreeData treeData) {
        StringBuilder plantUml = new StringBuilder();
        
        plantUml.append("@startuml\n");
        plantUml.append("!theme plain\n");
        plantUml.append("title Class Diagram - ").append(extractRepositoryName(treeData.getRepositoryUrl())).append("\n\n");
        
        // Group classes by package
        Map<String, List<JavaClassInfo>> packageGroups = new HashMap<>();
        for (JavaClassInfo classInfo : javaClasses) {
            String packageName = classInfo.getPackageName();
            if (packageName.isEmpty()) {
                packageName = "default";
            }
            packageGroups.computeIfAbsent(packageName, k -> new ArrayList<>()).add(classInfo);
        }
        
        // Generate PlantUML for each package
        for (Map.Entry<String, List<JavaClassInfo>> entry : packageGroups.entrySet()) {
            String packageName = entry.getKey();
            List<JavaClassInfo> classes = entry.getValue();
            
            if (!"default".equals(packageName)) {
                plantUml.append("package \"").append(packageName).append("\" {\n");
            }
            
            for (JavaClassInfo classInfo : classes) {
                plantUml.append("  class ").append(classInfo.getClassName()).append(" {\n");
                plantUml.append("    ' SHA: ").append(classInfo.getSha()).append("\n");
                plantUml.append("    ' Path: ").append(classInfo.getFullPath()).append("\n");
                plantUml.append("  }\n");
            }
            
            if (!"default".equals(packageName)) {
                plantUml.append("}\n\n");
            }
        }
        
        plantUml.append("@enduml\n");
        
        return plantUml.toString();
    }
    
    private Map<String, Object> generateJsonRepresentation(List<JavaClassInfo> javaClasses, RepositoryTreeData treeData) {
        Map<String, Object> jsonData = new HashMap<>();
        
        jsonData.put("repositoryUrl", treeData.getRepositoryUrl());
        jsonData.put("branch", treeData.getBranch());
        jsonData.put("platform", treeData.getPlatform());
        jsonData.put("generatedAt", LocalDateTime.now().toString());
        jsonData.put("totalClasses", javaClasses.size());
        
        // Group by packages
        Map<String, List<Map<String, Object>>> packages = new HashMap<>();
        
        for (JavaClassInfo classInfo : javaClasses) {
            String packageName = classInfo.getPackageName();
            if (packageName.isEmpty()) {
                packageName = "default";
            }
            
            Map<String, Object> classData = new HashMap<>();
            classData.put("className", classInfo.getClassName());
            classData.put("fullPath", classInfo.getFullPath());
            classData.put("sha", classInfo.getSha());
            classData.put("size", classInfo.getSize());
            classData.put("url", classInfo.getUrl());
            classData.put("downloadUrl", classInfo.getDownloadUrl());
            
            packages.computeIfAbsent(packageName, k -> new ArrayList<>()).add(classData);
        }
        
        jsonData.put("packages", packages);
        
        return jsonData;
    }
    
    private String extractRepositoryName(String repositoryUrl) {
        // Extract repository name from URL
        // Example: https://github.com/owner/repo -> repo
        String[] parts = repositoryUrl.split("/");
        return parts[parts.length - 1];
    }
    
    private void saveToFiles(String plantUmlContent, Map<String, Object> jsonData, 
                           String repositoryName, String timestamp) throws IOException {
        
        // Create umlDigr directory parallel to resources
        Path umlDigrPath = Paths.get("umlDigr");
        if (!Files.exists(umlDigrPath)) {
            Files.createDirectories(umlDigrPath);
        }
        
        // Save PlantUML file
        String plantUmlFileName = String.format("%s_%s.puml", repositoryName, timestamp);
        Path plantUmlFilePath = umlDigrPath.resolve(plantUmlFileName);
        
        try (FileWriter writer = new FileWriter(plantUmlFilePath.toFile())) {
            writer.write(plantUmlContent);
        }
        
        // Save JSON file
        String jsonFileName = String.format("%s_%s.json", repositoryName, timestamp);
        Path jsonFilePath = umlDigrPath.resolve(jsonFileName);
        
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFilePath.toFile(), jsonData);
        
        logger.info("Files saved: {} and {}", plantUmlFileName, jsonFileName);
    }
    
    // Inner class for Java class information
    public static class JavaClassInfo {
        private String className;
        private String fullPath;
        private String packageName;
        private String sha;
        private Long size;
        private String url;
        private String downloadUrl;
        
        // Getters and setters
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        
        public String getFullPath() { return fullPath; }
        public void setFullPath(String fullPath) { this.fullPath = fullPath; }
        
        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }
        
        public String getSha() { return sha; }
        public void setSha(String sha) { this.sha = sha; }
        
        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    }
    
    // Cleanup method for ExecutorService
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}