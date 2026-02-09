package com.archpilot.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.archpilot.dto.RepositoryBranchesRequest;
import com.archpilot.dto.RepositoryTreeRequest;
import com.archpilot.dto.RepositoryVerificationRequest;
import com.archpilot.model.ApiResponse;
import com.archpilot.model.RepositoryBranchesData;
import com.archpilot.model.RepositoryInfo;
import com.archpilot.model.RepositoryTreeData;
import com.archpilot.service.RepositoryVerificationService;

import reactor.core.publisher.Mono;

@Component
public class RepositoryFacade {
    
    private static final Logger logger = LoggerFactory.getLogger(RepositoryFacade.class);
    
    @Autowired
    private RepositoryVerificationService repositoryVerificationService;
    
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
    
    public Mono<ApiResponse<RepositoryTreeData>> getRepositoryTree(RepositoryTreeRequest request) {
        logger.info("Processing repository tree request: {}", request.getRepositoryUrl());
        
        return repositoryVerificationService
                .getRepositoryTree(request.getRepositoryUrl(), request.getAccessToken(), 
                                 request.getBranch(), request.getPath(), request.getRecursive())
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
    
    public Mono<ApiResponse<RepositoryTreeData>> getRepositoryTree(String repositoryUrl, String accessToken, 
                                                                  String branch, String path, Boolean recursive) {
        RepositoryTreeRequest request = new RepositoryTreeRequest(repositoryUrl, accessToken);
        request.setBranch(branch);
        request.setPath(path);
        request.setRecursive(recursive);
        return getRepositoryTree(request);
    }
    
    private RepositoryInfo mapToRepositoryInfo(com.archpilot.dto.RepositoryVerificationResponse response) {
        if (response.getRepositoryInfo() == null) {
            return null;
        }
        
        var repoInfo = response.getRepositoryInfo();
        return new RepositoryInfo(
                repoInfo.getName(),
                repoInfo.getFullName(),
                repoInfo.getDescription(),
                repoInfo.getDefaultBranch(),
                repoInfo.isPrivate(),
                repoInfo.getLanguage(),
                repoInfo.getPlatform(),
                response.getRepositoryUrl()
        );
    }
    
    private RepositoryBranchesData mapToBranchesData(com.archpilot.dto.RepositoryBranchesResponse response) {
        if (response.getBranches() == null) {
            return null;
        }
        
        var branches = response.getBranches().stream()
                .map(branch -> new com.archpilot.model.BranchInfo(
                        branch.getName(),
                        branch.getSha(),
                        branch.isDefault(),
                        branch.isProtected(),
                        branch.getLastCommitMessage(),
                        branch.getLastCommitAuthor(),
                        branch.getLastCommitDate()
                ))
                .toList();
        
        return new RepositoryBranchesData(response.getRepositoryUrl(), branches, response.getPlatform());
    }
    
    private RepositoryTreeData mapToTreeData(com.archpilot.dto.RepositoryTreeResponse response) {
        if (response.getTree() == null) {
            return null;
        }
        
        var treeNodes = response.getTree().stream()
                .map(this::mapToTreeNode)
                .toList();
        
        return new RepositoryTreeData(response.getRepositoryUrl(), response.getBranch(), 
                                    response.getPath(), treeNodes, response.getPlatform());
    }
    
    private com.archpilot.model.TreeNode mapToTreeNode(com.archpilot.dto.RepositoryTreeResponse.TreeItem item) {
        com.archpilot.model.TreeNode node = new com.archpilot.model.TreeNode(
                item.getName(), item.getPath(), item.getType(), item.getSha(), 
                item.getSize(), item.getUrl(), item.getDownloadUrl()
        );
        
        if (item.getChildren() != null) {
            var childNodes = item.getChildren().stream()
                    .map(this::mapToTreeNode)
                    .toList();
            node.setChildren(childNodes);
        }
        
        return node;
    }
}