package com.archpilot.controller;

import com.archpilot.dto.RepositoryVerificationRequest;
import com.archpilot.dto.RepositoryBranchesRequest;
import com.archpilot.facade.RepositoryFacade;
import com.archpilot.model.RepositoryInfo;
import com.archpilot.model.RepositoryBranchesData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/repository")
@Tag(name = "Repository Management", description = "APIs for repository verification and management")
@Validated
public class RepositoryController {
    
    @Autowired
    private RepositoryFacade repositoryFacade;
    
    @PostMapping("/verify")
    @Operation(summary = "Verify repository accessibility", description = "Verifies if a GitHub or GitLab repository URL is valid and accessible")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repository verification completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request format or URL"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Mono<ResponseEntity<com.archpilot.model.ApiResponse<RepositoryInfo>>> verifyRepository(@Valid @RequestBody RepositoryVerificationRequest request) {
        return repositoryFacade.verifyRepository(request).map(ResponseEntity::ok);
    }
    
    @GetMapping("/verify")
    @Operation(summary = "Verify repository via GET request", description = "Verifies repository accessibility using URL parameter")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repository verification completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid repository URL parameter")
    })
    public Mono<ResponseEntity<com.archpilot.model.ApiResponse<RepositoryInfo>>> verifyRepositoryGet(
            @RequestParam("url") @Parameter(description = "Repository URL to verify") String repositoryUrl,
            @RequestParam(value = "token", required = false) @Parameter(description = "Optional access token") String accessToken) {
        return repositoryFacade.verifyRepository(repositoryUrl, accessToken).map(ResponseEntity::ok);
    }
    
    @PostMapping("/branches")
    @Operation(summary = "Get repository branches", description = "Retrieves all branches from a GitHub or GitLab repository")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Branches retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request format or URL"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Mono<ResponseEntity<com.archpilot.model.ApiResponse<RepositoryBranchesData>>> getRepositoryBranches(@Valid @RequestBody RepositoryBranchesRequest request) {
        return repositoryFacade.getRepositoryBranches(request).map(ResponseEntity::ok);
    }
    
    @GetMapping("/branches")
    @Operation(summary = "Get repository branches via GET request", description = "Retrieves repository branches using URL parameter")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Branches retrieval completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid repository URL parameter")
    })
    public Mono<ResponseEntity<com.archpilot.model.ApiResponse<RepositoryBranchesData>>> getRepositoryBranchesGet(
            @RequestParam("url") @Parameter(description = "Repository URL") String repositoryUrl,
            @RequestParam(value = "token", required = false) @Parameter(description = "Optional access token") String accessToken,
            @RequestParam(value = "limit", defaultValue = "50") @Parameter(description = "Maximum branches (1-100)") Integer limit) {
        return repositoryFacade.getRepositoryBranches(repositoryUrl, accessToken, limit).map(ResponseEntity::ok);
    }
    
    @GetMapping("/health")
    @Operation(summary = "Repository service health check", description = "Check if the repository verification service is operational")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Repository verification service is operational");
    }
    
    @GetMapping("/auth-help")
    @Operation(summary = "GitHub authentication help", description = "Provides information on how to get GitHub access tokens")
    public ResponseEntity<com.archpilot.model.ApiResponse<Object>> getAuthHelp() {
        var helpInfo = java.util.Map.of(
            "message", "GitHub API now requires authentication for most operations",
            "howToGetToken", java.util.List.of(
                "1. Go to GitHub.com and sign in",
                "2. Click your profile picture → Settings",
                "3. Scroll down to 'Developer settings'",
                "4. Click 'Personal access tokens' → 'Tokens (classic)'",
                "5. Click 'Generate new token (classic)'",
                "6. Give it a name and select 'repo' scope for private repos, or 'public_repo' for public repos",
                "7. Click 'Generate token' and copy the token",
                "8. Use this token in the 'accessToken' field of your API requests"
            ),
            "note", "For public repositories, you can also try the verification endpoint which has fallback mechanisms"
        );
        
        return ResponseEntity.ok(com.archpilot.model.ApiResponse.success("GitHub authentication help", helpInfo));
    }
}