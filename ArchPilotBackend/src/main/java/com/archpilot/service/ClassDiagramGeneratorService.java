package com.archpilot.service;

import java.io.FileWriter;
import java.io.IOException;
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

import com.archpilot.model.RepositoryTreeData;
import com.archpilot.model.TreeNode;
import com.archpilot.service.agent.GeminiAgentService;
import com.archpilot.service.agent.GeminiClassAnalyzerService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ClassDiagramGeneratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(ClassDiagramGeneratorService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private GeminiClassAnalyzerService geminiClassAnalyzerService;
    
    @Autowired
    private GeminiAgentService geminiAgentService;
    
    @Autowired
    private PlantUmlToPngService plantUmlToPngService;
    
    public Map<String, Object> generateClassDiagram(RepositoryTreeData treeData) {
        logger.info("Generating enhanced class diagram for repository: {}", treeData.getRepositoryUrl());
        
        try {
            // Use the commit SHA from the tree data as the project SHA
            String projectSha = treeData.getCommitSha();
            String repositoryName = extractRepositoryName(treeData.getRepositoryUrl());
            
            if (projectSha == null || projectSha.isEmpty()) {
                logger.warn("No commit SHA available, proceeding without caching");
                projectSha = "no-sha-" + System.currentTimeMillis();
            } else {
                logger.info("Using commit SHA as project SHA: {} for repository: {}", projectSha, repositoryName);
                
                // Check if we have a cached diagram for this exact project state
                Map<String, Object> cachedDiagram = checkForCachedDiagram(repositoryName, projectSha);
                if (cachedDiagram != null) {
                    logger.info("Found existing diagram for project SHA: {}, returning cached result", projectSha);
                    return cachedDiagram;
                }
                
                logger.info("No cached diagram found for project SHA: {}, generating new diagram", projectSha);
            }
            
            // Make projectSha effectively final for lambda
            final String finalProjectSha = projectSha;
            
            // Run the blocking operations in a separate thread to avoid reactive context issues
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return generateClassDiagramBlocking(treeData, finalProjectSha);
                } catch (Exception e) {
                    logger.error("Error in async class diagram generation: {}", e.getMessage(), e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Failed to generate enhanced class diagram: " + e.getMessage());
                    return errorResponse;
                }
            }).get(); // This will block but in a separate thread
            
        } catch (Exception e) {
            logger.error("Error generating enhanced class diagram: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate enhanced class diagram: " + e.getMessage());
            return errorResponse;
        }
    }
    
    private Map<String, Object> generateClassDiagramBlocking(RepositoryTreeData treeData, String projectSha) {
        // Extract Java classes from tree data
        List<JavaClassInfo> javaClasses = extractJavaClasses(treeData);
        logger.info("Found {} Java classes to analyze", javaClasses.size());
        
        // Generate basic PlantUML structure first
        String basicPlantUml = generateBasicPlantUML(javaClasses, treeData);
        logger.info("Generated basic PlantUML structure");
        
        // Fetch file contents and analyze sequentially (no executor service)
        Map<String, GeminiClassAnalyzerService.ClassAnalysisResult> analysisResults = 
            analyzeClassesSequentially(javaClasses, treeData, basicPlantUml);
        logger.info("Completed sequential analysis for {} classes", analysisResults.size());
        
        // Generate enhanced PlantUML diagram with analysis results
        String enhancedPlantUml = generateEnhancedPlantUMLFromAnalysis(basicPlantUml, analysisResults, treeData);
        
        // Generate enhanced JSON representation
        Map<String, Object> jsonData = generateEnhancedJsonRepresentation(javaClasses, analysisResults, treeData);
        
        // Add project SHA to JSON data for caching
        jsonData.put("projectSha", projectSha);
        
        // Create umlDigr directory and save files
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String repositoryName = extractRepositoryName(treeData.getRepositoryUrl());
        
        // Generate PNG from PlantUML
        String pngBase64 = null;
        try {
            pngBase64 = plantUmlToPngService.convertToPngBase64(enhancedPlantUml);
            logger.info("Successfully generated PNG from PlantUML");
        } catch (Exception e) {
            logger.error("Error generating PNG from PlantUML: {}", e.getMessage(), e);
        }
        
        try {
            saveToFiles(enhancedPlantUml, jsonData, repositoryName, timestamp);
            
            // Also save PNG file if generation was successful
            if (pngBase64 != null) {
                String pngFileName = String.format("%s_%s.png", repositoryName, timestamp);
                plantUmlToPngService.savePngToFile(enhancedPlantUml, pngFileName, "umlDigr");
            }
        } catch (IOException e) {
            logger.error("Error saving files: {}", e.getMessage(), e);
        }
        
        // Return response
        Map<String, Object> response = new HashMap<>();
        response.put("plantUmlGenerated", true);
        response.put("jsonGenerated", true);
        response.put("pngGenerated", pngBase64 != null);
        response.put("classCount", javaClasses.size());
        response.put("analyzedCount", analysisResults.size());
        response.put("timestamp", timestamp);
        response.put("repositoryName", repositoryName);
        response.put("plantUmlContent", enhancedPlantUml);
        response.put("jsonData", jsonData);
        response.put("analysisResults", analysisResults);
        response.put("projectSha", projectSha);
        response.put("cached", false);
        
        // Include PNG data in response
        if (pngBase64 != null) {
            response.put("pngBase64", pngBase64);
        }
        
        return response;
    }
    
    private List<JavaClassInfo> extractJavaClasses(RepositoryTreeData treeData) {
        List<JavaClassInfo> javaClasses = new ArrayList<>();
        
        if (treeData.getTree() != null) {
            extractJavaClassesFromNodes(treeData.getTree(), javaClasses);
        }
        
        return javaClasses;
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
    
    private GeminiClassAnalyzerService.ClassAnalysisResult analyzeClassWithContext(
            String className, String content, String existingUml) {
        
        logger.info("Starting Gemini analysis for class: {}", className);
        
        String prompt = String.format("""
            I have a basic PlantUML class diagram that I want to enhance. Here's the current UML:
            
            ```plantuml
            %s
            ```
            
            Now I want to analyze this Java class and extract detailed information to enhance the diagram:
            
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
                "usedClasses": ["list of other classes this class references"],
                "annotations": ["list of class-level annotations"]
            }
            
            Focus on extracting:
            1. All fields with their types and visibility
            2. All method signatures with parameters and return types
            3. Class relationships (extends, implements)
            4. Other classes that this class uses or references
            """, existingUml, className, content, className);
        
        try {
            logger.info("Sending prompt to Gemini for class: {}", className);
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
    
    private String extractJsonFromResponse(String response) {
        // Find JSON block in the response
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");
        
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }
        
        return response; // Return as-is if no clear JSON block found
    }
    
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
    
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }
    
    private GeminiClassAnalyzerService.ClassAnalysisResult createFallbackResult(String className) {
        GeminiClassAnalyzerService.ClassAnalysisResult result = new GeminiClassAnalyzerService.ClassAnalysisResult();
        result.setClassName(className);
        result.setClassType("class");
        result.setAnalysisStatus("FAILED");
        return result;
    }
    
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
    
    private String generateEnhancedPlantUML(List<JavaClassInfo> javaClasses, 
                                           Map<String, GeminiClassAnalyzerService.ClassAnalysisResult> analysisResults,
                                           RepositoryTreeData treeData) {
        StringBuilder plantUml = new StringBuilder();
        
        plantUml.append("@startuml\n");
        plantUml.append("!theme plain\n");
        plantUml.append("title Enhanced Class Diagram - ").append(extractRepositoryName(treeData.getRepositoryUrl())).append("\n\n");
        
        // Group classes by package
        Map<String, List<JavaClassInfo>> packageGroups = new HashMap<>();
        for (JavaClassInfo classInfo : javaClasses) {
            String packageName = classInfo.getPackageName();
            if (packageName.isEmpty()) {
                packageName = "default";
            }
            packageGroups.computeIfAbsent(packageName, k -> new ArrayList<>()).add(classInfo);
        }
        
        // Generate PlantUML for each package with enhanced details
        for (Map.Entry<String, List<JavaClassInfo>> entry : packageGroups.entrySet()) {
            String packageName = entry.getKey();
            List<JavaClassInfo> classes = entry.getValue();
            
            if (!"default".equals(packageName)) {
                plantUml.append("package \"").append(packageName).append("\" {\n");
            }
            
            for (JavaClassInfo classInfo : classes) {
                GeminiClassAnalyzerService.ClassAnalysisResult analysis = analysisResults.get(classInfo.getClassName());
                
                if (analysis != null && "SUCCESS".equals(analysis.getAnalysisStatus())) {
                    generateEnhancedClassDefinition(plantUml, classInfo, analysis);
                } else {
                    // Fallback to basic class definition
                    generateBasicClassDefinition(plantUml, classInfo);
                }
            }
            
            if (!"default".equals(packageName)) {
                plantUml.append("}\n\n");
            }
        }
        
        // Generate relationships between classes
        generateClassRelationships(plantUml, analysisResults);
        
        plantUml.append("@enduml\n");
        
        return plantUml.toString();
    }
    
    private void generateEnhancedClassDefinition(StringBuilder plantUml, JavaClassInfo classInfo, 
                                               GeminiClassAnalyzerService.ClassAnalysisResult analysis) {
        String classType = analysis.getClassType() != null ? analysis.getClassType() : "class";
        
        if ("interface".equals(classType)) {
            plantUml.append("  interface ").append(classInfo.getClassName()).append(" {\n");
        } else if ("abstract class".equals(classType)) {
            plantUml.append("  abstract class ").append(classInfo.getClassName()).append(" {\n");
        } else if ("enum".equals(classType)) {
            plantUml.append("  enum ").append(classInfo.getClassName()).append(" {\n");
        } else {
            plantUml.append("  class ").append(classInfo.getClassName()).append(" {\n");
        }
        
        // Add fields from analysis
        if (analysis.getFields() != null) {
            for (GeminiClassAnalyzerService.FieldInfo field : analysis.getFields()) {
                plantUml.append("    ");
                if ("private".equals(field.getVisibility())) plantUml.append("-");
                else if ("protected".equals(field.getVisibility())) plantUml.append("#");
                else if ("public".equals(field.getVisibility())) plantUml.append("+");
                else plantUml.append("~");
                
                plantUml.append(field.getName()).append(" : ").append(field.getType());
                if (field.isStatic()) plantUml.append(" {static}");
                if (field.isFinal()) plantUml.append(" {final}");
                plantUml.append("\n");
            }
        }
        
        // Add separator between fields and methods
        if (analysis.getFields() != null && !analysis.getFields().isEmpty() && 
            analysis.getMethods() != null && !analysis.getMethods().isEmpty()) {
            plantUml.append("    --\n");
        }
        
        // Add methods from analysis
        if (analysis.getMethods() != null) {
            for (GeminiClassAnalyzerService.MethodInfo method : analysis.getMethods()) {
                plantUml.append("    ");
                if ("private".equals(method.getVisibility())) plantUml.append("-");
                else if ("protected".equals(method.getVisibility())) plantUml.append("#");
                else if ("public".equals(method.getVisibility())) plantUml.append("+");
                else plantUml.append("~");
                
                plantUml.append(method.getName()).append("(");
                
                // Add parameters
                if (method.getParameters() != null) {
                    for (int i = 0; i < method.getParameters().size(); i++) {
                        if (i > 0) plantUml.append(", ");
                        GeminiClassAnalyzerService.ParameterInfo param = method.getParameters().get(i);
                        plantUml.append(param.getName()).append(": ").append(param.getType());
                    }
                }
                
                plantUml.append(") : ").append(method.getReturnType());
                if (method.isStatic()) plantUml.append(" {static}");
                if (method.isAbstract()) plantUml.append(" {abstract}");
                plantUml.append("\n");
            }
        }
        
        // Add metadata as comments
        plantUml.append("    ' SHA: ").append(classInfo.getSha()).append("\n");
        plantUml.append("    ' Path: ").append(classInfo.getFullPath()).append("\n");
        plantUml.append("  }\n");
    }
    
    private void generateBasicClassDefinition(StringBuilder plantUml, JavaClassInfo classInfo) {
        plantUml.append("  class ").append(classInfo.getClassName()).append(" {\n");
        plantUml.append("    ' SHA: ").append(classInfo.getSha()).append("\n");
        plantUml.append("    ' Path: ").append(classInfo.getFullPath()).append("\n");
        plantUml.append("    ' Analysis: FAILED - Using basic definition\n");
        plantUml.append("  }\n");
    }
    
    private void generateClassRelationships(StringBuilder plantUml, 
                                          Map<String, GeminiClassAnalyzerService.ClassAnalysisResult> analysisResults) {
        plantUml.append("\n' Class Relationships\n");
        
        for (GeminiClassAnalyzerService.ClassAnalysisResult analysis : analysisResults.values()) {
            if (analysis == null || !"SUCCESS".equals(analysis.getAnalysisStatus())) continue;
            
            String className = analysis.getClassName();
            
            // Inheritance relationships
            if (analysis.getExtendsClass() != null && !analysis.getExtendsClass().isEmpty()) {
                plantUml.append(analysis.getExtendsClass()).append(" <|-- ").append(className).append("\n");
            }
            
            // Interface implementations
            if (analysis.getImplementsInterfaces() != null) {
                for (String interfaceName : analysis.getImplementsInterfaces()) {
                    plantUml.append(interfaceName).append(" <|.. ").append(className).append("\n");
                }
            }
            
            // Usage relationships (based on used classes)
            if (analysis.getUsedClasses() != null) {
                for (String usedClass : analysis.getUsedClasses()) {
                    // Only show relationships to classes that are in our analysis
                    if (analysisResults.containsKey(usedClass)) {
                        plantUml.append(className).append(" --> ").append(usedClass).append(" : uses\n");
                    }
                }
            }
        }
    }
    
    private Map<String, Object> generateEnhancedJsonRepresentation(List<JavaClassInfo> javaClasses, 
                                                                 Map<String, GeminiClassAnalyzerService.ClassAnalysisResult> analysisResults,
                                                                 RepositoryTreeData treeData) {
        Map<String, Object> jsonData = new HashMap<>();
        
        jsonData.put("repositoryUrl", treeData.getRepositoryUrl());
        jsonData.put("branch", treeData.getBranch());
        jsonData.put("platform", treeData.getPlatform());
        jsonData.put("generatedAt", LocalDateTime.now().toString());
        jsonData.put("totalClasses", javaClasses.size());
        jsonData.put("analyzedClasses", analysisResults.size());
        jsonData.put("enhancedAnalysis", true);
        jsonData.put("pngGenerated", true);  // Indicate PNG was generated
        
        // Group by packages with enhanced analysis
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
            
            // Add enhanced analysis data if available
            GeminiClassAnalyzerService.ClassAnalysisResult analysis = analysisResults.get(classInfo.getClassName());
            if (analysis != null) {
                classData.put("analysisStatus", analysis.getAnalysisStatus());
                classData.put("classType", analysis.getClassType());
                classData.put("extendsClass", analysis.getExtendsClass());
                classData.put("implementsInterfaces", analysis.getImplementsInterfaces());
                classData.put("fields", analysis.getFields());
                classData.put("methods", analysis.getMethods());
                classData.put("usedClasses", analysis.getUsedClasses());
                classData.put("annotations", analysis.getAnnotations());
                classData.put("rawAnalysis", analysis.getRawAnalysis());
            } else {
                classData.put("analysisStatus", "NOT_ANALYZED");
            }
            
            packages.computeIfAbsent(packageName, k -> new ArrayList<>()).add(classData);
        }
        
        jsonData.put("packages", packages);
        
        // Add relationship summary
        Map<String, Object> relationships = new HashMap<>();
        int inheritanceCount = 0;
        int implementationCount = 0;
        int usageCount = 0;
        
        for (GeminiClassAnalyzerService.ClassAnalysisResult analysis : analysisResults.values()) {
            if (analysis != null && "SUCCESS".equals(analysis.getAnalysisStatus())) {
                if (analysis.getExtendsClass() != null && !analysis.getExtendsClass().isEmpty()) {
                    inheritanceCount++;
                }
                if (analysis.getImplementsInterfaces() != null) {
                    implementationCount += analysis.getImplementsInterfaces().size();
                }
                if (analysis.getUsedClasses() != null) {
                    usageCount += analysis.getUsedClasses().size();
                }
            }
        }
        
        relationships.put("inheritanceRelationships", inheritanceCount);
        relationships.put("interfaceImplementations", implementationCount);
        relationships.put("usageRelationships", usageCount);
        jsonData.put("relationshipSummary", relationships);
        
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
    
    /**
     * Check for cached diagram by looking at existing JSON files in umlDigr directory
     */
    private Map<String, Object> checkForCachedDiagram(String repositoryName, String projectSha) {
        try {
            Path umlDigrPath = Paths.get("umlDigr");
            if (!Files.exists(umlDigrPath)) {
                return null;
            }
            
            // Look for JSON files matching the repository name pattern
            return Files.list(umlDigrPath)
                .filter(path -> path.toString().endsWith(".json"))
                .filter(path -> path.getFileName().toString().startsWith(repositoryName + "_"))
                .map(jsonFile -> {
                    try {
                        // Read and parse the JSON file
                        String jsonContent = Files.readString(jsonFile);
                        Map<String, Object> jsonData = objectMapper.readValue(jsonContent, Map.class);
                        
                        // Check if the project SHA matches
                        String cachedSha = (String) jsonData.get("projectSha");
                        if (projectSha.equals(cachedSha)) {
                            logger.info("Found matching cached diagram: {} with SHA: {}", jsonFile.getFileName(), cachedSha);
                            
                            // Also try to read the corresponding PlantUML file
                            String pumlFileName = jsonFile.getFileName().toString().replace(".json", ".puml");
                            Path pumlFile = umlDigrPath.resolve(pumlFileName);
                            
                            if (Files.exists(pumlFile)) {
                                String plantUmlContent = Files.readString(pumlFile);
                                jsonData.put("plantUmlContent", plantUmlContent);
                                
                                // Generate PNG from cached PlantUML content
                                try {
                                    String pngBase64 = plantUmlToPngService.convertToPngBase64(plantUmlContent);
                                    jsonData.put("pngBase64", pngBase64);
                                    jsonData.put("pngGenerated", true);
                                    logger.info("Generated PNG from cached PlantUML content");
                                } catch (Exception e) {
                                    logger.warn("Error generating PNG from cached PlantUML: {}", e.getMessage());
                                    jsonData.put("pngGenerated", false);
                                }
                            }
                            
                            // Mark as cached and return
                            jsonData.put("cached", true);
                            jsonData.put("cachedFile", jsonFile.getFileName().toString());
                            
                            return jsonData;
                        }
                    } catch (Exception e) {
                        logger.warn("Error reading cached file {}: {}", jsonFile.getFileName(), e.getMessage());
                    }
                    return null;
                })
                .filter(result -> result != null)
                .findFirst()
                .orElse(null);
                
        } catch (Exception e) {
            logger.error("Error checking for cached diagram: {}", e.getMessage());
            return null;
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