package com.archpilot.service;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Service
public class PlantUmlToPngService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlantUmlToPngService.class);
    
    public byte[] convertToPng(String plantUmlContent) throws IOException {
        logger.info("Converting PlantUML content to PNG");
        
        try {
            SourceStringReader reader = new SourceStringReader(plantUmlContent);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Generate PNG
            reader.outputImage(outputStream, new FileFormatOption(FileFormat.PNG));
            
            byte[] pngBytes = outputStream.toByteArray();
            logger.info("Successfully converted PlantUML to PNG, size: {} bytes", pngBytes.length);
            
            return pngBytes;
            
        } catch (Exception e) {
            logger.error("Error converting PlantUML to PNG: {}", e.getMessage(), e);
            throw new IOException("Failed to convert PlantUML to PNG: " + e.getMessage(), e);
        }
    }
    
    public String convertToPngBase64(String plantUmlContent) throws IOException {
        byte[] pngBytes = convertToPng(plantUmlContent);
        return Base64.getEncoder().encodeToString(pngBytes);
    }
    
    public void savePngToFile(String plantUmlContent, String fileName, String directory) throws IOException {
        byte[] pngBytes = convertToPng(plantUmlContent);
        
        // Create directory if it doesn't exist
        Path dirPath = Paths.get(directory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        // Save PNG file
        Path pngFilePath = dirPath.resolve(fileName);
        Files.write(pngFilePath, pngBytes);
        
        logger.info("PNG file saved: {}", pngFilePath.toString());
    }
}