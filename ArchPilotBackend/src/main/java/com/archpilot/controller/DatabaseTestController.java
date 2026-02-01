package com.archpilot.controller;

import com.archpilot.entity.TestData;
import com.archpilot.repository.TestDataRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Database Test", description = "Endpoints for testing database connectivity and operations")
public class DatabaseTestController {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private TestDataRepository testDataRepository;
    
    @GetMapping("/db-connection")
    @Operation(summary = "Test database connection", description = "Tests the database connection and returns connection details")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            response.put("status", "SUCCESS");
            response.put("connected", true);
            response.put("database", connection.getCatalog());
            response.put("url", connection.getMetaData().getURL());
            response.put("driver", connection.getMetaData().getDriverName());
            response.put("timestamp", LocalDateTime.now());
            
            // Test a simple query
            Long recordCount = testDataRepository.countAllRecords();
            response.put("testTableRecordCount", recordCount);
            
        } catch (SQLException e) {
            response.put("status", "ERROR");
            response.put("connected", false);
            response.put("error", e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/db-data")
    @Operation(summary = "Get test data", description = "Retrieves all test data from the database")
    public ResponseEntity<List<TestData>> getTestData() {
        try {
            List<TestData> data = testDataRepository.findAllOrderByCreatedAtDesc();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping("/db-data")
    @Operation(summary = "Insert test data", description = "Inserts a new test record into the database")
    public ResponseEntity<TestData> insertTestData(@RequestBody TestDataRequest request) {
        try {
            TestData testData = new TestData(request.getName(), request.getDescription());
            TestData saved = testDataRepository.save(testData);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping("/db-data/sample")
    @Operation(summary = "Insert sample data", description = "Inserts predefined sample data for testing")
    public ResponseEntity<Map<String, Object>> insertSampleData() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Insert sample records
            TestData sample1 = new TestData("Database Connection Test", "This record confirms PostgreSQL connectivity is working");
            TestData sample2 = new TestData("JPA Integration Test", "This record confirms Spring Data JPA is properly configured");
            TestData sample3 = new TestData("CRUD Operations Test", "This record confirms basic CRUD operations are functional");
            
            testDataRepository.save(sample1);
            testDataRepository.save(sample2);
            testDataRepository.save(sample3);
            
            response.put("status", "SUCCESS");
            response.put("message", "Sample data inserted successfully");
            response.put("recordsInserted", 3);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to insert sample data");
            response.put("error", e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @DeleteMapping("/db-data")
    @Operation(summary = "Clear test data", description = "Deletes all test data from the database")
    public ResponseEntity<Map<String, Object>> clearTestData() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long count = testDataRepository.count();
            testDataRepository.deleteAll();
            
            response.put("status", "SUCCESS");
            response.put("message", "All test data cleared");
            response.put("recordsDeleted", count);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to clear test data");
            response.put("error", e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // Request DTO
    public static class TestDataRequest {
        private String name;
        private String description;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
}