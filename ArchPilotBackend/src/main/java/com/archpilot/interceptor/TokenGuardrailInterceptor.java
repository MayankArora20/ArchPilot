package com.archpilot.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.archpilot.service.agent.TokenGuardrailService;
import com.archpilot.service.agent.TokenGuardrailService.TokenValidationResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * TokenGuardrailInterceptor - Intercepts AI agent requests for rate limiting
 * 
 * Main Context:
 * - Implements interceptor pattern as per requirements
 * - Pre-validates requests against token bucket limits
 * - Returns 429 Too Many Requests with retry-after header when limits exceeded
 * - Extracts user identity from request for rate limiting
 */
@Component
public class TokenGuardrailInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TokenGuardrailInterceptor.class);

    @Autowired
    private TokenGuardrailService tokenGuardrailService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only intercept AI agent endpoints
        String requestURI = request.getRequestURI();
        if (!isAIAgentEndpoint(requestURI)) {
            return true; // Allow non-AI requests to proceed
        }

        logger.info("Intercepting AI agent request: {}", requestURI);

        // Extract user identity (could be from API key, session, or IP)
        String userId = extractUserIdentity(request);
        
        // Estimate token usage based on request content
        int estimatedTokens = estimateTokenUsage(request);

        // Validate against rate limits
        TokenValidationResult validation = tokenGuardrailService.validateRequest(userId, estimatedTokens);

        if (!validation.isApproved()) {
            // Rate limit exceeded - return 429 with retry-after header
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-Retry-After", String.valueOf(validation.getWaitTimeSeconds()));
            response.setHeader("X-RateLimit-Limit-RPM", String.valueOf(tokenGuardrailService.maxRequestsPerMinute));
            response.setHeader("X-RateLimit-Limit-TPM", String.valueOf(tokenGuardrailService.maxTokensPerMinute));
            response.setHeader("X-RateLimit-Remaining-Requests", String.valueOf(validation.getRemainingRequests()));
            response.setHeader("X-RateLimit-Remaining-Tokens", String.valueOf(validation.getRemainingTokens()));
            response.setHeader("Content-Type", "application/json");
            
            String errorResponse = String.format(
                "{\"error\":\"Rate limit exceeded\",\"limitingFactor\":\"%s\",\"retryAfterSeconds\":%d,\"remainingRequests\":%d,\"remainingTokens\":%d}",
                validation.getLimitingFactor(),
                validation.getWaitTimeSeconds(),
                validation.getRemainingRequests(),
                validation.getRemainingTokens()
            );
            
            response.getWriter().write(errorResponse);
            response.getWriter().flush();
            
            logger.warn("Rate limit exceeded for user: {}. Limiting factor: {}, Wait time: {} seconds", 
                       userId, validation.getLimitingFactor(), validation.getWaitTimeSeconds());
            return false; // Block the request
        }

        // Store user ID and estimated tokens for post-processing
        request.setAttribute("userId", userId);
        request.setAttribute("estimatedTokens", estimatedTokens);
        
        logger.info("Request approved for user: {} with {} estimated tokens", userId, estimatedTokens);
        return true; // Allow request to proceed
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Update actual token consumption after AI response
        String userId = (String) request.getAttribute("userId");
        if (userId != null && isAIAgentEndpoint(request.getRequestURI())) {
            // TODO: Extract actual token usage from AI response
            // For now, use estimated tokens
            Integer estimatedTokens = (Integer) request.getAttribute("estimatedTokens");
            if (estimatedTokens != null) {
                tokenGuardrailService.updateConsumption(userId, estimatedTokens);
            }
        }
    }

    /**
     * Checks if the request is for an AI agent endpoint
     */
    private boolean isAIAgentEndpoint(String requestURI) {
        return requestURI.contains("/api/agent/") || 
               requestURI.contains("/api/chat/") ||
               requestURI.contains("/api/uml/");
    }

    /**
     * Extracts user identity from request
     * Priority: API Key > Session ID > IP Address
     */
    private String extractUserIdentity(HttpServletRequest request) {
        // Check for API key in header
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return "api:" + apiKey;
        }

        // Check for session ID
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : null;
        if (sessionId != null) {
            return "session:" + sessionId;
        }

        // Fallback to IP address
        String clientIP = getClientIP(request);
        return "ip:" + clientIP;
    }

    /**
     * Gets the real client IP address considering proxies
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Estimates token usage based on request content
     * Rough estimation: 1 token ≈ 4 characters for English text
     */
    private int estimateTokenUsage(HttpServletRequest request) {
        try {
            // Get content length if available
            int contentLength = request.getContentLength();
            if (contentLength > 0) {
                // Rough estimation: content length / 4 + some overhead for response
                return (contentLength / 4) + 1000; // Add 1000 tokens overhead for response
            }
            
            // Default estimation for requests without content
            return 2000; // Conservative estimate
        } catch (Exception e) {
            logger.warn("Error estimating token usage: {}", e.getMessage());
            return 5000; // Conservative fallback
        }
    }
}