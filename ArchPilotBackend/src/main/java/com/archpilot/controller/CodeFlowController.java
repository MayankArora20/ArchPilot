package com.archpilot.controller;

import com.archpilot.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/flow")
@CrossOrigin(origins = "*")
public class CodeFlowController {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeFlowController.class);
    
    @GetMapping("/diagram/{projectName}/{fileName}")
    public ResponseEntity<Resource> getDiagramFile(
            @PathVariable String projectName,
            @PathVariable String fileName) {
        try {
            logger.info("Serving diagram file: {} for project: {}", fileName, projectName);
            
            Path filePath = Paths.get("ArchpilotResource", projectName, fileName);
            
            if (!Files.exists(filePath)) {
                logger.warn("Diagram file not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(filePath);
            
            // Determine content type based on file extension
            String contentType = determineContentType(fileName);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"");
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
                
        } catch (Exception e) {
            logger.error("Error serving diagram file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/png/{projectName}/{fileName}")
    public ResponseEntity<Resource> getPngDiagram(
            @PathVariable String projectName,
            @PathVariable String fileName) {
        try {
            logger.info("Serving PNG diagram: {} for project: {}", fileName, projectName);
            
            // Ensure fileName ends with .png
            String pngFileName = fileName.endsWith(".png") ? fileName : fileName + ".png";
            Path pngFilePath = Paths.get("ArchpilotResource", projectName, pngFileName);
            
            if (!Files.exists(pngFilePath)) {
                logger.warn("PNG diagram file not found: {}", pngFilePath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(pngFilePath);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pngFileName + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "public, max-age=3600"); // Cache for 1 hour
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
                
        } catch (Exception e) {
            logger.error("Error serving PNG diagram: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/diagram/{projectName}/{fileName}/content")
    public ResponseEntity<ApiResponse<String>> getDiagramContent(
            @PathVariable String projectName,
            @PathVariable String fileName) {
        try {
            logger.info("Getting diagram content: {} for project: {}", fileName, projectName);
            
            Path filePath = Paths.get("ArchpilotResource", projectName, fileName);
            
            if (!Files.exists(filePath)) {
                logger.warn("Diagram file not found: {}", filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Diagram file not found: " + fileName));
            }
            
            String content = Files.readString(filePath);
            
            return ResponseEntity.ok(ApiResponse.success(content));
                
        } catch (Exception e) {
            logger.error("Error reading diagram content: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to read diagram content: " + e.getMessage()));
        }
    }
    
    @GetMapping("/diagrams/{projectName}")
    public ResponseEntity<ApiResponse<String[]>> listDiagrams(@PathVariable String projectName) {
        try {
            logger.info("Listing diagrams for project: {}", projectName);
            
            Path projectPath = Paths.get("ArchpilotResource", projectName);
            
            if (!Files.exists(projectPath)) {
                logger.warn("Project directory not found: {}", projectPath);
                return ResponseEntity.ok(ApiResponse.success(new String[0]));
            }
            
            String[] files = Files.list(projectPath)
                .filter(path -> path.toString().endsWith(".puml") || path.toString().endsWith(".png"))
                .map(path -> path.getFileName().toString())
                .toArray(String[]::new);
            
            return ResponseEntity.ok(ApiResponse.success(files));
                
        } catch (Exception e) {
            logger.error("Error listing diagrams: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to list diagrams: " + e.getMessage()));
        }
    }
    
    private String determineContentType(String fileName) {
        if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".puml")) {
            return "text/plain";
        } else if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        } else {
            return "application/octet-stream";
        }
    }
}