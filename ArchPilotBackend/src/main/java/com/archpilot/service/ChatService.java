package com.archpilot.service;

import com.archpilot.model.ChatMessage;
import com.archpilot.model.ChatSession;
import com.archpilot.service.agent.JavaArchitectAgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private JavaArchitectAgentService javaArchitectAgentService;
    
    public ChatSession initializeSession(String projectName) throws IOException {
        logger.info("Initializing chat session for project: {}", projectName);
        
        // Check if project files exist in umlDigr directory
        Path umlDigrPath = Paths.get("umlDigr");
        
        // Find the latest .puml and .json files for the project
        String umlContent = findAndReadUmlFile(umlDigrPath, projectName);
        Map<String, Object> jsonData = findAndReadJsonFile(umlDigrPath, projectName);
        
        if (umlContent == null || jsonData == null) {
            throw new IllegalArgumentException("Project not found: " + projectName);
        }
        
        ChatSession session = new ChatSession(projectName, umlContent, jsonData);
        
        // Add welcome message
        ChatMessage welcomeMessage = new ChatMessage(
            "Session initialized for project: " + projectName, 
            "assistant"
        );
        session.addMessage(welcomeMessage);
        
        logger.info("Chat session initialized successfully for project: {}", projectName);
        return session;
    }
    
    public String processMessage(ChatSession session, String userMessage) {
        logger.info("Processing message for project: {}", session.getProjectName());
        
        // Add user message to session
        ChatMessage userMsg = new ChatMessage(userMessage, "user");
        session.addMessage(userMsg);
        
        // Process message through Java Architect Agent
        String response = javaArchitectAgentService.processArchitectRequest(session, userMessage);
        
        // Add assistant response to session
        ChatMessage assistantMsg = new ChatMessage(response, "assistant");
        session.addMessage(assistantMsg);
        
        session.updateActivity();
        
        return response;
    }
    
    private String findAndReadUmlFile(Path umlDigrPath, String projectName) throws IOException {
        if (!Files.exists(umlDigrPath)) {
            return null;
        }
        
        // Look for files that start with the project name and end with .puml
        File[] umlFiles = umlDigrPath.toFile().listFiles((dir, name) -> 
            name.startsWith(projectName) && name.endsWith(".puml") && !name.contains("_lite"));
        
        if (umlFiles == null || umlFiles.length == 0) {
            return null;
        }
        
        // Get the most recent file (assuming timestamp in filename)
        File latestUmlFile = umlFiles[0];
        for (File file : umlFiles) {
            if (file.lastModified() > latestUmlFile.lastModified()) {
                latestUmlFile = file;
            }
        }
        
        logger.info("Found UML file: {}", latestUmlFile.getName());
        return Files.readString(latestUmlFile.toPath());
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> findAndReadJsonFile(Path umlDigrPath, String projectName) throws IOException {
        if (!Files.exists(umlDigrPath)) {
            return null;
        }
        
        // Look for files that start with the project name and end with .json
        File[] jsonFiles = umlDigrPath.toFile().listFiles((dir, name) -> 
            name.startsWith(projectName) && name.endsWith(".json"));
        
        if (jsonFiles == null || jsonFiles.length == 0) {
            return null;
        }
        
        // Get the most recent file (assuming timestamp in filename)
        File latestJsonFile = jsonFiles[0];
        for (File file : jsonFiles) {
            if (file.lastModified() > latestJsonFile.lastModified()) {
                latestJsonFile = file;
            }
        }
        
        logger.info("Found JSON file: {}", latestJsonFile.getName());
        return objectMapper.readValue(latestJsonFile, Map.class);
    }
}