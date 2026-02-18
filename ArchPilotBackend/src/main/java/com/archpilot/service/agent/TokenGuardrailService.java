package com.archpilot.service.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * TokenGuardrailService - Acts as a guardrail for AI Agent requests
 * 
 * Main Context:
 * - Implements dual Token Bucket strategy for both RPM and TPM rate limiting
 * - Manages user token consumption and request count with separate refill rates
 * - Prevents API rate limit violations for Gemini 2.5 models
 * - Calculates wait times when either RPM or TPM limits are exceeded
 * 
 * Rate Limits (Free Tier 2026):
 * - Gemini 2.5 Flash: ~10 RPM, 250,000 TPM
 * - Gemini 2.5 Flash-Lite: ~15 RPM
 * 
 * Dual Bucket Strategy:
 * - RPM Bucket: Tracks request count, refills at 1 request per (60/maxRPM) seconds
 * - TPM Bucket: Tracks token usage, refills at maxTPM/60 tokens per second
 */
@Service
public class TokenGuardrailService {

    private static final Logger logger = LoggerFactory.getLogger(TokenGuardrailService.class);

    @Value("${gemini.rate-limit.max-tokens-per-minute:250000}")
    public int maxTokensPerMinute;

    @Value("${gemini.rate-limit.max-requests-per-minute:10}")
    public int maxRequestsPerMinute;

    @Value("${gemini.rate-limit.tokens-refill-per-second:4167}")  // 250000/60 tokens per second
    private int tokensRefillPerSecond;

    @Value("${gemini.rate-limit.requests-refill-per-second:0.167}")  // 10/60 requests per second
    private double requestsRefillPerSecond;

    /**
     * Validates if a request can proceed based on both RPM and TPM bucket states
     * 
     * @param userId User identifier for rate limiting
     * @param estimatedTokens Estimated tokens for the request
     * @return TokenValidationResult containing approval status and wait time for both limits
     */
    public TokenValidationResult validateRequest(String userId, int estimatedTokens) {
        // TODO: Implement dual bucket validation logic
        // 1. Get current user RPM and TPM bucket states from database
        // 2. Calculate refill amounts for both buckets based on time elapsed
        // 3. Check if request can be fulfilled against BOTH limits
        // 4. Return validation result with maximum wait time if either limit exceeded
        
        logger.info("Validating request for user: {} with estimated tokens: {}", userId, estimatedTokens);
        
        // Placeholder implementation - replace with actual dual bucket logic
        // Need to check both:
        // - Available requests in RPM bucket >= 1
        // - Available tokens in TPM bucket >= estimatedTokens
        
        return new TokenValidationResult(true, 0, 0, maxRequestsPerMinute, maxTokensPerMinute);
    }

    /**
     * Updates both request count and token consumption after successful API call
     * 
     * @param userId User identifier
     * @param actualTokensUsed Actual tokens consumed from Gemini API response
     */
    public void updateConsumption(String userId, int actualTokensUsed) {
        // TODO: Implement dual consumption update
        // 1. Subtract 1 from RPM bucket (request count)
        // 2. Subtract actual tokens from TPM bucket
        // 3. Update last request timestamp for both buckets
        // 4. Store in database atomically
        
        logger.info("Updating consumption for user: {} - 1 request, {} tokens", userId, actualTokensUsed);
    }

    /**
     * Calculates wait time in seconds when either rate limit is exceeded
     * 
     * @param requiredTokens Tokens needed for the request
     * @param availableTokens Current available tokens in TPM bucket
     * @param availableRequests Current available requests in RPM bucket
     * @return Wait time in seconds (maximum of RPM or TPM wait time)
     */
    public long calculateWaitTime(int requiredTokens, int availableTokens, double availableRequests) {
        long tokenWaitTime = 0;
        long requestWaitTime = 0;
        
        // Calculate wait time for TPM limit
        if (availableTokens < requiredTokens) {
            int tokensNeeded = requiredTokens - availableTokens;
            tokenWaitTime = (long) Math.ceil((double) tokensNeeded / tokensRefillPerSecond);
        }
        
        // Calculate wait time for RPM limit
        if (availableRequests < 1.0) {
            double requestsNeeded = 1.0 - availableRequests;
            requestWaitTime = (long) Math.ceil(requestsNeeded / requestsRefillPerSecond);
        }
        
        // Return the maximum wait time (most restrictive limit)
        return Math.max(tokenWaitTime, requestWaitTime);
    }

    /**
     * Result class for dual bucket token validation (RPM + TPM)
     */
    public static class TokenValidationResult {
        private final boolean approved;
        private final long waitTimeSeconds;
        private final long rpmWaitTime;
        private final int remainingRequests;
        private final int remainingTokens;

        public TokenValidationResult(boolean approved, long waitTimeSeconds, long rpmWaitTime, 
                                   int remainingRequests, int remainingTokens) {
            this.approved = approved;
            this.waitTimeSeconds = waitTimeSeconds;
            this.rpmWaitTime = rpmWaitTime;
            this.remainingRequests = remainingRequests;
            this.remainingTokens = remainingTokens;
        }

        public boolean isApproved() { return approved; }
        public long getWaitTimeSeconds() { return waitTimeSeconds; }
        public long getRpmWaitTime() { return rpmWaitTime; }
        public int getRemainingRequests() { return remainingRequests; }
        public int getRemainingTokens() { return remainingTokens; }
        
        public String getLimitingFactor() {
            if (waitTimeSeconds == rpmWaitTime) {
                return "RPM";
            } else {
                return "TPM";
            }
        }
    }
}