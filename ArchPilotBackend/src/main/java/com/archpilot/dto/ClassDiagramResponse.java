package com.archpilot.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for class diagram generation with PNG output
 */
public class ClassDiagramResponse {
    private String status;
    private String message;
    private ClassDiagramData data;
    private String timestamp;

    public ClassDiagramResponse() {
        this.timestamp = LocalDateTime.now().toString();
    }

    public static ClassDiagramResponse success(String message, ClassDiagramData data) {
        ClassDiagramResponse response = new ClassDiagramResponse();
        response.status = "SUCCESS";
        response.message = message;
        response.data = data;
        return response;
    }

    public static ClassDiagramResponse error(String message) {
        ClassDiagramResponse response = new ClassDiagramResponse();
        response.status = "ERROR";
        response.message = message;
        response.data = null;
        return response;
    }

    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ClassDiagramData getData() { return data; }
    public void setData(ClassDiagramData data) { this.data = data; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    /**
     * Inner class for the data payload
     */
    public static class ClassDiagramData {
        private String mainPngFileName;
        private int classCount;
        private String mainPngBase64;
        private String repositoryName;
        private String litePngFileName;
        private String timestamp;

        public ClassDiagramData() {}

        public ClassDiagramData(String mainPngFileName, int classCount, String mainPngBase64, 
                               String repositoryName, String litePngFileName, String timestamp) {
            this.mainPngFileName = mainPngFileName;
            this.classCount = classCount;
            this.mainPngBase64 = mainPngBase64;
            this.repositoryName = repositoryName;
            this.litePngFileName = litePngFileName;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getMainPngFileName() { return mainPngFileName; }
        public void setMainPngFileName(String mainPngFileName) { this.mainPngFileName = mainPngFileName; }

        public int getClassCount() { return classCount; }
        public void setClassCount(int classCount) { this.classCount = classCount; }

        public String getMainPngBase64() { return mainPngBase64; }
        public void setMainPngBase64(String mainPngBase64) { this.mainPngBase64 = mainPngBase64; }

        public String getRepositoryName() { return repositoryName; }
        public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }

        public String getLitePngFileName() { return litePngFileName; }
        public void setLitePngFileName(String litePngFileName) { this.litePngFileName = litePngFileName; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}