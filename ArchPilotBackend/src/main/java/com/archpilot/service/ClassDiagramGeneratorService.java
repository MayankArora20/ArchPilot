package com.archpilot.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.archpilot.dto.ClassDiagramResponse;
import com.archpilot.model.RepositoryTreeData;
import com.archpilot.model.TreeNode;
import com.archpilot.service.agent.GeminiAgentService;
import com.archpilot.service.agent.GeminiClassAnalyzerService;

@Service
public class ClassDiagramGeneratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(ClassDiagramGeneratorService.class);
    
    @Autowired
    private GeminiAgentService geminiAgentService;
    
    @Autowired
    private PlantUmlToPngService plantUmlToPngService;
    
    /**
     * Generate class diagram and return only PNG image data in the specified format
     * Uses SHA-based caching to avoid regenerating unchanged projects
     */
    public ClassDiagramResponse generateClassDiagramImage(RepositoryTreeData treeData) {
        logger.info("Generating class diagram image for repository: {}", treeData.getRepositoryUrl());
        
        try {
            String projectSha = treeData.getCommitSha();
            String repositoryName = extractRepositoryName(treeData.getRepositoryUrl());
            
            if (projectSha == null || projectSha.isEmpty()) {
                logger.warn("No commit SHA available, proceeding without caching");
                projectSha = "no-sha-" + System.currentTimeMillis();
            } else {
                logger.info("Using commit SHA for caching: {} for repository: {}", projectSha, repositoryName);
                
                // Check if we have a cached image for this exact project state
                ClassDiagramResponse cachedResponse = checkForCachedImage(repositoryName, projectSha);
                if (cachedResponse != null) {
                    logger.info("Found existing image for project SHA: {}, returning cached result", projectSha);
                    return cachedResponse;
                }
                
                logger.info("No cached image found for project SHA: {}, generating new image", projectSha);
            }
            
            // Make projectSha effectively final for lambda
            final String finalProjectSha = projectSha;
            
            // Run the blocking operations in a separate thread
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return generateClassDiagramImageBlocking(treeData, finalProjectSha);
                } catch (Exception e) {
                    logger.error("Error in async class diagram image generation: {}", e.getMessage(), e);
                    return ClassDiagramResponse.error("Failed to generate class diagram image: " + e.getMessage());
                }
            }).get();
            
        } catch (Exception e) {
            logger.error("Error generating class diagram image: {}", e.getMessage(), e);
            return ClassDiagramResponse.error("Failed to generate class diagram image: " + e.getMessage());
        }
    }
    
    private ClassDiagramResponse generateClassDiagramImageBlocking(RepositoryTreeData treeData, String projectSha) {
        // Extract Java classes from tree data
        List<JavaClassInfo> javaClasses = extractJavaClasses(treeData);
        logger.info("Found {} Java classes to analyze", javaClasses.size());
        
        if (javaClasses.isEmpty()) {
            return ClassDiagramResponse.error("No Java classes found in the repository");
        }
        
        // Generate basic PlantUML structure
        String basicPlantUml = generateBasicPlantUML(javaClasses, treeData);
        logger.info("Generated basic PlantUML structure");
        
        // Fetch file contents and analyze with enhanced relationships
        Map<String, GeminiClassAnalyzerService.ClassAnalysisResult> analysisResults = 
            analyzeClassesSequentially(javaClasses, treeData, basicPlantUml);
        logger.info("Completed enhanced analysis for {} classes", analysisResults.size());
        
        // Generate enhanced PlantUML diagram with analysis results
        String enhancedPlantUml = generateEnhancedPlantUMLFromAnalysis(basicPlantUml, analysisResults, treeData);
        
        // Generate PNG from enhanced PlantUML
        String pngBase64 = null;
        try {
            pngBase64 = plantUmlToPngService.convertToPngBase64(enhancedPlantUml);
            logger.info("Successfully generated PNG from enhanced PlantUML");
        } catch (Exception e) {
            logger.error("Error generating PNG from PlantUML: {}", e.getMessage(), e);
            return ClassDiagramResponse.error("Failed to generate PNG from PlantUML: " + e.getMessage());
        }
        
        // Create timestamp and filenames
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String repositoryName = extractRepositoryName(treeData.getRepositoryUrl());
        String mainPngFileName = String.format("%s_%s.png", repositoryName, timestamp);
        String litePngFileName = String.format("%s_%s_lite.png", repositoryName, timestamp);
        
        // Save PNG file to disk and metadata for caching
        try {
            plantUmlToPngService.savePngToFile(enhancedPlantUml, mainPngFileName, "umlDigr");
            logger.info("Saved PNG file: {}", mainPngFileName);
            
            // Save metadata for caching
            saveMetadata(repositoryName, timestamp, javaClasses.size(), projectSha);
            
        } catch (Exception e) {
            logger.warn("Error saving PNG file or metadata: {}", e.getMessage());
        }
        
        // Create response data
        ClassDiagramResponse.ClassDiagramData data = new ClassDiagramResponse.ClassDiagramData(
            mainPngFileName,
            javaClasses.size(),
            pngBase64,
            repositoryName,
            litePngFileName,
            timestamp
        );
        
        return ClassDiagramResponse.success("Class diagram generated successfully", data);
    }
    
    /**
     * Check for cached image by looking at existing PNG files in umlDigr directory
     */
    private ClassDiagramResponse checkForCachedImage(String repositoryName, String projectSha) {
        try {
            Path umlDigrPath = Paths.get("umlDigr");
            if (!Files.exists(umlDigrPath)) {
                return null;
            }
            
            // Look for PNG files matching the repository name pattern
            return Files.list(umlDigrPath)
                .filter(path -> path.toString().endsWith(".png"))
                .filter(path -> path.getFileName().toString().startsWith(repositoryName + "_"))
                .filter(path -> !path.getFileName().toString().contains("_lite.png")) // Skip lite versions
                .map(pngFile -> {
                    try {
                        // Check if there's a corresponding metadata file with the same SHA
                        String pngFileName = pngFile.getFileName().toString();
                        String metadataFileName = pngFileName.replace(".png", "_metadata.txt");
                        Path metadataFile = umlDigrPath.resolve(metadataFileName);
                        
                        if (Files.exists(metadataFile)) {
                            String metadataContent = Files.readString(metadataFile);
                            
                            // Check if the project SHA matches
                            if (metadataContent.contains("projectSha=" + projectSha)) {
                                logger.info("Found matching cached image: {} with SHA: {}", pngFileName, projectSha);
                                
                                // Read the PNG file and convert to base64
                                byte[] pngBytes = Files.readAllBytes(pngFile);
                                String pngBase64 = java.util.Base64.getEncoder().encodeToString(pngBytes);
                                
                                // Extract information from metadata
                                String repositoryNameFromFile = extractMetadataValue(metadataContent, "repositoryName");
                                String timestampFromFile = extractMetadataValue(metadataContent, "timestamp");
                                String classCountStr = extractMetadataValue(metadataContent, "classCount");
                                int classCount = classCountStr != null ? Integer.parseInt(classCountStr) : 0;
                                
                                String litePngFileName = pngFileName.replace(".png", "_lite.png");
                                
                                // Create response data
                                ClassDiagramResponse.ClassDiagramData data = new ClassDiagramResponse.ClassDiagramData(
                                    pngFileName,
                                    classCount,
                                    pngBase64,
                                    repositoryNameFromFile != null ? repositoryNameFromFile : repositoryName,
                                    litePngFileName,
                                    timestampFromFile
                                );
                                
                                return ClassDiagramResponse.success("Class diagram retrieved from cache", data);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error reading cached file {}: {}", pngFile.getFileName(), e.getMessage());
                    }
                    return null;
                })
                .filter(result -> result != null)
                .findFirst()
                .orElse(null);
                
        } catch (Exception e) {
            logger.error("Error checking for cached image: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract value from metadata content
     */
    private String extractMetadataValue(String metadataContent, String key) {
        String[] lines = metadataContent.split("\n");
        for (String line : lines) {
            if (line.startsWith(key + "=")) {
                return line.substring(key.length() + 1);
            }
        }
        return null;
    }
    
    /**
     * Save metadata file alongside the PNG for caching purposes
     */
    private void saveMetadata(String repositoryName, String timestamp, int classCount, String projectSha) {
        try {
            Path umlDigrPath = Paths.get("umlDigr");
            if (!Files.exists(umlDigrPath)) {
                Files.createDirectories(umlDigrPath);
            }
            
            String metadataFileName = String.format("%s_%s_metadata.txt", repositoryName, timestamp);
            Path metadataFilePath = umlDigrPath.resolve(metadataFileName);
            
            String metadataContent = String.format(
                "repositoryName=%s\n" +
                "timestamp=%s\n" +
                "classCount=%d\n" +
                "projectSha=%s\n" +
                "generatedAt=%s\n",
                repositoryName,
                timestamp,
                classCount,
                projectSha,
                LocalDateTime.now().toString()
            );
            
            Files.writeString(metadataFilePath, metadataContent);
            logger.info("Saved metadata file: {}", metadataFileName);
            
        } catch (Exception e) {
            logger.warn("Error saving metadata file: {}", e.getMessage());
        }
    }
    
    /**
     * Clean up old cached files (optional maintenance method)
     * Removes files older than 7 days to prevent disk space issues
     */
    public void cleanupOldCachedFiles() {
        try {
            Path umlDigrPath = Paths.get("umlDigr");
            if (!Files.exists(umlDigrPath)) {
                return;
            }
            
            long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
            
            Files.list(umlDigrPath)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < sevenDaysAgo;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        logger.info("Deleted old cached file: {}", path.getFileName());
                    } catch (Exception e) {
                        logger.warn("Error deleting old cached file {}: {}", path.getFileName(), e.getMessage());
                    }
                });
                
        } catch (Exception e) {
            logger.error("Error during cache cleanup: {}", e.getMessage());
        }
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
    
    private String generateBasicPlantUML(List<JavaClassInfo> javaClasses, RepositoryTreeData treeData) {
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
        
        // Generate basic PlantUML for each package
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
    
    private String extractRepositoryName(String repositoryUrl) {
        // Extract repository name from URL
        // Example: https://github.com/owner/repo -> repo
        String[] parts = repositoryUrl.split("/");
        return parts[parts.length - 1];
    }
    
    /**
     * Analyze classes sequentially with enhanced relationship detection
     */
    private Map<String, GeminiClassAnalyzerService.ClassAnalysisResult> analyzeClassesSequentially(
            List<JavaClassInfo> javaClasses, RepositoryTreeData treeData, String basicPlantUml) {
        
        Map<String, GeminiClassAnalyzerService.ClassAnalysisResult> analysisResults = new HashMap<>();
        
        // Limit the number of files to analyze based on token guardrail (max 5 files for free tier)
        int maxFiles = Math.min(javaClasses.size(), 5);
        logger.info("Limiting analysis to {} files due to token guardrail restrictions", maxFiles);
        
        for (int i = 0; i < maxFiles; i++) {
            JavaClassInfo javaClass = javaClasses.get(i);
            
            try {
                // Fetch file content using raw GitHub URL
                String content = fetchSingleFileContentFromGitHub(javaClass, treeData);
                
                if (content != null && !content.trim().isEmpty()) {
                    // Limit content size to respect token limits (max 3000 chars per file)
                    if (content.length() > 3000) {
                        content = content.substring(0, 3000) + "\n// ... (truncated due to token limits)";
                        logger.info("Truncated content for {} due to token limits", javaClass.getClassName());
                    }
                    
                    // Analyze using Gemini with context of existing UML
                    GeminiClassAnalyzerService.ClassAnalysisResult result = 
                        analyzeClassWithContext(javaClass.getClassName(), content, basicPlantUml);
                    
                    if (result != null) {
                        analysisResults.put(javaClass.getClassName(), result);
                        logger.info("Successfully analyzed class: {}", javaClass.getClassName());
                    }
                }
                
                // Add small delay to respect rate limits
                Thread.sleep(1000);
                
            } catch (Exception e) {
                logger.error("Error analyzing class {}: {}", javaClass.getClassName(), e.getMessage());
            }
        }
        
        return analysisResults;
    }
    
    /**
     * Fetch single file content from GitHub raw URL
     */
    private String fetchSingleFileContentFromGitHub(JavaClassInfo javaClass, RepositoryTreeData treeData) {
        try {
            // Construct raw GitHub URL from repository URL and file path
            String rawUrl = constructRawGitHubUrl(treeData.getRepositoryUrl(), treeData.getBranch(), javaClass.getFullPath());
            
            if (rawUrl != null) {
                logger.info("Fetching content from: {}", rawUrl);
                
                // Use RestTemplate instead of WebClient to avoid blocking issues
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                String content = restTemplate.getForObject(rawUrl, String.class);
                
                if (content != null && !content.trim().isEmpty()) {
                    logger.info("Successfully fetched {} characters for class: {}", content.length(), javaClass.getClassName());
                    return content;
                } else {
                    logger.warn("Empty content received for class: {}", javaClass.getClassName());
                    return null;
                }
            } else {
                logger.warn("Could not construct raw URL for class: {}", javaClass.getClassName());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error fetching file content for {}: {}", javaClass.getClassName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Construct raw GitHub URL for file content
     */
    private String constructRawGitHubUrl(String repositoryUrl, String branch, String filePath) {
        try {
            // Convert https://github.com/owner/repo to https://raw.githubusercontent.com/owner/repo/branch/path
            if (repositoryUrl.contains("github.com")) {
                String[] parts = repositoryUrl.replace("https://github.com/", "").split("/");
                if (parts.length >= 2) {
                    String owner = parts[0];
                    String repo = parts[1];
                    // Use the actual branch from the tree data, fallback to master, then main
                    String branchName = (branch != null && !branch.isEmpty()) ? branch : "master";
                    
                    String rawUrl = String.format("https://raw.githubusercontent.com/%s/%s/%s/%s", 
                                                 owner, repo, branchName, filePath);
                    logger.info("Constructed raw GitHub URL: {}", rawUrl);
                    return rawUrl;
                }
            }
        } catch (Exception e) {
            logger.error("Error constructing raw GitHub URL: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Analyze class with enhanced context for relationships
     */
    private GeminiClassAnalyzerService.ClassAnalysisResult analyzeClassWithContext(
            String className, String content, String existingUml) {
        
        logger.info("Starting enhanced Gemini analysis for class: {}", className);
        
        String prompt = String.format("""
            I have a basic PlantUML class diagram that I want to enhance with detailed class analysis. Here's the current UML:
            
            ```plantuml
            %s
            ```
            
            Now I want to analyze this Java class and extract detailed information including relationships, methods, and fields:
            
            Class Name: %s
            
            Java Code:
            ```java
            %s
            ```
            
            Please provide ONLY a JSON response with the following structure:
            {
                "className": "%s",
                "classType": "class|interface|enum|abstract class",
                "packageName": "extracted package name",
                "extends": "parent class name if any, null otherwise",
                "implements": ["list of implemented interfaces"],
                "fields": [
                    {
                        "name": "field name",
                        "type": "field type",
                        "visibility": "private|public|protected|package",
                        "isStatic": true/false,
                        "isFinal": true/false
                    }
                ],
                "methods": [
                    {
                        "name": "method name",
                        "returnType": "return type",
                        "visibility": "private|public|protected|package",
                        "isStatic": true/false,
                        "isAbstract": true/false,
                        "parameters": [
                            {
                                "name": "param name",
                                "type": "param type"
                            }
                        ]
                    }
                ],
                "usedClasses": ["list of other classes this class references or uses"],
                "annotations": ["list of class-level annotations"]
            }
            
            Focus on extracting:
            1. All fields with their types and visibility
            2. All method signatures with parameters and return types
            3. Class relationships (extends, implements)
            4. Other classes that this class uses or references
            5. Annotations and modifiers
            """, existingUml, className, content, className);
        
        try {
            logger.info("Sending enhanced prompt to Gemini for class: {}", className);
            String response = geminiAgentService.askQuestion(prompt);
            logger.info("Received response from Gemini for class: {} (length: {})", className, response != null ? response.length() : 0);
            
            if (response != null && !response.trim().isEmpty()) {
                return parseGeminiResponse(className, response);
            } else {
                logger.warn("Empty response from Gemini for class: {}", className);
                return createFallbackResult(className);
            }
        } catch (Exception e) {
            logger.error("Error calling Gemini API for class {}: {}", className, e.getMessage(), e);
            return createFallbackResult(className);
        }
    }
    
    /**
     * Parse Gemini response into structured result
     */
    private GeminiClassAnalyzerService.ClassAnalysisResult parseGeminiResponse(String className, String response) {
        try {
            // Extract JSON from response
            String jsonPart = extractJsonFromResponse(response);
            return parseJsonToClassAnalysisResult(jsonPart);
        } catch (Exception e) {
            logger.error("Error parsing Gemini response for class {}: {}", className, e.getMessage());
            return createFallbackResult(className);
        }
    }
    
    /**
     * Extract JSON block from Gemini response
     */
    private String extractJsonFromResponse(String response) {
        // Find JSON block in the response
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");
        
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }
        
        return response; // Return as-is if no clear JSON block found
    }
    
    /**
     * Parse JSON to ClassAnalysisResult
     */
    private GeminiClassAnalyzerService.ClassAnalysisResult parseJsonToClassAnalysisResult(String json) {
        GeminiClassAnalyzerService.ClassAnalysisResult result = new GeminiClassAnalyzerService.ClassAnalysisResult();
        
        try {
            // Extract basic information using simple string parsing
            result.setClassName(extractJsonValue(json, "className"));
            result.setClassType(extractJsonValue(json, "classType"));
            result.setPackageName(extractJsonValue(json, "packageName"));
            result.setExtendsClass(extractJsonValue(json, "extends"));
            result.setRawAnalysis(json);
            result.setAnalysisStatus("SUCCESS");
        } catch (Exception e) {
            logger.error("Error parsing JSON: {}", e.getMessage());
            result.setAnalysisStatus("FAILED");
        }
        
        return result;
    }
    
    /**
     * Extract value from JSON string
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }
    
    /**
     * Create fallback result for failed analysis
     */
    private GeminiClassAnalyzerService.ClassAnalysisResult createFallbackResult(String className) {
        GeminiClassAnalyzerService.ClassAnalysisResult result = new GeminiClassAnalyzerService.ClassAnalysisResult();
        result.setClassName(className);
        result.setClassType("class");
        result.setAnalysisStatus("FAILED");
        return result;
    }
    
    /**
     * Generate enhanced PlantUML from analysis results with relationships
     */
    private String generateEnhancedPlantUMLFromAnalysis(String basicPlantUml, 
                                                       Map<String, GeminiClassAnalyzerService.ClassAnalysisResult> analysisResults,
                                                       RepositoryTreeData treeData) {
        StringBuilder enhanced = new StringBuilder();
        
        enhanced.append("@startuml\n");
        enhanced.append("!theme plain\n");
        enhanced.append("title Enhanced Class Diagram - ").append(extractRepositoryName(treeData.getRepositoryUrl())).append("\n\n");
        
        // Group classes by package
        Map<String, List<JavaClassInfo>> packageGroups = new HashMap<>();
        for (GeminiClassAnalyzerService.ClassAnalysisResult analysis : analysisResults.values()) {
            if (analysis != null) {
                String packageName = analysis.getPackageName();
                if (packageName == null || packageName.isEmpty()) {
                    packageName = "default";
                }
                
                // Create a JavaClassInfo for this analysis
                JavaClassInfo classInfo = new JavaClassInfo();
                classInfo.setClassName(analysis.getClassName());
                classInfo.setPackageName(packageName);
                
                packageGroups.computeIfAbsent(packageName, k -> new ArrayList<>()).add(classInfo);
            }
        }
        
        // Generate enhanced PlantUML for each package
        for (Map.Entry<String, List<JavaClassInfo>> entry : packageGroups.entrySet()) {
            String packageName = entry.getKey();
            List<JavaClassInfo> classes = entry.getValue();
            
            if (!"default".equals(packageName)) {
                enhanced.append("package \"").append(packageName).append("\" {\n");
            }
            
            for (JavaClassInfo classInfo : classes) {
                GeminiClassAnalyzerService.ClassAnalysisResult analysis = analysisResults.get(classInfo.getClassName());
                
                if (analysis != null && "SUCCESS".equals(analysis.getAnalysisStatus())) {
                    generateDetailedClassDefinition(enhanced, analysis);
                }
            }
            
            if (!"default".equals(packageName)) {
                enhanced.append("}\n\n");
            }
        }
        
        // Generate relationships
        generateClassRelationshipsFromAnalysis(enhanced, analysisResults);
        
        enhanced.append("@enduml\n");
        
        return enhanced.toString();
    }
    
    /**
     * Generate detailed class definition with fields and methods
     */
    private void generateDetailedClassDefinition(StringBuilder plantUml, GeminiClassAnalyzerService.ClassAnalysisResult analysis) {
        String classType = analysis.getClassType() != null ? analysis.getClassType() : "class";
        String className = analysis.getClassName();
        
        // Start class definition
        if ("interface".equals(classType)) {
            plantUml.append("  interface ").append(className).append(" {\n");
        } else if ("abstract class".equals(classType)) {
            plantUml.append("  abstract class ").append(className).append(" {\n");
        } else if ("enum".equals(classType)) {
            plantUml.append("  enum ").append(className).append(" {\n");
        } else {
            plantUml.append("  class ").append(className).append(" {\n");
        }
        
        // Parse and add fields from rawAnalysis
        addFieldsFromRawAnalysis(plantUml, analysis.getRawAnalysis());
        
        // Add separator between fields and methods
        plantUml.append("    --\n");
        
        // Parse and add methods from rawAnalysis
        addMethodsFromRawAnalysis(plantUml, analysis.getRawAnalysis());
        
        plantUml.append("  }\n");
    }
    
    /**
     * Add fields from raw analysis JSON
     */
    private void addFieldsFromRawAnalysis(StringBuilder plantUml, String rawAnalysis) {
        if (rawAnalysis == null) return;
        
        try {
            // Simple JSON parsing to extract fields
            String fieldsSection = extractJsonSection(rawAnalysis, "fields");
            if (fieldsSection != null) {
                String[] fields = fieldsSection.split("\\{");
                for (String field : fields) {
                    if (field.contains("\"name\"")) {
                        String name = extractJsonValue(field, "name");
                        String type = extractJsonValue(field, "type");
                        String visibility = extractJsonValue(field, "visibility");
                        boolean isStatic = field.contains("\"isStatic\": true");
                        boolean isFinal = field.contains("\"isFinal\": true");
                        
                        if (name != null && type != null) {
                            plantUml.append("    ");
                            
                            // Add visibility symbol
                            if ("private".equals(visibility)) plantUml.append("-");
                            else if ("protected".equals(visibility)) plantUml.append("#");
                            else if ("public".equals(visibility)) plantUml.append("+");
                            else plantUml.append("~");
                            
                            plantUml.append(name).append(" : ").append(type);
                            
                            if (isStatic) plantUml.append(" {static}");
                            if (isFinal) plantUml.append(" {final}");
                            
                            plantUml.append("\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error parsing fields from raw analysis: {}", e.getMessage());
        }
    }
    
    /**
     * Add methods from raw analysis JSON
     */
    private void addMethodsFromRawAnalysis(StringBuilder plantUml, String rawAnalysis) {
        if (rawAnalysis == null) return;
        
        try {
            // Simple JSON parsing to extract methods
            String methodsSection = extractJsonSection(rawAnalysis, "methods");
            if (methodsSection != null) {
                String[] methods = methodsSection.split("\\{");
                for (String method : methods) {
                    if (method.contains("\"name\"")) {
                        String name = extractJsonValue(method, "name");
                        String returnType = extractJsonValue(method, "returnType");
                        String visibility = extractJsonValue(method, "visibility");
                        boolean isStatic = method.contains("\"isStatic\": true");
                        boolean isAbstract = method.contains("\"isAbstract\": true");
                        
                        if (name != null && returnType != null) {
                            plantUml.append("    ");
                            
                            // Add visibility symbol
                            if ("private".equals(visibility)) plantUml.append("-");
                            else if ("protected".equals(visibility)) plantUml.append("#");
                            else if ("public".equals(visibility)) plantUml.append("+");
                            else plantUml.append("~");
                            
                            plantUml.append(name).append("()");
                            plantUml.append(" : ").append(returnType);
                            
                            if (isStatic) plantUml.append(" {static}");
                            if (isAbstract) plantUml.append(" {abstract}");
                            
                            plantUml.append("\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error parsing methods from raw analysis: {}", e.getMessage());
        }
    }
    
    /**
     * Extract JSON section by key
     */
    private String extractJsonSection(String json, String sectionName) {
        try {
            String pattern = "\"" + sectionName + "\"\\s*:\\s*\\[([^\\]]+)\\]";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(json);
            
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            logger.warn("Error extracting JSON section {}: {}", sectionName, e.getMessage());
        }
        return null;
    }
    
    /**
     * Generate class relationships from analysis results
     */
    private void generateClassRelationshipsFromAnalysis(StringBuilder plantUml, 
                                                       Map<String, GeminiClassAnalyzerService.ClassAnalysisResult> analysisResults) {
        plantUml.append("\n' Class Relationships\n");
        
        for (GeminiClassAnalyzerService.ClassAnalysisResult analysis : analysisResults.values()) {
            if (analysis == null || !"SUCCESS".equals(analysis.getAnalysisStatus())) continue;
            
            String className = analysis.getClassName();
            String rawAnalysis = analysis.getRawAnalysis();
            
            if (rawAnalysis != null) {
                // Extract inheritance relationships
                String extendsClass = extractJsonValue(rawAnalysis, "extends");
                if (extendsClass != null && !extendsClass.equals("null") && analysisResults.containsKey(extendsClass)) {
                    plantUml.append(extendsClass).append(" <|-- ").append(className).append("\n");
                }
                
                // Extract usage relationships from usedClasses
                String usedClassesSection = extractJsonSection(rawAnalysis, "usedClasses");
                if (usedClassesSection != null) {
                    String[] usedClasses = usedClassesSection.split(",");
                    for (String usedClass : usedClasses) {
                        String cleanUsedClass = usedClass.replaceAll("[\"\\s]", "");
                        if (analysisResults.containsKey(cleanUsedClass)) {
                            plantUml.append(className).append(" --> ").append(cleanUsedClass).append(" : uses\n");
                        }
                    }
                }
            }
        }
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
}