package com.archpilot.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for JIRA ticket creation response
 */
public class JiraTicketResponse {
    private String ticketId;
    private String heading;
    private String description;
    private Integer storyPoints;
    private List<String> classesToConsider;
    private List<String> methodsToConsider;
    private List<String> designPatterns;
    private List<String> unitTestCases;
    private String successCriteria;
    private String priority;
    private String ticketType;
    private LocalDateTime createdAt;

    public JiraTicketResponse() {
        this.createdAt = LocalDateTime.now();
    }

    public JiraTicketResponse(String ticketId, String heading, String description, Integer storyPoints) {
        this();
        this.ticketId = ticketId;
        this.heading = heading;
        this.description = description;
        this.storyPoints = storyPoints;
    }

    // Getters and setters
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getStoryPoints() { return storyPoints; }
    public void setStoryPoints(Integer storyPoints) { this.storyPoints = storyPoints; }

    public List<String> getClassesToConsider() { return classesToConsider; }
    public void setClassesToConsider(List<String> classesToConsider) { this.classesToConsider = classesToConsider; }

    public List<String> getMethodsToConsider() { return methodsToConsider; }
    public void setMethodsToConsider(List<String> methodsToConsider) { this.methodsToConsider = methodsToConsider; }

    public List<String> getDesignPatterns() { return designPatterns; }
    public void setDesignPatterns(List<String> designPatterns) { this.designPatterns = designPatterns; }

    public List<String> getUnitTestCases() { return unitTestCases; }
    public void setUnitTestCases(List<String> unitTestCases) { this.unitTestCases = unitTestCases; }

    public String getSuccessCriteria() { return successCriteria; }
    public void setSuccessCriteria(String successCriteria) { this.successCriteria = successCriteria; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getTicketType() { return ticketType; }
    public void setTicketType(String ticketType) { this.ticketType = ticketType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}