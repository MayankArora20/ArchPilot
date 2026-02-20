package com.archpilot.service.agent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.archpilot.service.ClassDiagramGeneratorService.JavaClassInfo;

/**
 * Gemini Class Analyzer Agent Service
 * 
 * Main Context:
 * - Specialized AI agent for analyzing Java class files and extracting structural information
 * - Uses Google's Gemini AI to understand class relationships, methods, fields, and dependencies
 * - Performs parallel analysis of multiple Java classes for enhanced UML diagram generation
 * - Extracts class-level metadata including inheritance, interfaces, annotations, and usage patterns
 * - Provides structured analysis results for PlantUML diagram enhancement
 * 
 * Token Guardrail Protection:
 * - This agent is automatically protected by the TokenGuardrailInterceptor
 * - Uses a controlled thread pool (2 threads) to manage API rate limits
 * - Token consumption is tracked and managed automatically through the guardrail system
 * - Respects the dual bucket strategy (RPM + TPM) for Gemini API calls
 * 
 * Analysis Capabilities:
 * - Class structure analysis (fields, methods, constructors)
 * - Inheritance and interface relationship detection
 * - Cross-class dependency identification
 * - Annotation and modifier extraction
 * - Package and import analysis
 * - Design pattern recognition support
 * 
 * Performance Features:
 * - Parallel processing with configurable thread pool
 * - Timeout management for AI API calls
 * - Fallback mechanisms for failed analyses
 * - Concurrent result collection and aggregation
 */
@Service
public class GeminiClassAnalyzerAgentService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClassAnalyzerAgentService.class);
    private final ChatClient chatClient;
    private final ExecutorService executorService;

    public GeminiClassAnalyzerAgentService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        // Using 2 threads as per TokenGuardrail strategy to manage API rate limits
        this.executorService = Executors.newFixedThreadPool(2);
    }

    /**
     * Analyzes Java class files in parallel and extracts class-level information
     * for enhanced PlantUML diagram generation
     */
    public Map<String, ClassAnalysisResult> analyzeJavaClasses(List<JavaClassInfo> javaClasses, 
                                                              Map<String, String> fileContents) {
        logger.info("Starting parallel analysis of {} Java classes with 2 threads", javaClasses.size());
        
        Map<String, ClassAnalysisResult> results = new java.util.concurrent.ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
        
        for (JavaClassInfo javaClass : javaClasses) {
            String content = fileContents.get(javaClass.getClassName());
            if (content != null && !content.trim().isEmpty()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        ClassAnalysisResult result = analyzeClassContent(javaClass.getClassName(), content);
                        results.put(javaClass.getClassName(), result);
                    } catch (Exception e) {
                        logger.error("Error analyzing class {}: {}", javaClass.getClassName(), e.getMessage(), e);
                    }
                }, executorService);
                futures.add(future);
            }
        }
        
        // Wait for all analysis to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(60, TimeUnit.SECONDS); // Increased timeout for AI processing
            logger.info("All Java classes analyzed successfully");
        } catch (Exception e) {
            logger.error("Error during parallel class analysis: {}", e.getMessage(), e);
        }
        
        return results;
    }

    /**
     * Analyzes a single Java class file content using Gemini AI
     */
    private ClassAnalysisResult analyzeClassContent(String className, String content) {
        String prompt = buildAnalysisPrompt(className, content);
        
        try {
            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            
            return parseAnalysisResponse(className, response);
        } catch (Exception e) {
            logger.error("Error calling Gemini API for class {}: {}", className, e.getMessage(), e);
            return createFallbackResult(className);
        }
    }

    /**
     * Builds the analysis prompt for Gemini AI
     */
    private String buildAnalysisPrompt(String className, String content) {
        return String.format("""
            Analyze the following Java class and extract ONLY the following information in a structured format:
            
            Class Name: %s
            
            Java Code:
            ```java
            %s
            ```
            
            Please provide the analysis in this exact JSON format:
            {
                "className": "%s",
                "classType": "class|interface|enum|abstract class",
                "packageName": "extracted package name",
                "imports": ["list of imported classes that are used in the class"],
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
            
            Focus ONLY on:
            1. Class-level variables (fields)
            2. Method signatures (not implementation details)
            3. Other class objects that this class uses or references
            4. Inheritance and interface relationships
            5. Class-level annotations
            
            Do NOT include:
            - Method implementations
            - Local variables inside methods
            - Comments
            - Import statements that are not actually used
            """, className, content, className);
    }

    /**
     * Parses the Gemini AI response into a structured result
     */
    private ClassAnalysisResult parseAnalysisResponse(String className, String response) {
        try {
            // Extract JSON from response (Gemini might include extra text)
            String jsonPart = extractJsonFromResponse(response);
            
            // Parse JSON using a simple approach (you might want to use Jackson for production)
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

    private ClassAnalysisResult parseJsonToClassAnalysisResult(String json) {
        // For now, create a simple parser. In production, use Jackson ObjectMapper
        ClassAnalysisResult result = new ClassAnalysisResult();
        
        // This is a simplified parser - you should use Jackson ObjectMapper for production
        try {
            // Extract basic information using simple string parsing
            result.setClassName(extractJsonValue(json, "className"));
            result.setClassType(extractJsonValue(json, "classType"));
            result.setPackageName(extractJsonValue(json, "packageName"));
            result.setExtendsClass(extractJsonValue(json, "extends"));
            
            // For arrays and complex objects, you'd need more sophisticated parsing
            result.setRawAnalysis(json); // Store raw JSON for now
            
        } catch (Exception e) {
            logger.error("Error parsing JSON: {}", e.getMessage());
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

    private ClassAnalysisResult createFallbackResult(String className) {
        ClassAnalysisResult result = new ClassAnalysisResult();
        result.setClassName(className);
        result.setClassType("class");
        result.setAnalysisStatus("FAILED");
        return result;
    }

    /**
     * Cleanup method for ExecutorService
     */
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

    /**
     * Result class for storing class analysis information
     */
    public static class ClassAnalysisResult {
        private String className;
        private String classType;
        private String packageName;
        private String extendsClass;
        private List<String> implementsInterfaces;
        private List<FieldInfo> fields;
        private List<MethodInfo> methods;
        private List<String> usedClasses;
        private List<String> annotations;
        private String rawAnalysis; // Store raw JSON response
        private String analysisStatus = "SUCCESS";

        // Getters and setters
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public String getClassType() { return classType; }
        public void setClassType(String classType) { this.classType = classType; }

        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }

        public String getExtendsClass() { return extendsClass; }
        public void setExtendsClass(String extendsClass) { this.extendsClass = extendsClass; }

        public List<String> getImplementsInterfaces() { return implementsInterfaces; }
        public void setImplementsInterfaces(List<String> implementsInterfaces) { this.implementsInterfaces = implementsInterfaces; }

        public List<FieldInfo> getFields() { return fields; }
        public void setFields(List<FieldInfo> fields) { this.fields = fields; }

        public List<MethodInfo> getMethods() { return methods; }
        public void setMethods(List<MethodInfo> methods) { this.methods = methods; }

        public List<String> getUsedClasses() { return usedClasses; }
        public void setUsedClasses(List<String> usedClasses) { this.usedClasses = usedClasses; }

        public List<String> getAnnotations() { return annotations; }
        public void setAnnotations(List<String> annotations) { this.annotations = annotations; }

        public String getRawAnalysis() { return rawAnalysis; }
        public void setRawAnalysis(String rawAnalysis) { this.rawAnalysis = rawAnalysis; }

        public String getAnalysisStatus() { return analysisStatus; }
        public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }
    }

    public static class FieldInfo {
        private String name;
        private String type;
        private String visibility;
        private boolean isStatic;
        private boolean isFinal;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getVisibility() { return visibility; }
        public void setVisibility(String visibility) { this.visibility = visibility; }

        public boolean isStatic() { return isStatic; }
        public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

        public boolean isFinal() { return isFinal; }
        public void setFinal(boolean isFinal) { this.isFinal = isFinal; }
    }

    public static class MethodInfo {
        private String name;
        private String returnType;
        private String visibility;
        private boolean isStatic;
        private boolean isAbstract;
        private List<ParameterInfo> parameters;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getReturnType() { return returnType; }
        public void setReturnType(String returnType) { this.returnType = returnType; }

        public String getVisibility() { return visibility; }
        public void setVisibility(String visibility) { this.visibility = visibility; }

        public boolean isStatic() { return isStatic; }
        public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

        public boolean isAbstract() { return isAbstract; }
        public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }

        public List<ParameterInfo> getParameters() { return parameters; }
        public void setParameters(List<ParameterInfo> parameters) { this.parameters = parameters; }
    }

    public static class ParameterInfo {
        private String name;
        private String type;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}