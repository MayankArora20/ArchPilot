package com.archpilot.facade;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.archpilot.dto.RepositoryBranchesRequest;
import com.archpilot.dto.RepositoryVerificationRequest;
import com.archpilot.model.ApiResponse;
import com.archpilot.model.RepositoryBranchesData;
import com.archpilot.model.RepositoryInfo;
import com.archpilot.model.RepositoryTreeData;
import com.archpilot.service.ClassDiagramGeneratorService;
import com.archpilot.service.RepositoryVerificationService;

import reactor.core.publisher.Mono;

@Component
public class RepositoryFacade {
    
    private static final Logger logger = LoggerFactory.getLogger(RepositoryFacade.class);
    
    @Autowired
    private RepositoryVerificationService repositoryVerificationService;
    
    @Autowired
    private ClassDiagramGeneratorService classDiagramGeneratorService;
    
    public Mono<ApiResponse<RepositoryInfo>> verifyRepository(RepositoryVerificationRequest request) {
        logger.info("Processing repository verification request: {}", request.getRepositoryUrl());
        
        return repositoryVerificationService
                .verifyRepository(request.getRepositoryUrl(), request.getAccessToken())
                .map(response -> {
                    if ("Verified".equals(response.getStatus())) {
                        RepositoryInfo repoInfo = mapToRepositoryInfo(response);
                        return ApiResponse.success("Repository verified successfully", repoInfo);
                    } else {
                        return ApiResponse.<RepositoryInfo>error(response.getMessage());
                    }
                })
                .onErrorResume(ex -> {
                    logger.error("Error in repository verification facade: {}", ex.getMessage());
                    return Mono.just(ApiResponse.error("Internal server error: " + ex.getMessage()));
                });
    }
    
    public Mono<ApiResponse<RepositoryInfo>> verifyRepository(String repositoryUrl, String accessToken) {
        RepositoryVerificationRequest request = new RepositoryVerificationRequest(repositoryUrl, accessToken);
        return verifyRepository(request);
    }
    
    public Mono<ApiResponse<RepositoryBranchesData>> getRepositoryBranches(RepositoryBranchesRequest request) {
        logger.info("Processing repository branches request: {}", request.getRepositoryUrl());
        
        return repositoryVerificationService
                .getRepositoryBranches(request.getRepositoryUrl(), request.getAccessToken(), request.getLimit())
                .map(response -> {
                    if ("Success".equals(response.getStatus())) {
                        RepositoryBranchesData branchesData = mapToBranchesData(response);
                        return ApiResponse.success("Branches retrieved successfully", branchesData);
                    } else {
                        return ApiResponse.<RepositoryBranchesData>error(response.getMessage());
                    }
                })
                .onErrorResume(ex -> {
                    logger.error("Error in repository branches facade: {}", ex.getMessage());
                    return Mono.just(ApiResponse.error("Internal server error: " + ex.getMessage()));
                });
    }
    
    public Mono<ApiResponse<RepositoryBranchesData>> getRepositoryBranches(String repositoryUrl, String accessToken, Integer limit) {
        RepositoryBranchesRequest request = new RepositoryBranchesRequest(repositoryUrl, accessToken);
        request.setLimit(limit);
        return getRepositoryBranches(request);
    }
    
    public Mono<ApiResponse<RepositoryTreeData>> getRepositoryTree(String repositoryUrl, String accessToken, 
                                                                  String branch, Boolean recursive) {
        logger.info("Processing repository tree request: {}", repositoryUrl);
        return repositoryVerificationService
                .getRepositoryTree(repositoryUrl, accessToken, branch, recursive)
                .map(response -> {
                    if ("Success".equals(response.getStatus())) {
                        RepositoryTreeData treeData = mapToTreeData(response);
                        return ApiResponse.success("Tree structure retrieved successfully", treeData);
                    } else {
                        return ApiResponse.<RepositoryTreeData>error(response.getMessage());
                    }
                })
                .onErrorResume(ex -> {
                    logger.error("Error in repository tree facade: {}", ex.getMessage());
                    return Mono.just(ApiResponse.error("Internal server error: " + ex.getMessage()));
                });
    }
    
    public Mono<ApiResponse<Object>> generateClassDiagram(String repositoryUrl, String accessToken, 
                                                         String branch, Boolean recursive) {
        logger.info("Processing class diagram generation request: {}", repositoryUrl);
        
        return repositoryVerificationService
                .getRepositoryTree(repositoryUrl, accessToken, branch, recursive)
                .map(response -> {
                    if ("Success".equals(response.getStatus())) {
                        RepositoryTreeData treeData = mapToTreeData(response);
                        RepositoryTreeData refinedTreeData = refineToJavaClasses(treeData);
                        
                        // Generate class diagram using the service
                        Map<String, Object> diagramResult = classDiagramGeneratorService.generateClassDiagram(refinedTreeData);
                        
                        return ApiResponse.<Object>success("Class diagram generated successfully", diagramResult);
                    } else {
                        return ApiResponse.<Object>error(response.getMessage());
                    }
                })
                .onErrorResume(ex -> {
                    logger.error("Error in class diagram generation facade: {}", ex.getMessage());
                    return Mono.just(ApiResponse.<Object>error("Internal server error: " + ex.getMessage()));
                });
    }
    
    private RepositoryInfo mapToRepositoryInfo(com.archpilot.dto.RepositoryVerificationResponse response) {
        return response.getRepositoryInfo();
    }
    
    private RepositoryBranchesData mapToBranchesData(com.archpilot.dto.RepositoryBranchesResponse response) {
        if (response.getBranches() == null) {
            return null;
        }
        
        return new RepositoryBranchesData(response.getRepositoryUrl(), response.getBranches(), response.getPlatform());
    }
    
    private RepositoryTreeData mapToTreeData(com.archpilot.dto.RepositoryTreeResponse response) {
        if (response.getTree() == null) {
            return null;
        }
        
        return new RepositoryTreeData(response.getRepositoryUrl(), response.getBranch(), 
                                    response.getTree(), response.getPlatform(), response.getCommitSha());
    }
    
    private com.archpilot.model.TreeNode filterJavaClassesFromNode(com.archpilot.model.TreeNode node) {
        if (node == null) {
            return null;
        }
        
        // If it's a file, check if it's a Java class file
        if ("file".equals(node.getType())) {
            if (node.getName() != null && node.getName().endsWith(".java")) {
                // Return the Java file with all its properties including SHA
                return new com.archpilot.model.TreeNode(
                        node.getName(),
                        node.getPath(),
                        node.getType(),
                        node.getSha(),
                        node.getSize(),
                        node.getUrl(),
                        node.getDownloadUrl()
                );
            }
            // Not a Java file, exclude it
            return null;
        }
        
        // If it's a directory, recursively filter its children
        if ("dir".equals(node.getType()) && node.getChildren() != null) {
            var filteredChildren = node.getChildren().stream()
                    .map(this::filterJavaClassesFromNode)
                    .filter(child -> child != null)
                    .toList();
            
            // Only include the directory if it has Java files in it (directly or in subdirectories)
            if (!filteredChildren.isEmpty()) {
                com.archpilot.model.TreeNode dirNode = new com.archpilot.model.TreeNode(
                        node.getName(),
                        node.getPath(),
                        node.getType(),
                        node.getSha(),
                        node.getSize(),
                        node.getUrl(),
                        node.getDownloadUrl()
                );
                dirNode.setChildren(filteredChildren);
                return dirNode;
            }
        }
        
        return null;
    }
    
    private RepositoryTreeData refineToJavaClasses(RepositoryTreeData originalTreeData) {
        if (originalTreeData == null || originalTreeData.getTree() == null) {
            return originalTreeData;
        }
        
        var refinedTree = originalTreeData.getTree().stream()
                .map(this::filterJavaClassesFromNode)
                .filter(node -> node != null)
                .toList();
        
        return new RepositoryTreeData(
                originalTreeData.getRepositoryUrl(),
                originalTreeData.getBranch(),
                refinedTree,
                originalTreeData.getPlatform(),
                originalTreeData.getCommitSha()
        );
    }
}