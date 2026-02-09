package com.archpilot.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.archpilot.dto.RepositoryBranchesResponse;
import com.archpilot.dto.RepositoryTreeResponse;
import com.archpilot.dto.RepositoryVerificationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    
    public Mono<RepositoryTreeResponse> getRepositoryTree(String repositoryUrl, String accessToken, 
                                                         String branch, String path, Boolean recursive) {
        logger.info("Fetching tree structure for repository: {}, branch: {}, path: {}", repositoryUrl, branch, path);
        
        try {
            if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
                return Mono.just(RepositoryTreeResponse.error("Repository URL is required"));
            }
            
            repositoryUrl = repositoryUrl.trim();
            
            if (isGitHubUrl(repositoryUrl)) {
                return fetchGitHubTree(repositoryUrl, accessToken, branch, path, recursive);
            } else if (isGitLabUrl(repositoryUrl)) {
                return fetchGitLabTree(repositoryUrl, accessToken, branch, path, recursive);
            } else {
                return Mono.just(RepositoryTreeResponse.error("Unsupported repository platform. Only GitHub and GitLab are supported"));
            }
            
        } catch (Exception e) {
            logger.error("Error fetching repository tree: {}", e.getMessage(), e);
            return Mono.just(RepositoryTreeResponse.error("Internal error: " + e.getMessage()));
        }
    }
    
    private Mono<RepositoryTreeResponse> fetchGitHubTree(String repositoryUrl, String accessToken, 
                                                        String branch, String path, Boolean recursive) {
        Matcher matcher = GITHUB_PATTERN.matcher(repositoryUrl);
        if (!matcher.matches()) {
            return Mono.just(RepositoryTreeResponse.error("Invalid GitHub URL format"));
        }
        
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        
        // If branch is not specified, get the default branch first
        if (branch == null || branch.trim().isEmpty()) {
            return getDefaultBranch(repositoryUrl, accessToken, owner, repo, "GitHub")
                    .flatMap(defaultBranch -> fetchGitHubTreeWithBranch(repositoryUrl, accessToken, owner, repo, defaultBranch, path, recursive));
        } else {
            return fetchGitHubTreeWithBranch(repositoryUrl, accessToken, owner, repo, branch, path, recursive);
        }
    }
    
    private Mono<String> getDefaultBranch(String repositoryUrl, String accessToken, String owner, String repo, String platform) {
        String apiUrl = platform.equals("GitHub") 
            ? String.format("https://api.github.com/repos/%s/%s", owner, repo)
            : String.format("https://gitlab.com/api/v4/projects/%s", (owner + "/" + repo).replace("/", "%2F"));
        
        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(apiUrl);
        
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            if (platform.equals("GitHub")) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.trim());
            } else {
                request = request.header("PRIVATE-TOKEN", accessToken.trim());
            }
        }
        
        return request
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .map(responseBody -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        String defaultBranch = jsonNode.path("default_branch").asText("main");
                        logger.info("Retrieved default branch: {} for repository: {}", defaultBranch, repositoryUrl);
                        return defaultBranch;
                    } catch (Exception e) {
                        logger.warn("Error parsing default branch, using 'main': {}", e.getMessage());
                        return "main";
                    }
                })
                .onErrorResume(ex -> {
                    logger.warn("Error getting default branch for {}, trying common branch names: {}", repositoryUrl, ex.getMessage());
                    // Try common branch names as fallback
                    return Mono.just("master"); // Most older repos use master
                });
    }
    
    private Mono<RepositoryTreeResponse> fetchGitHubTreeWithBranch(String repositoryUrl, String accessToken, 
                                                                  String owner, String repo, String branch, 
                                                                  String path, Boolean recursive) {
        // Use GitHub Contents API for directory listing
        String apiPath = (path == null || path.trim().isEmpty()) ? "" : "/" + path.trim();
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents%s?ref=%s", owner, repo, apiPath, branch);
        
        logger.info("Fetching GitHub tree from: {}", apiUrl);
        
        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(apiUrl);
        
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.trim());
        }
        
        return request
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .flatMap(responseBody -> parseGitHubTreeResponse(responseBody, repositoryUrl, branch, path, recursive, accessToken, owner, repo))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.warn("GitHub tree API error for {}: {} - {}", repositoryUrl, ex.getStatusCode(), ex.getMessage());
                    
                    if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED && (accessToken == null || accessToken.trim().isEmpty())) {
                        // Try with different branch names if unauthorized
                        return tryDifferentBranches(repositoryUrl, owner, repo, branch, path, recursive);
                    } else {
                        return handleGitHubTreeError(ex);
                    }
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Network error fetching GitHub tree: {}", ex.getMessage());
                    return Mono.just(RepositoryTreeResponse.error("Network error: " + ex.getMessage()));
                });
    }
    
    private Mono<RepositoryTreeResponse> tryDifferentBranches(String repositoryUrl, String owner, String repo, 
                                                             String originalBranch, String path, Boolean recursive) {
        logger.info("Trying different branch names for repository: {}", repositoryUrl);
        
        // Try common branch names
        String[] branchesToTry = {"master", "main", "develop", "dev"};
        
        return Mono.fromCallable(() -> {
            for (String branchName : branchesToTry) {
                if (!branchName.equals(originalBranch)) {
                    logger.info("Trying branch: {} for repository: {}", branchName, repositoryUrl);
                    
                    String apiPath = (path == null || path.trim().isEmpty()) ? "" : "/" + path.trim();
                    String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents%s?ref=%s", owner, repo, apiPath, branchName);
                    
                    try {
                        // Try synchronous call for fallback
                        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(apiUrl);
                        
                        String responseBody = request
                                .retrieve()
                                .bodyToMono(String.class)
                                .timeout(Duration.ofSeconds(5))
                                .block();
                        
                        if (responseBody != null) {
                            return parseGitHubTreeResponseSync(responseBody, repositoryUrl, branchName, path, recursive);
                        }
                    } catch (Exception e) {
                        logger.debug("Branch {} failed for repository: {}", branchName, repositoryUrl);
                        continue;
                    }
                }
            }
            return null;
        })
        .cast(RepositoryTreeResponse.class)
        .switchIfEmpty(
            // If Contents API fails for all branches, try Git Trees API
            tryGitTreesApiFallback(repositoryUrl, owner, repo, "master", path, recursive)
                .switchIfEmpty(tryGitTreesApiFallback(repositoryUrl, owner, repo, "main", path, recursive))
        )
        .switchIfEmpty(
            Mono.just(RepositoryTreeResponse.error(
                "GitHub API requires authentication for this repository. Please provide an access token. " +
                "You can get one from GitHub Settings > Developer settings > Personal access tokens"
            ))
        );
    }
    
    private RepositoryTreeResponse parseGitHubTreeResponseSync(String responseBody, String repositoryUrl, 
                                                              String branch, String path, Boolean recursive) {
        try {
            JsonNode jsonArray = objectMapper.readTree(responseBody);
            List<RepositoryTreeResponse.TreeItem> treeItems = new ArrayList<>();
            
            // Handle both single file and directory array responses
            if (jsonArray.isArray()) {
                for (JsonNode itemNode : jsonArray) {
                    RepositoryTreeResponse.TreeItem item = parseGitHubTreeItem(itemNode);
                    treeItems.add(item);
                }
            } else {
                // Single file response
                RepositoryTreeResponse.TreeItem item = parseGitHubTreeItem(jsonArray);
                treeItems.add(item);
            }
            
            logger.info("Successfully parsed {} tree items for GitHub repository: {}", treeItems.size(), repositoryUrl);
            return RepositoryTreeResponse.success(repositoryUrl, branch, path, treeItems, "GitHub");
            
        } catch (Exception e) {
            logger.error("Error parsing GitHub tree response: {}", e.getMessage());
            return null;
        }
    }
    
    private Mono<RepositoryTreeResponse> tryGitTreesApiFallback(String repositoryUrl, String owner, String repo, 
                                                               String branch, String path, Boolean recursive) {
        logger.info("Trying Git Trees API fallback for repository: {}", repositoryUrl);
        
        // First get the commit SHA for the branch
        String branchApiUrl = String.format("https://api.github.com/repos/%s/%s/branches/%s", owner, repo, branch);
        
        return webClient.get()
                .uri(branchApiUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .flatMap(branchResponse -> {
                    try {
                        JsonNode branchNode = objectMapper.readTree(branchResponse);
                        String commitSha = branchNode.path("commit").path("sha").asText();
                        
                        // Now get the tree using the commit SHA
                        String treeApiUrl = String.format("https://api.github.com/repos/%s/%s/git/trees/%s", owner, repo, commitSha);
                        if (Boolean.TRUE.equals(recursive)) {
                            treeApiUrl += "?recursive=1";
                        }
                        
                        return webClient.get()
                                .uri(treeApiUrl)
                                .retrieve()
                                .bodyToMono(String.class)
                                .timeout(Duration.ofSeconds(15))
                                .map(treeResponse -> parseGitTreesApiResponse(treeResponse, repositoryUrl, branch, path));
                        
                    } catch (Exception e) {
                        logger.error("Error parsing branch response: {}", e.getMessage());
                        return Mono.just(RepositoryTreeResponse.error("Error parsing branch information"));
                    }
                })
                .onErrorResume(ex -> {
                    logger.warn("Git Trees API fallback failed: {}", ex.getMessage());
                    // Try web scraping as final fallback
                    return tryWebScrapingFallback(repositoryUrl, owner, repo, branch, path, recursive);
                });
    }
    
    private Mono<RepositoryTreeResponse> tryWebScrapingFallback(String repositoryUrl, String owner, String repo, 
                                                               String branch, String path, Boolean recursive) {
        logger.info("Trying web scraping fallback for repository: {} (recursive: {})", repositoryUrl, recursive);
        
        // Build the GitHub web URL
        String webPath = (path == null || path.trim().isEmpty()) ? "" : "/" + path.trim();
        String webUrl = String.format("https://github.com/%s/%s/tree/%s%s", owner, repo, branch, webPath);
        
        return webClient.get()
                .uri(webUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .flatMap(htmlContent -> parseGitHubWebPageWithRecursion(htmlContent, repositoryUrl, branch, path, owner, repo, recursive))
                .onErrorResume(ex -> {
                    logger.warn("Web scraping fallback failed: {}", ex.getMessage());
                    return Mono.just(createAuthenticationRequiredResponse(repositoryUrl));
                });
    }
    
    private Mono<RepositoryTreeResponse> parseGitHubWebPageWithRecursion(String htmlContent, String repositoryUrl, 
                                                                         String branch, String path, String owner, String repo, Boolean recursive) {
        try {
            List<RepositoryTreeResponse.TreeItem> treeItems = new ArrayList<>();
            
            // Simple regex-based parsing to extract file/directory names
            Pattern filePattern = Pattern.compile("href=\"[^\"]*/(blob|tree)/[^\"]*?/([^/\"]+)\"[^>]*>\\s*([^<]+)");
            java.util.regex.Matcher matcher = filePattern.matcher(htmlContent);
            
            java.util.Set<String> seenItems = new java.util.HashSet<>();
            
            while (matcher.find()) {
                String type = "blob".equals(matcher.group(1)) ? "file" : "dir";
                String name = matcher.group(2);
                String displayName = matcher.group(3).trim();
                
                // Avoid duplicates and filter out navigation items
                if (!seenItems.contains(name) && !name.equals("..") && !name.isEmpty()) {
                    seenItems.add(name);
                    
                    String itemPath = (path == null || path.trim().isEmpty()) ? name : path.trim() + "/" + name;
                    
                    RepositoryTreeResponse.TreeItem item = new RepositoryTreeResponse.TreeItem(
                        name, itemPath, type, "unknown", null, null, null
                    );
                    
                    treeItems.add(item);
                }
            }
            
            if (treeItems.isEmpty()) {
                // If no items found, create a helpful message
                return Mono.just(createAuthenticationRequiredResponse(repositoryUrl));
            }
            
            // If recursive is requested, fetch children for directories
            if (Boolean.TRUE.equals(recursive)) {
                return fetchChildrenForDirectories(treeItems, repositoryUrl, branch, owner, repo)
                        .map(items -> {
                            logger.info("Successfully parsed {} items with children using web scraping for repository: {}", items.size(), repositoryUrl);
                            return RepositoryTreeResponse.success(repositoryUrl, branch, path, items, "GitHub");
                        });
            } else {
                logger.info("Successfully parsed {} items using web scraping for repository: {}", treeItems.size(), repositoryUrl);
                return Mono.just(RepositoryTreeResponse.success(repositoryUrl, branch, path, treeItems, "GitHub"));
            }
            
        } catch (Exception e) {
            logger.error("Error parsing GitHub web page: {}", e.getMessage());
            return Mono.just(createAuthenticationRequiredResponse(repositoryUrl));
        }
    }
    
    private Mono<List<RepositoryTreeResponse.TreeItem>> fetchChildrenForDirectories(
            List<RepositoryTreeResponse.TreeItem> items, String repositoryUrl, String branch, String owner, String repo) {
        
        List<Mono<RepositoryTreeResponse.TreeItem>> itemMonos = new ArrayList<>();
        
        for (RepositoryTreeResponse.TreeItem item : items) {
            if ("dir".equals(item.getType())) {
                Mono<RepositoryTreeResponse.TreeItem> itemWithChildren = fetchDirectoryChildrenWebScraping(
                    item, repositoryUrl, branch, owner, repo);
                itemMonos.add(itemWithChildren);
            } else {
                itemMonos.add(Mono.just(item));
            }
        }
        
        if (itemMonos.isEmpty()) {
            return Mono.just(items);
        }
        
        // Use Flux.merge to handle all the monos properly
        return Flux.fromIterable(itemMonos)
                .flatMap(mono -> mono)
                .collectList();
    }
    
    private Mono<RepositoryTreeResponse.TreeItem> fetchDirectoryChildrenWebScraping(
            RepositoryTreeResponse.TreeItem directory, String repositoryUrl, String branch, 
            String owner, String repo) {
        
        String webUrl = String.format("https://github.com/%s/%s/tree/%s/%s", owner, repo, branch, directory.getPath());
        
        return webClient.get()
                .uri(webUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(8))
                .map(htmlContent -> {
                    try {
                        List<RepositoryTreeResponse.TreeItem> children = new ArrayList<>();
                        
                        // Parse child items
                        Pattern filePattern = Pattern.compile("href=\"[^\"]*/(blob|tree)/[^\"]*?/([^/\"]+)\"[^>]*>\\s*([^<]+)");
                        java.util.regex.Matcher matcher = filePattern.matcher(htmlContent);
                        
                        java.util.Set<String> seenItems = new java.util.HashSet<>();
                        
                        while (matcher.find()) {
                            String type = "blob".equals(matcher.group(1)) ? "file" : "dir";
                            String name = matcher.group(2);
                            
                            // Avoid duplicates and filter out navigation items
                            if (!seenItems.contains(name) && !name.equals("..") && !name.isEmpty()) {
                                seenItems.add(name);
                                
                                String childPath = directory.getPath() + "/" + name;
                                
                                RepositoryTreeResponse.TreeItem child = new RepositoryTreeResponse.TreeItem(
                                    name, childPath, type, "unknown", null, null, null
                                );
                                
                                children.add(child);
                            }
                        }
                        
                        directory.setChildren(children);
                        logger.debug("Fetched {} children for directory: {}", children.size(), directory.getPath());
                        return directory;
                        
                    } catch (Exception e) {
                        logger.warn("Error parsing children for directory {}: {}", directory.getPath(), e.getMessage());
                        return directory;
                    }
                })
                .onErrorReturn(directory);
    }
    
    private RepositoryTreeResponse createAuthenticationRequiredResponse(String repositoryUrl) {
        String message = "GitHub now requires authentication for API access, even for public repositories. " +
                        "To get the complete tree structure with detailed information, please provide a GitHub access token. " +
                        "\n\nHow to get a GitHub access token:" +
                        "\n1. Go to GitHub.com → Settings → Developer settings" +
                        "\n2. Click 'Personal access tokens' → 'Tokens (classic)'" +
                        "\n3. Generate new token with 'repo' or 'public_repo' scope" +
                        "\n4. Use the token in the 'token' parameter or request body" +
                        "\n\nAlternatively, you can use the repository verification endpoint which has better fallback support.";
        
        return RepositoryTreeResponse.error(message);
    }
    
    private RepositoryTreeResponse parseGitTreesApiResponse(String responseBody, String repositoryUrl, String branch, String path) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode treeArray = jsonNode.path("tree");
            List<RepositoryTreeResponse.TreeItem> treeItems = new ArrayList<>();
            
            String filterPath = (path == null || path.trim().isEmpty()) ? "" : path.trim() + "/";
            
            for (JsonNode itemNode : treeArray) {
                String itemPath = itemNode.path("path").asText();
                
                // Filter by path if specified
                if (!filterPath.isEmpty() && !itemPath.startsWith(filterPath)) {
                    continue;
                }
                
                // For non-recursive, only show immediate children
                if (path != null && !path.trim().isEmpty()) {
                    String relativePath = itemPath.substring(filterPath.length());
                    if (relativePath.contains("/")) {
                        continue; // Skip nested items for non-recursive
                    }
                }
                
                String name = itemPath.contains("/") ? itemPath.substring(itemPath.lastIndexOf("/") + 1) : itemPath;
                String type = itemNode.path("type").asText();
                String sha = itemNode.path("sha").asText();
                Long size = itemNode.path("size").isNull() ? null : itemNode.path("size").asLong();
                
                // Convert Git API type to Contents API type
                if ("tree".equals(type)) {
                    type = "dir";
                } else if ("blob".equals(type)) {
                    type = "file";
                }
                
                RepositoryTreeResponse.TreeItem item = new RepositoryTreeResponse.TreeItem(
                    name, itemPath, type, sha, size, null, null
                );
                
                treeItems.add(item);
            }
            
            logger.info("Successfully parsed {} tree items using Git Trees API for repository: {}", treeItems.size(), repositoryUrl);
            return RepositoryTreeResponse.success(repositoryUrl, branch, path, treeItems, "GitHub");
            
        } catch (Exception e) {
            logger.error("Error parsing Git Trees API response: {}", e.getMessage());
            return RepositoryTreeResponse.error("Error parsing tree structure from Git Trees API");
        }
    }
    
    private Mono<RepositoryTreeResponse> parseGitHubTreeResponse(String responseBody, String repositoryUrl, 
                                                               String branch, String path, Boolean recursive, 
                                                               String accessToken, String owner, String repo) {
        try {
            JsonNode jsonArray = objectMapper.readTree(responseBody);
            List<RepositoryTreeResponse.TreeItem> treeItems = new ArrayList<>();
            
            // Handle both single file and directory array responses
            if (jsonArray.isArray()) {
                for (JsonNode itemNode : jsonArray) {
                    RepositoryTreeResponse.TreeItem item = parseGitHubTreeItem(itemNode);
                    treeItems.add(item);
                }
            } else {
                // Single file response
                RepositoryTreeResponse.TreeItem item = parseGitHubTreeItem(jsonArray);
                treeItems.add(item);
            }
            
            // If recursive is requested, fetch children for directories
            if (Boolean.TRUE.equals(recursive)) {
                return fetchChildrenRecursively(treeItems, repositoryUrl, branch, accessToken, owner, repo, "GitHub")
                        .map(items -> RepositoryTreeResponse.success(repositoryUrl, branch, path, items, "GitHub"));
            } else {
                return Mono.just(RepositoryTreeResponse.success(repositoryUrl, branch, path, treeItems, "GitHub"));
            }
            
        } catch (Exception e) {
            logger.error("Error parsing GitHub tree response: {}", e.getMessage());
            return Mono.just(RepositoryTreeResponse.error("Error parsing tree structure"));
        }
    }
    
    private RepositoryTreeResponse.TreeItem parseGitHubTreeItem(JsonNode itemNode) {
        String name = itemNode.path("name").asText();
        String itemPath = itemNode.path("path").asText();
        String type = itemNode.path("type").asText();
        String sha = itemNode.path("sha").asText();
        Long size = itemNode.path("size").isNull() ? null : itemNode.path("size").asLong();
        String url = itemNode.path("url").asText();
        String downloadUrl = itemNode.path("download_url").asText();
        
        return new RepositoryTreeResponse.TreeItem(name, itemPath, type, sha, size, url, downloadUrl);
    }
    
    private Mono<List<RepositoryTreeResponse.TreeItem>> fetchChildrenRecursively(
            List<RepositoryTreeResponse.TreeItem> items, String repositoryUrl, String branch, 
            String accessToken, String owner, String repo, String platform) {
        
        List<Mono<RepositoryTreeResponse.TreeItem>> childFetches = new ArrayList<>();
        
        for (RepositoryTreeResponse.TreeItem item : items) {
            if ("dir".equals(item.getType())) {
                Mono<RepositoryTreeResponse.TreeItem> childFetch = fetchDirectoryChildren(
                        item, repositoryUrl, branch, accessToken, owner, repo, platform);
                childFetches.add(childFetch);
            } else {
                childFetches.add(Mono.just(item));
            }
        }
        
        return Mono.zip(childFetches, objects -> {
            List<RepositoryTreeResponse.TreeItem> result = new ArrayList<>();
            for (Object obj : objects) {
                result.add((RepositoryTreeResponse.TreeItem) obj);
            }
            return result;
        });
    }
    
    private Mono<RepositoryTreeResponse.TreeItem> fetchDirectoryChildren(
            RepositoryTreeResponse.TreeItem directory, String repositoryUrl, String branch, 
            String accessToken, String owner, String repo, String platform) {
        
        String apiUrl = platform.equals("GitHub")
            ? String.format("https://api.github.com/repos/%s/%s/contents/%s?ref=%s", owner, repo, directory.getPath(), branch)
            : String.format("https://gitlab.com/api/v4/projects/%s/repository/tree?path=%s&ref=%s", 
                           (owner + "/" + repo).replace("/", "%2F"), directory.getPath(), branch);
        
        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(apiUrl);
        
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            if (platform.equals("GitHub")) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.trim());
            } else {
                request = request.header("PRIVATE-TOKEN", accessToken.trim());
            }
        }
        
        return request
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .map(responseBody -> {
                    try {
                        JsonNode jsonArray = objectMapper.readTree(responseBody);
                        List<RepositoryTreeResponse.TreeItem> children = new ArrayList<>();
                        
                        for (JsonNode itemNode : jsonArray) {
                            RepositoryTreeResponse.TreeItem child = platform.equals("GitHub")
                                ? parseGitHubTreeItem(itemNode)
                                : parseGitLabTreeItem(itemNode);
                            children.add(child);
                        }
                        
                        directory.setChildren(children);
                        return directory;
                        
                    } catch (Exception e) {
                        logger.warn("Error parsing children for directory {}: {}", directory.getPath(), e.getMessage());
                        return directory;
                    }
                })
                .onErrorReturn(directory);
    }
    
    private Mono<RepositoryTreeResponse> handleGitHubTreeError(WebClientResponseException ex) {
        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            return Mono.just(RepositoryTreeResponse.error("Repository, branch, or path not found"));
        } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
            return Mono.just(RepositoryTreeResponse.error("Access forbidden. Repository may be private or rate limit exceeded"));
        } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return Mono.just(RepositoryTreeResponse.error("GitHub API requires authentication. Please provide an access token"));
        } else {
            return Mono.just(RepositoryTreeResponse.error("GitHub API error: " + ex.getStatusCode()));
        }
    }
    
    private Mono<RepositoryTreeResponse> fetchGitLabTree(String repositoryUrl, String accessToken, 
                                                        String branch, String path, Boolean recursive) {
        Matcher matcher = GITLAB_PATTERN.matcher(repositoryUrl);
        if (!matcher.matches()) {
            return Mono.just(RepositoryTreeResponse.error("Invalid GitLab URL format"));
        }
        
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        
        // If branch is not specified, get the default branch first
        if (branch == null || branch.trim().isEmpty()) {
            return getDefaultBranch(repositoryUrl, accessToken, owner, repo, "GitLab")
                    .flatMap(defaultBranch -> fetchGitLabTreeWithBranch(repositoryUrl, accessToken, owner, repo, defaultBranch, path, recursive));
        } else {
            return fetchGitLabTreeWithBranch(repositoryUrl, accessToken, owner, repo, branch, path, recursive);
        }
    }
    
    private Mono<RepositoryTreeResponse> fetchGitLabTreeWithBranch(String repositoryUrl, String accessToken, 
                                                                  String owner, String repo, String branch, 
                                                                  String path, Boolean recursive) {
        String projectPath = owner + "/" + repo;
        String encodedPath = projectPath.replace("/", "%2F");
        
        StringBuilder apiUrl = new StringBuilder(String.format("https://gitlab.com/api/v4/projects/%s/repository/tree", encodedPath));
        apiUrl.append("?ref=").append(branch);
        
        if (path != null && !path.trim().isEmpty()) {
            apiUrl.append("&path=").append(path.trim());
        }
        
        if (Boolean.TRUE.equals(recursive)) {
            apiUrl.append("&recursive=true");
        }
        
        logger.info("Fetching GitLab tree from: {}", apiUrl);
        
        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(apiUrl.toString());
        
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            request = request.header("PRIVATE-TOKEN", accessToken.trim());
        }
        
        return request
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .map(responseBody -> parseGitLabTreeResponse(responseBody, repositoryUrl, branch, path))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.warn("GitLab tree API error for {}: {} - {}", repositoryUrl, ex.getStatusCode(), ex.getMessage());
                    return handleGitLabTreeError(ex);
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Network error fetching GitLab tree: {}", ex.getMessage());
                    return Mono.just(RepositoryTreeResponse.error("Network error: " + ex.getMessage()));
                });
    }
    
    private RepositoryTreeResponse parseGitLabTreeResponse(String responseBody, String repositoryUrl, 
                                                          String branch, String path) {
        try {
            JsonNode jsonArray = objectMapper.readTree(responseBody);
            List<RepositoryTreeResponse.TreeItem> treeItems = new ArrayList<>();
            
            for (JsonNode itemNode : jsonArray) {
                RepositoryTreeResponse.TreeItem item = parseGitLabTreeItem(itemNode);
                treeItems.add(item);
            }
            
            logger.info("Successfully fetched {} tree items for GitLab repository: {}", treeItems.size(), repositoryUrl);
            return RepositoryTreeResponse.success(repositoryUrl, branch, path, treeItems, "GitLab");
            
        } catch (Exception e) {
            logger.error("Error parsing GitLab tree response: {}", e.getMessage());
            return RepositoryTreeResponse.error("Error parsing tree structure");
        }
    }
    
    private RepositoryTreeResponse.TreeItem parseGitLabTreeItem(JsonNode itemNode) {
        String name = itemNode.path("name").asText();
        String itemPath = itemNode.path("path").asText();
        String type = itemNode.path("type").asText();
        String id = itemNode.path("id").asText();
        String mode = itemNode.path("mode").asText();
        
        // GitLab uses different field names
        return new RepositoryTreeResponse.TreeItem(name, itemPath, type, id, null, null, null);
    }
    
    private Mono<RepositoryTreeResponse> handleGitLabTreeError(WebClientResponseException ex) {
        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            return Mono.just(RepositoryTreeResponse.error("Repository, branch, or path not found"));
        } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
            return Mono.just(RepositoryTreeResponse.error("Access forbidden. Repository may be private or access token invalid"));
        } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return Mono.just(RepositoryTreeResponse.error("Unauthorized. Access token required for this repository"));
        } else {
            return Mono.just(RepositoryTreeResponse.error("GitLab API error: " + ex.getStatusCode()));
        }
    }
}