package com.archpilot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.archpilot.dto.RepositoryBranchesRequest;
import com.archpilot.dto.RepositoryVerificationRequest;
import com.archpilot.facade.RepositoryFacade;
import com.archpilot.model.RepositoryBranchesData;
import com.archpilot.model.RepositoryInfo;
import com.archpilot.model.RepositoryTreeData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    
    @GetMapping("/tree")
    @Operation(summary = "Get repository tree structure via GET request", description = "Retrieves repository tree structure using URL parameters")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tree structure retrieval completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid repository URL parameter")
    })
    public Mono<ResponseEntity<com.archpilot.model.ApiResponse<RepositoryTreeData>>> getRepositoryTreeGet(
            @RequestParam("url") @Parameter(description = "Repository URL") String repositoryUrl,
            @RequestParam(value = "token", required = false) @Parameter(description = "Optional access token") String accessToken,
            @RequestParam(value = "branch", required = false) @Parameter(description = "Branch name (defaults to default branch)") String branch,
            @RequestParam(value = "recursive", defaultValue = "0") @Parameter(description = "Fetch tree recursively (1 for recursive, 0 for non-recursive)") Integer recursive) {
        return repositoryFacade.getRepositoryTree(repositoryUrl, accessToken, branch, recursive == 1).map(ResponseEntity::ok);
    }
    

}