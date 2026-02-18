package com.archpilot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to track dual bucket rate limiting state (RPM + TPM) per user
 */
@Entity
@Table(name = "rate_limit_buckets")
public class RateLimitBucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "available_requests", nullable = false)
    private Double availableRequests;

    @Column(name = "available_tokens", nullable = false)
    private Integer availableTokens;

    @Column(name = "last_request_time", nullable = false)
    private LocalDateTime lastRequestTime;

    @Column(name = "last_token_time", nullable = false)
    private LocalDateTime lastTokenTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public RateLimitBucket() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public RateLimitBucket(String userId, Double maxRequests, Integer maxTokens) {
        this();
        this.userId = userId;
        this.availableRequests = maxRequests;
        this.availableTokens = maxTokens;
        this.lastRequestTime = LocalDateTime.now();
        this.lastTokenTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Double getAvailableRequests() { return availableRequests; }
    public void setAvailableRequests(Double availableRequests) { this.availableRequests = availableRequests; }

    public Integer getAvailableTokens() { return availableTokens; }
    public void setAvailableTokens(Integer availableTokens) { this.availableTokens = availableTokens; }

    public LocalDateTime getLastRequestTime() { return lastRequestTime; }
    public void setLastRequestTime(LocalDateTime lastRequestTime) { this.lastRequestTime = lastRequestTime; }

    public LocalDateTime getLastTokenTime() { return lastTokenTime; }
    public void setLastTokenTime(LocalDateTime lastTokenTime) { this.lastTokenTime = lastTokenTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}