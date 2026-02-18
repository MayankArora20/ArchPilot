package com.archpilot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.archpilot.facade.RepositoryFacade;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/umldigr")
@Tag(name = "UML Diagram Generation", description = "APIs for generating UML diagrams from repository code")
@Validated
public class UmlDigrController {
    
    @Autowired
    private RepositoryFacade repositoryFacade;
    
    @GetMapping("/classDigrGenerator")
    @Operation(summary = "Generate class diagram from Java classes", description = "Retrieves Java class files and generates PlantUML diagram and JSON representation")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class diagram generation completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid repository URL parameter")
    })
    public Mono<ResponseEntity<com.archpilot.model.ApiResponse<Object>>> generateClassDiagram(
            @RequestParam("url") @Parameter(description = "Repository URL") String repositoryUrl,
            @RequestParam(value = "token", required = false) @Parameter(description = "Optional access token") String accessToken,
            @RequestParam(value = "branch", required = false) @Parameter(description = "Branch name (defaults to default branch)") String branch,
            @RequestParam(value = "recursive", defaultValue = "0") @Parameter(description = "Fetch tree recursively (1 for recursive, 0 for non-recursive)") Integer recursive) {
        return repositoryFacade.generateClassDiagram(repositoryUrl, accessToken, branch, recursive == 1).map(ResponseEntity::ok);
    }
    
    @GetMapping("/health")
    @Operation(summary = "UML diagram service health check", description = "Check if the UML diagram generation service is operational")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("UML diagram generation service is operational");
    }
}