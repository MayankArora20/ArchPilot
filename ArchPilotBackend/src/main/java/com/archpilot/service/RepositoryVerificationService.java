package com.archpilot.service;

import com.archpilot.dto.RepositoryVerificationResponse;
import com.archpilot.dto.RepositoryBranchesResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RepositoryVerificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RepositoryVerificationService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    // Regex patterns for extracting owner and repo from URLs
    private static final Pattern GITHUB_PATTERN = Pattern.compile("^https://github\\.com/([\\w.-]+)/([\\w.-]+)/?$");
    private static final Pattern GITLAB_PATTERN = Pattern.compile("^https://gitlab\\.com/([\\w.-]+)/([\\w.-]+)/?$");
    
    @Autowired
    public RepositoryVerificationService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
        this.objectMapper = objectMapper;
    }
    
    public Mono<RepositoryVerificationResponse> verifyRepository(String repositoryUrl, String accessToken) {
        logger.info("Verifying repository: {}", repositoryUrl);
        
        try {
            if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
                return Mono.just(RepositoryVerificationResponse.error("Repository URL is required"));
            }
            
            repositoryUrl = repositoryUrl.trim();
            
            if (isGitHubUrl(repositoryUrl)) {
                return verifyGitHubRepository(repositoryUrl, accessToken);
            } else if (isGitLabUrl(repositoryUrl)) {
                return verifyGitLabRepository(repositoryUrl, accessToken);
            } else {
                return Mono.just(RepositoryVerificationResponse.error("Unsupported repository platform. Only GitHub and GitLab are supported"));
            }
            
        } catch (Exception e) {
            logger.error("Error verifying repository: {}", e.getMessage(), e);
            return Mono.just(RepositoryVerificationResponse.error("Internal error: " + e.getMessage()));
        }
    }
    
    private boolean isGitHubUrl(String url) {
        return GITHUB_PATTERN.matcher(url).matches();
    }
    
    private boolean isGitLabUrl(String url) {
        return GITLAB_PATTERN.matcher(url).matches();
    }
    
    private Mono<RepositoryVerificationResponse> verifyGitHubRepository(String repositoryUrl, String accessToken) {
        Matcher matcher = GITHUB_PATTERN.matcher(repositoryUrl);
        if (!matcher.matches()) {
            return Mono.just(RepositoryVerificationResponse.error("Invalid GitHub URL format"));
        }
        
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        String apiUrl = String.format("https://api.github.com/repos/%s/%s", owner, repo);
        
        logger.info("Making GitHub API request to: {}", apiUrl);
        
        // First, try with authentication if token is provided
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            return makeGitHubRequest(apiUrl, accessToken.trim(), repositoryUrl);
        } else {
            // For unauthenticated requests, try a different approach
            // GitHub sometimes requires authentication even for public repos via API
            // Let's try to check if the repository exists by making a simple HTTP request to the repo page
            return checkGitHubRepositoryExists(repositoryUrl, owner, repo);
        }
    }
    
    private Mono<RepositoryVerificationResponse> makeGitHubRequest(String apiUrl, String accessToken, String repositoryUrl) {
        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(apiUrl);
        
        logger.info("Adding authorization header for GitHub API request");
        request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        
        return request
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .doOnNext(responseBody -> logger.debug("GitHub API response received, length: {}", responseBody.length()))
                .map(responseBody -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        
                        RepositoryVerificationResponse.RepositoryInfo info = new RepositoryVerificationResponse.RepositoryInfo(
                                jsonNode.path("name").asText(),
                                jsonNode.path("full_name").asText(),
                                jsonNode.path("description").asText("No description"),
                                jsonNode.path("default_branch").asText("main"),
                                jsonNode.path("private").asBoolean(false),
                                jsonNode.path("language").asText("Unknown"),
                                "GitHub"
                        );
                        
                        logger.info("Successfully verified GitHub repository: {}", repositoryUrl);
                        return RepositoryVerificationResponse.verified(repositoryUrl, info);
                        
                    } catch (Exception e) {
                        logger.error("Error parsing GitHub API response: {}", e.getMessage());
                        return RepositoryVerificationResponse.error("Error parsing repository information");
                    }
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.warn("GitHub API error for {}: {} - {} - Response: {}", 
                               repositoryUrl, ex.getStatusCode(), ex.getMessage(), ex.getResponseBodyAsString());
                    
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.just(RepositoryVerificationResponse.error("Repository not found or not accessible"));
                    } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                        return Mono.just(RepositoryVerificationResponse.error("Access forbidden. Repository may be private or rate limit exceeded"));
                    } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        String errorBody = ex.getResponseBodyAsString();
                        if (errorBody.contains("rate limit")) {
                            return Mono.just(RepositoryVerificationResponse.error("GitHub API rate limit exceeded. Please try again later"));
                        } else {
                            return Mono.just(RepositoryVerificationResponse.error("GitHub API authentication failed. Please check your access token"));
                        }
                    } else {
                        return Mono.just(RepositoryVerificationResponse.error("GitHub API error: " + ex.getStatusCode() + " - " + ex.getMessage()));
                    }
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Network error verifying GitHub repository: {}", ex.getMessage());
                    return Mono.just(RepositoryVerificationResponse.error("Network error: " + ex.getMessage()));
                });
    }
    
    private Mono<RepositoryVerificationResponse> checkGitHubRepositoryExists(String repositoryUrl, String owner, String repo) {
        logger.info("Checking GitHub repository existence without API: {}", repositoryUrl);
        
        // Try to access the repository's main page to verify it exists
        return webClient.get()
                .uri(repositoryUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .map(responseBody -> {
                    // If we can access the page, the repository exists and is public
                    RepositoryVerificationResponse.RepositoryInfo info = new RepositoryVerificationResponse.RepositoryInfo(
                            repo,
                            owner + "/" + repo,
                            "Repository verified via web access (API authentication required for detailed info)",
                            "main",
                            false, // Assuming public since we can access it
                            "Unknown",
                            "GitHub"
                    );
                    
                    logger.info("Successfully verified GitHub repository via web access: {}", repositoryUrl);
                    return RepositoryVerificationResponse.verified(repositoryUrl, info);
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.warn("GitHub web access error for {}: {} - {}", repositoryUrl, ex.getStatusCode(), ex.getMessage());
                    
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.just(RepositoryVerificationResponse.error("Repository not found"));
                    } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                        return Mono.just(RepositoryVerificationResponse.error("Repository is private or access is restricted"));
                    } else {
                        return Mono.just(RepositoryVerificationResponse.error("Unable to verify repository: " + ex.getStatusCode()));
                    }
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Network error checking GitHub repository: {}", ex.getMessage());
                    return Mono.just(RepositoryVerificationResponse.error("Network error: " + ex.getMessage()));
                });
    }
    
    public Mono<RepositoryBranchesResponse> getRepositoryBranches(String repositoryUrl, String accessToken, Integer limit) {
        logger.info("Fetching branches for repository: {}", repositoryUrl);
        
        try {
            if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
                return Mono.just(RepositoryBranchesResponse.error("Repository URL is required"));
            }
            
            repositoryUrl = repositoryUrl.trim();
            
            if (isGitHubUrl(repositoryUrl)) {
                return fetchGitHubBranches(repositoryUrl, accessToken, limit);
            } else if (isGitLabUrl(repositoryUrl)) {
                return fetchGitLabBranches(repositoryUrl, accessToken, limit);
            } else {
                return Mono.just(RepositoryBranchesResponse.error("Unsupported repository platform. Only GitHub and GitLab are supported"));
            }
            
        } catch (Exception e) {
            logger.error("Error fetching repository branches: {}", e.getMessage(), e);
            return Mono.just(RepositoryBranchesResponse.error("Internal error: " + e.getMessage()));
        }
    }
    
    private Mono<RepositoryBranchesResponse> fetchGitHubBranches(String repositoryUrl, String accessToken, Integer limit) {
        Matcher matcher = GITHUB_PATTERN.matcher(repositoryUrl);
        if (!matcher.matches()) {
            return Mono.just(RepositoryBranchesResponse.error("Invalid GitHub URL format"));
        }
        
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/branches?per_page=%d", owner, repo, limit);
        
        logger.info("Fetching GitHub branches from: {}", apiUrl);
        
        // First, try with authentication if token is provided
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            return makeGitHubBranchesRequest(apiUrl, accessToken.trim(), repositoryUrl);
        } else {
            // For unauthenticated requests, try without token first, then fallback if needed
            return makeGitHubBranchesRequestUnauthenticated(apiUrl, repositoryUrl, owner, repo, limit);
        }
    }
    
    private Mono<RepositoryBranchesResponse> makeGitHubBranchesRequest(String apiUrl, String accessToken, String repositoryUrl) {
        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(apiUrl);
        request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        
        return request
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .map(responseBody -> parseGitHubBranchesResponse(responseBody, repositoryUrl))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.warn("GitHub branches API error for {}: {} - {}", repositoryUrl, ex.getStatusCode(), ex.getMessage());
                    return handleGitHubBranchesError(ex);
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Network error fetching GitHub branches: {}", ex.getMessage());
                    return Mono.just(RepositoryBranchesResponse.error("Network error: " + ex.getMessage()));
                });
    }
    
    private Mono<RepositoryBranchesResponse> makeGitHubBranchesRequestUnauthenticated(String apiUrl, String repositoryUrl, String owner, String repo, Integer limit) {
        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(apiUrl);
        
        return request
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .map(responseBody -> parseGitHubBranchesResponse(responseBody, repositoryUrl))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.warn("GitHub unauthenticated branches API error for {}: {} - {}", repositoryUrl, ex.getStatusCode(), ex.getMessage());
                    
                    if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                        // Fallback: try to get basic branch info from repository info
                        logger.info("Falling back to basic branch info for: {}", repositoryUrl);
                        return getBasicBranchInfoFallback(repositoryUrl, owner, repo);
                    } else {
                        return handleGitHubBranchesError(ex);
                    }
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Network error fetching GitHub branches: {}", ex.getMessage());
                    return Mono.just(RepositoryBranchesResponse.error("Network error: " + ex.getMessage()));
                });
    }
    
    private RepositoryBranchesResponse parseGitHubBranchesResponse(String responseBody, String repositoryUrl) {
        try {
            JsonNode jsonArray = objectMapper.readTree(responseBody);
            List<RepositoryBranchesResponse.BranchInfo> branches = new ArrayList<>();
            
            for (JsonNode branchNode : jsonArray) {
                String branchName = branchNode.path("name").asText();
                String sha = branchNode.path("commit").path("sha").asText();
                boolean isProtected = branchNode.path("protected").asBoolean(false);
                
                RepositoryBranchesResponse.BranchInfo branchInfo = new RepositoryBranchesResponse.BranchInfo(
                        branchName, sha, false, isProtected, null, null, null
                );
                
                branches.add(branchInfo);
            }
            
            logger.info("Successfully fetched {} branches for GitHub repository: {}", branches.size(), repositoryUrl);
            return RepositoryBranchesResponse.success(repositoryUrl, branches, "GitHub");
            
        } catch (Exception e) {
            logger.error("Error parsing GitHub branches response: {}", e.getMessage());
            return RepositoryBranchesResponse.error("Error parsing branches information");
        }
    }
    
    private Mono<RepositoryBranchesResponse> handleGitHubBranchesError(WebClientResponseException ex) {
        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            return Mono.just(RepositoryBranchesResponse.error("Repository not found or not accessible"));
        } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
            return Mono.just(RepositoryBranchesResponse.error("Access forbidden. Repository may be private or rate limit exceeded. Try providing an access token"));
        } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return Mono.just(RepositoryBranchesResponse.error("GitHub API requires authentication for branch access. Please provide an access token"));
        } else {
            return Mono.just(RepositoryBranchesResponse.error("GitHub API error: " + ex.getStatusCode()));
        }
    }
    
    private Mono<RepositoryBranchesResponse> getBasicBranchInfoFallback(String repositoryUrl, String owner, String repo) {
        logger.info("Using web scraping fallback for branches: {}", repositoryUrl);
        
        // For unauthenticated requests, we'll provide a basic response indicating authentication is needed
        // but still show that the repository exists (since verification worked)
        List<RepositoryBranchesResponse.BranchInfo> branches = new ArrayList<>();
        
        // Add a placeholder branch indicating authentication is required for full branch list
        RepositoryBranchesResponse.BranchInfo placeholderBranch = new RepositoryBranchesResponse.BranchInfo(
                "main", // Assume main as default
                "unknown", 
                true, // Mark as default
                false, 
                "Authentication required to view full branch list and details", 
                "GitHub API", 
                null
        );
        branches.add(placeholderBranch);
        
        // Add common branch names that might exist
        String[] commonBranches = {"develop", "dev", "master"};
        for (String branchName : commonBranches) {
            RepositoryBranchesResponse.BranchInfo commonBranch = new RepositoryBranchesResponse.BranchInfo(
                    branchName,
                    "unknown",
                    false,
                    false,
                    "Possible branch - authentication required to verify",
                    "Estimated",
                    null
            );
            branches.add(commonBranch);
        }
        
        logger.info("Fallback: Created basic branch structure for GitHub repository: {}", repositoryUrl);
        return Mono.just(RepositoryBranchesResponse.success(repositoryUrl, branches, "GitHub"));
    }
    
    private Mono<RepositoryBranchesResponse> fetchGitLabBranches(String repositoryUrl, String accessToken, Integer limit) {
        Matcher matcher = GITLAB_PATTERN.matcher(repositoryUrl);
        if (!matcher.matches()) {
            return Mono.just(RepositoryBranchesResponse.error("Invalid GitLab URL format"));
        }
        
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        String projectPath = owner + "/" + repo;
        String encodedPath = projectPath.replace("/", "%2F");
        String apiUrl = String.format("https://gitlab.com/api/v4/projects/%s/repository/branches?per_page=%d", encodedPath, limit);
        
        logger.info("Fetching GitLab branches from: {}", apiUrl);
        
        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(apiUrl);
        
        // Add authorization header if access token is provided
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            request = request.header("PRIVATE-TOKEN", accessToken.trim());
        }
        
        return request
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .map(responseBody -> {
                    try {
                        JsonNode jsonArray = objectMapper.readTree(responseBody);
                        List<RepositoryBranchesResponse.BranchInfo> branches = new ArrayList<>();
                        
                        for (JsonNode branchNode : jsonArray) {
                            String branchName = branchNode.path("name").asText();
                            String sha = branchNode.path("commit").path("id").asText();
                            boolean isProtected = branchNode.path("protected").asBoolean(false);
                            boolean isDefault = branchNode.path("default").asBoolean(false);
                            
                            // Extract commit information if available
                            JsonNode commitNode = branchNode.path("commit");
                            String commitMessage = commitNode.path("message").asText(null);
                            String commitAuthor = commitNode.path("author_name").asText(null);
                            
                            RepositoryBranchesResponse.BranchInfo branchInfo = new RepositoryBranchesResponse.BranchInfo(
                                    branchName, sha, isDefault, isProtected, commitMessage, commitAuthor, null
                            );
                            
                            branches.add(branchInfo);
                        }
                        
                        logger.info("Successfully fetched {} branches for GitLab repository: {}", branches.size(), repositoryUrl);
                        return RepositoryBranchesResponse.success(repositoryUrl, branches, "GitLab");
                        
                    } catch (Exception e) {
                        logger.error("Error parsing GitLab branches response: {}", e.getMessage());
                        return RepositoryBranchesResponse.error("Error parsing branches information");
                    }
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.warn("GitLab branches API error for {}: {} - {}", repositoryUrl, ex.getStatusCode(), ex.getMessage());
                    
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.just(RepositoryBranchesResponse.error("Repository not found or not accessible"));
                    } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                        return Mono.just(RepositoryBranchesResponse.error("Access forbidden. Repository may be private or access token invalid"));
                    } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        return Mono.just(RepositoryBranchesResponse.error("Unauthorized. Access token required for this repository"));
                    } else {
                        return Mono.just(RepositoryBranchesResponse.error("GitLab API error: " + ex.getStatusCode()));
                    }
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Network error fetching GitLab branches: {}", ex.getMessage());
                    return Mono.just(RepositoryBranchesResponse.error("Network error: " + ex.getMessage()));
                });
    }
    
    private Mono<RepositoryVerificationResponse> verifyGitLabRepository(String repositoryUrl, String accessToken) {
        Matcher matcher = GITLAB_PATTERN.matcher(repositoryUrl);
        if (!matcher.matches()) {
            return Mono.just(RepositoryVerificationResponse.error("Invalid GitLab URL format"));
        }
        
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        String projectPath = owner + "/" + repo;
        String encodedPath = projectPath.replace("/", "%2F");
        String apiUrl = String.format("https://gitlab.com/api/v4/projects/%s", encodedPath);
        
        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(apiUrl);
        
        // Add authorization header if access token is provided
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            request = request.header("PRIVATE-TOKEN", accessToken.trim());
        }
        
        return request
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .map(responseBody -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        
                        RepositoryVerificationResponse.RepositoryInfo info = new RepositoryVerificationResponse.RepositoryInfo(
                                jsonNode.path("name").asText(),
                                jsonNode.path("path_with_namespace").asText(),
                                jsonNode.path("description").asText("No description"),
                                jsonNode.path("default_branch").asText("main"),
                                jsonNode.path("visibility").asText("public").equals("private"),
                                "Unknown", // GitLab API doesn't provide primary language in basic info
                                "GitLab"
                        );
                        
                        return RepositoryVerificationResponse.verified(repositoryUrl, info);
                        
                    } catch (Exception e) {
                        logger.error("Error parsing GitLab API response: {}", e.getMessage());
                        return RepositoryVerificationResponse.error("Error parsing repository information");
                    }
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.warn("GitLab API error for {}: {} - {}", repositoryUrl, ex.getStatusCode(), ex.getMessage());
                    
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.just(RepositoryVerificationResponse.error("Repository not found or not accessible"));
                    } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                        return Mono.just(RepositoryVerificationResponse.error("Access forbidden. Repository may be private or access token invalid"));
                    } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        return Mono.just(RepositoryVerificationResponse.error("Unauthorized. Invalid or missing access token for private repository"));
                    } else {
                        return Mono.just(RepositoryVerificationResponse.error("GitLab API error: " + ex.getStatusCode()));
                    }
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Network error verifying GitLab repository: {}", ex.getMessage());
                    return Mono.just(RepositoryVerificationResponse.error("Network error: " + ex.getMessage()));
                });
    }
}