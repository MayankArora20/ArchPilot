package com.archpilot.service;

import java.io.IOException;
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
    
    // Repository verification methods (keeping existing implementation)
    private Mono<RepositoryVerificationResponse> verifyGitHubRepository(String repositoryUrl, String accessToken) {
        Matcher matcher = GITHUB_PATTERN.matcher(repositoryUrl);
        if (!matcher.matches()) {
            return Mono.just(RepositoryVerificationResponse.error("Invalid GitHub URL format"));
        }
        
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        String apiUrl = String.format("https://api.github.com/repos/%s/%s", owner, repo);
        
        logger.info("Making GitHub API request to: {}", apiUrl);
        
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            return makeGitHubRequest(apiUrl, accessToken.trim(), repositoryUrl);
        } else {
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
                        
                        com.archpilot.model.RepositoryInfo info = new com.archpilot.model.RepositoryInfo(
                                jsonNode.path("name").asText(),
                                jsonNode.path("full_name").asText(),
                                jsonNode.path("description").asText("No description"),
                                jsonNode.path("default_branch").asText("main"),
                                jsonNode.path("private").asBoolean(false),
                                jsonNode.path("language").asText("Unknown"),
                                "GitHub",
                                repositoryUrl
                        );
                        
                        logger.info("Successfully verified GitHub repository: {}", repositoryUrl);
                        return RepositoryVerificationResponse.verified(repositoryUrl, info);
                        
                    } catch (IOException e) {
                        logger.error("Error parsing GitHub API response: {}", e.getMessage());
                        return RepositoryVerificationResponse.error("Error parsing repository information");
                    } catch (Exception e) {
                        logger.error("Unexpected error parsing GitHub API response: {}", e.getMessage());
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
        
        return webClient.get()
                .uri(repositoryUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .map(responseBody -> {
                    com.archpilot.model.RepositoryInfo info = new com.archpilot.model.RepositoryInfo(
                            repo,
                            owner + "/" + repo,
                            "Repository verified via web access (API authentication required for detailed info)",
                            "main",
                            false,
                            "Unknown",
                            "GitHub",
                            repositoryUrl
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
                        
                        com.archpilot.model.RepositoryInfo info = new com.archpilot.model.RepositoryInfo(
                                jsonNode.path("name").asText(),
                                jsonNode.path("path_with_namespace").asText(),
                                jsonNode.path("description").asText("No description"),
                                jsonNode.path("default_branch").asText("main"),
                                jsonNode.path("visibility").asText("public").equals("private"),
                                "Unknown",
                                "GitLab",
                                repositoryUrl
                        );
                        
                        return RepositoryVerificationResponse.verified(repositoryUrl, info);
                        
                    } catch (IOException e) {
                        logger.error("Error parsing GitLab API response: {}", e.getMessage());
                        return RepositoryVerificationResponse.error("Error parsing repository information");
                    } catch (Exception e) {
                        logger.error("Unexpected error parsing GitLab API response: {}", e.getMessage());
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
    
    // Branches methods (keeping existing implementation but simplified)
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
        
        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(apiUrl);
        
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.trim());
        }
        
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
    
    private RepositoryBranchesResponse parseGitHubBranchesResponse(String responseBody, String repositoryUrl) {
        try {
            JsonNode jsonArray = objectMapper.readTree(responseBody);
            List<com.archpilot.model.BranchInfo> branches = new ArrayList<>();
            
            for (JsonNode branchNode : jsonArray) {
                String branchName = branchNode.path("name").asText();
                String sha = branchNode.path("commit").path("sha").asText();
                boolean isProtected = branchNode.path("protected").asBoolean(false);
                
                com.archpilot.model.BranchInfo branchInfo = new com.archpilot.model.BranchInfo(
                        branchName, sha, false, isProtected, null, null, null
                );
                
                branches.add(branchInfo);
            }
            
            logger.info("Successfully fetched {} branches for GitHub repository: {}", branches.size(), repositoryUrl);
            return RepositoryBranchesResponse.success(repositoryUrl, branches, "GitHub");
            
        } catch (IOException e) {
            logger.error("Error parsing GitHub branches response: {}", e.getMessage());
            return RepositoryBranchesResponse.error("Error parsing branches information");
        } catch (Exception e) {
            logger.error("Unexpected error parsing GitHub branches response: {}", e.getMessage());
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
                        List<com.archpilot.model.BranchInfo> branches = new ArrayList<>();
                        
                        for (JsonNode branchNode : jsonArray) {
                            String branchName = branchNode.path("name").asText();
                            String sha = branchNode.path("commit").path("id").asText();
                            boolean isProtected = branchNode.path("protected").asBoolean(false);
                            boolean isDefault = branchNode.path("default").asBoolean(false);
                            
                            JsonNode commitNode = branchNode.path("commit");
                            String commitMessage = commitNode.path("message").asText(null);
                            String commitAuthor = commitNode.path("author_name").asText(null);
                            
                            com.archpilot.model.BranchInfo branchInfo = new com.archpilot.model.BranchInfo(
                                    branchName, sha, isDefault, isProtected, commitMessage, commitAuthor, null
                            );
                            
                            branches.add(branchInfo);
                        }
                        
                        logger.info("Successfully fetched {} branches for GitLab repository: {}", branches.size(), repositoryUrl);
                        return RepositoryBranchesResponse.success(repositoryUrl, branches, "GitLab");
                        
                    } catch (IOException e) {
                        logger.error("Error parsing GitLab branches response: {}", e.getMessage());
                        return RepositoryBranchesResponse.error("Error parsing branches information");
                    } catch (Exception e) {
                        logger.error("Unexpected error parsing GitLab branches response: {}", e.getMessage());
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
    
    // NEW SIMPLIFIED TREE API IMPLEMENTATION
    public Mono<RepositoryTreeResponse> getRepositoryTree(String repositoryUrl, String accessToken, 
                                                         String branch, Boolean recursive) {
        logger.info("Fetching tree structure for repository: {}, branch: {}", repositoryUrl, branch);
        
        try {
            if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
                return Mono.just(RepositoryTreeResponse.error("Repository URL is required"));
            }
            
            repositoryUrl = repositoryUrl.trim();
            
            if (isGitHubUrl(repositoryUrl)) {
                logger.info("Fetching GitHub tree for URL: {}", repositoryUrl);
                return fetchGitHubTree(repositoryUrl, accessToken, branch, recursive);
            } else {
                logger.warn("Unsupported repository platform for tree fetching: {}", repositoryUrl);
                return Mono.just(RepositoryTreeResponse.error("Unsupported repository platform. Only GitHub is supported"));
            }
            
        } catch (Exception e) {
            logger.error("Error fetching repository tree: {}", e.getMessage(), e);
            return Mono.just(RepositoryTreeResponse.error("Internal error: " + e.getMessage()));
        }
    }
    
    private Mono<RepositoryTreeResponse> fetchGitHubTree(String repositoryUrl, String accessToken, 
                                                        String branch, Boolean recursive) {
        Matcher matcher = GITHUB_PATTERN.matcher(repositoryUrl);
        if (!matcher.matches()) {
            return Mono.just(RepositoryTreeResponse.error("Invalid GitHub URL format"));
        }
        
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        
        return fetchGitHubTreeWithBranch(repositoryUrl, accessToken, owner, repo, branch, recursive);
    }
    
    private Mono<RepositoryTreeResponse> fetchGitHubTreeWithBranch(String repositoryUrl, String accessToken, 
                                                                  String owner, String repo, String branch, 
                                                                  Boolean recursive) {
        logger.debug("Processing GitHub tree request for repository URL: {}", repositoryUrl);
        
        // If no access token, try different approaches
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return tryUnauthenticatedTreeAccess(repositoryUrl, owner, repo, branch, recursive);
        }
        
        // With access token, use direct Git Trees API
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/git/trees/%s?recursive=%d", 
                                     owner, repo, branch != null ? branch : "HEAD", recursive != null && recursive ? 1 : 0);
        
        logger.info("Fetching GitHub tree from: {}", apiUrl);
        
        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(apiUrl);
        request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.trim());
        
        return request
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .map(responseBody -> parseGitTreesApiResponse(responseBody, repositoryUrl, branch))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.warn("GitHub tree API error for {}: {} - {}", repositoryUrl, ex.getStatusCode(), ex.getMessage());
                    return handleGitHubTreeError(ex);
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Network error fetching GitHub tree: {}", ex.getMessage());
                    return Mono.just(RepositoryTreeResponse.error("Network error: " + ex.getMessage()));
                });
    }
    
    private Mono<RepositoryTreeResponse> tryUnauthenticatedTreeAccess(String repositoryUrl, String owner, String repo, String branch, Boolean recursive) {
        logger.info("Trying unauthenticated access for repository: {}", repositoryUrl);
        
        // First, try to get the commit SHA for the branch using the branches API
        String branchApiUrl = String.format("https://api.github.com/repos/%s/%s/branches/%s", owner, repo, branch != null ? branch : "master");
        
        return webClient.get()
                .uri(branchApiUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .flatMap(branchResponse -> {
                    try {
                        JsonNode branchNode = objectMapper.readTree(branchResponse);
                        String commitSha = branchNode.path("commit").path("sha").asText();
                        
                        // Now try the Git Trees API with the commit SHA
                        String treeApiUrl = String.format("https://api.github.com/repos/%s/%s/git/trees/%s?recursive=%d", 
                                                         owner, repo, commitSha, recursive != null && recursive ? 1 : 0);
                        
                        logger.info("Trying Git Trees API with commit SHA: {}", treeApiUrl);
                        
                        return webClient.get()
                                .uri(treeApiUrl)
                                .retrieve()
                                .bodyToMono(String.class)
                                .timeout(Duration.ofSeconds(15))
                                .map(treeResponse -> parseGitTreesApiResponse(treeResponse, repositoryUrl, branch));
                        
                    } catch (IOException e) {
                        logger.error("Error parsing branch response: {}", e.getMessage());
                        return Mono.just(RepositoryTreeResponse.error("Error parsing branch information"));
                    } catch (Exception e) {
                        logger.error("Unexpected error parsing branch response: {}", e.getMessage());
                        return Mono.just(RepositoryTreeResponse.error("Error parsing branch information"));
                    }
                })
                .onErrorResume(ex -> {
                    logger.warn("Branch API failed, trying alternative approaches: {}", ex.getMessage());
                    return tryAlternativeBranches(repositoryUrl, owner, repo, recursive);
                });
    }
    
    private Mono<RepositoryTreeResponse> tryAlternativeBranches(String repositoryUrl, String owner, String repo, Boolean recursive) {
        // Try common branch names
        String[] branchesToTry = {"master", "main", "HEAD"};
        
        return tryBranchSequentially(repositoryUrl, owner, repo, branchesToTry, 0, recursive);
    }
    
    private Mono<RepositoryTreeResponse> tryBranchSequentially(String repositoryUrl, String owner, String repo, 
                                                              String[] branches, int index, Boolean recursive) {
        if (index >= branches.length) {
            return Mono.just(RepositoryTreeResponse.error(
                "GitHub API requires authentication for this repository. Please provide an access token. " +
                "You can get one from GitHub Settings > Developer settings > Personal access tokens"
            ));
        }
        
        String branch = branches[index];
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/git/trees/%s?recursive=%d", 
                                     owner, repo, branch, recursive != null && recursive ? 1 : 0);
        
        logger.info("Trying branch '{}' for repository: {}", branch, repositoryUrl);
        
        return webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .map(responseBody -> parseGitTreesApiResponse(responseBody, repositoryUrl, branch))
                .onErrorResume(ex -> {
                    logger.debug("Branch '{}' failed, trying next: {}", branch, ex.getMessage());
                    return tryBranchSequentially(repositoryUrl, owner, repo, branches, index + 1, recursive);
                });
    }
    
    private RepositoryTreeResponse parseGitTreesApiResponse(String responseBody, String repositoryUrl, String branch) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode treeArray = jsonNode.path("tree");
            List<com.archpilot.model.TreeNode> treeItems = new ArrayList<>();
            
            for (JsonNode itemNode : treeArray) {
                String itemPath = itemNode.path("path").asText();
                String name = itemPath.contains("/") ? itemPath.substring(itemPath.lastIndexOf("/") + 1) : itemPath;
                String type = itemNode.path("type").asText();
                String sha = itemNode.path("sha").asText();
                Long size = itemNode.path("size").isNull() ? null : itemNode.path("size").asLong();
                String url = itemNode.path("url").asText();
                
                // Convert Git API type to standard type
                if ("tree".equals(type)) {
                    type = "dir";
                } else if ("blob".equals(type)) {
                    type = "file";
                }
                
                com.archpilot.model.TreeNode item = new com.archpilot.model.TreeNode(
                    name, itemPath, type, sha, size, url, null
                );
                
                treeItems.add(item);
            }
            
            logger.info("Successfully parsed {} tree items using Git Trees API for repository: {}", treeItems.size(), repositoryUrl);
            return RepositoryTreeResponse.success(repositoryUrl, branch, treeItems, "GitHub", branch);
            
        } catch (IOException e) {
            logger.error("Error parsing Git Trees API response: {}", e.getMessage());
            return RepositoryTreeResponse.error("Error parsing tree structure from Git Trees API");
        } catch (Exception e) {
            logger.error("Unexpected error parsing Git Trees API response: {}", e.getMessage());
            return RepositoryTreeResponse.error("Error parsing tree structure from Git Trees API");
        }
    }
    
    private Mono<RepositoryTreeResponse> handleGitHubTreeError(WebClientResponseException ex) {
        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            return Mono.just(RepositoryTreeResponse.error("Repository not found or branch does not exist"));
        } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
            return Mono.just(RepositoryTreeResponse.error("Access forbidden. Repository may be private or rate limit exceeded. Please provide an access token"));
        } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return Mono.just(RepositoryTreeResponse.error("GitHub API requires authentication. Please provide an access token"));
        } else {
            return Mono.just(RepositoryTreeResponse.error("GitHub API error: " + ex.getStatusCode()));
        }
    }
}