# Repository Tree Structure API Guide

This guide explains how to use the new Repository Tree Structure API that retrieves the file and directory structure from GitHub and GitLab repositories.

## API Endpoints

### 1. POST /api/repository/tree
Retrieves repository tree structure using JSON request body.

**Request Body:**
```json
{
  "repositoryUrl": "https://github.com/owner/repo",
  "accessToken": "optional_access_token",
  "branch": "optional_branch_name",
  "path": "optional/path/within/repo",
  "recursive": false
}
```

**Example:**
```bash
curl -X POST "http://localhost:8080/api/repository/tree" \
  -H "Content-Type: application/json" \
  -d '{
    "repositoryUrl": "https://github.com/spring-projects/spring-boot",
    "path": "spring-boot-project",
    "recursive": false
  }'
```

### 2. GET /api/repository/tree
Retrieves repository tree structure using URL parameters.

**Parameters:**
- `url` (required): Repository URL
- `token` (optional): Access token for private repositories
- `branch` (optional): Branch name (defaults to default branch)
- `path` (optional): Path within repository (defaults to root)
- `recursive` (optional): Fetch tree recursively (default: false)

**Example:**
```bash
curl -X GET "http://localhost:8080/api/repository/tree?url=https://github.com/spring-projects/spring-boot&path=spring-boot-project&recursive=false"
```

## Response Format

**Success Response:**
```json
{
  "status": "SUCCESS",
  "message": "Tree structure retrieved successfully",
  "data": {
    "repositoryUrl": "https://github.com/owner/repo",
    "branch": "main",
    "path": "optional/path",
    "platform": "GitHub",
    "tree": [
      {
        "name": "file.txt",
        "path": "path/to/file.txt",
        "type": "file",
        "sha": "abc123...",
        "size": 1024,
        "url": "https://api.github.com/repos/owner/repo/contents/path/to/file.txt",
        "downloadUrl": "https://raw.githubusercontent.com/owner/repo/main/path/to/file.txt",
        "children": null
      },
      {
        "name": "directory",
        "path": "path/to/directory",
        "type": "dir",
        "sha": "def456...",
        "size": null,
        "url": "https://api.github.com/repos/owner/repo/contents/path/to/directory",
        "downloadUrl": null,
        "children": [
          // Child items when recursive=true
        ]
      }
    ]
  },
  "timestamp": "2024-01-01T12:00:00"
}
```

**Error Response:**
```json
{
  "status": "ERROR",
  "message": "Error description",
  "data": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

## Supported Platforms

- **GitHub**: `https://github.com/owner/repo`
- **GitLab**: `https://gitlab.com/owner/repo`

## Authentication

### Public Repositories
- No authentication required for most public repositories
- Some operations may require authentication due to API rate limits

### Private Repositories
- Access token is required
- **GitHub**: Use Personal Access Token with `repo` scope
- **GitLab**: Use Personal Access Token with `read_repository` scope

### Getting Access Tokens

#### GitHub:
1. Go to GitHub.com → Settings → Developer settings
2. Personal access tokens → Tokens (classic)
3. Generate new token with `repo` scope
4. Copy the token and use it in the `accessToken` field

#### GitLab:
1. Go to GitLab.com → User Settings → Access Tokens
2. Create token with `read_repository` scope
3. Copy the token and use it in the `accessToken` field

## Usage Examples

### 1. Get Root Directory Structure
```bash
curl -X GET "http://localhost:8080/api/repository/tree?url=https://github.com/spring-projects/spring-boot"
```

### 2. Get Specific Directory
```bash
curl -X GET "http://localhost:8080/api/repository/tree?url=https://github.com/spring-projects/spring-boot&path=spring-boot-project"
```

### 3. Get Recursive Structure (Use with Caution)
```bash
curl -X GET "http://localhost:8080/api/repository/tree?url=https://github.com/spring-projects/spring-boot&path=spring-boot-project/spring-boot-starters&recursive=true"
```

### 4. Use Specific Branch
```bash
curl -X GET "http://localhost:8080/api/repository/tree?url=https://github.com/spring-projects/spring-boot&branch=2.7.x&path=spring-boot-project"
```

### 5. Private Repository with Token
```bash
curl -X POST "http://localhost:8080/api/repository/tree" \
  -H "Content-Type: application/json" \
  -d '{
    "repositoryUrl": "https://github.com/owner/private-repo",
    "accessToken": "ghp_your_token_here",
    "path": "src"
  }'
```

## Tree Item Types

- **file**: Regular file
- **dir**: Directory/folder

## Important Notes

### Performance Considerations
- **Recursive fetching** can be slow for large repositories
- Use `recursive=true` only when necessary and preferably on smaller directories
- Consider using pagination for large directory listings

### Rate Limits
- GitHub API: 60 requests/hour for unauthenticated, 5000/hour for authenticated
- GitLab API: Similar limits apply
- Use access tokens to increase rate limits

### Error Handling
Common error scenarios:
- Repository not found (404)
- Access forbidden (403) - usually requires authentication
- Invalid branch or path (404)
- Rate limit exceeded (403)
- Network timeouts

### Best Practices
1. Always handle error responses appropriately
2. Use authentication tokens when possible
3. Cache results when appropriate to reduce API calls
4. Be mindful of recursive operations on large repositories
5. Specify branch names when working with specific versions

## Integration with Existing APIs

This tree structure API complements the existing repository APIs:

1. **Repository Verification** (`/api/repository/verify`) - Verify repository exists and get basic info
2. **Repository Branches** (`/api/repository/branches`) - Get list of branches
3. **Repository Tree** (`/api/repository/tree`) - Get file/directory structure

Typical workflow:
1. Verify repository accessibility
2. Get available branches
3. Fetch tree structure for specific branch/path
4. Use tree information to navigate or process repository content

## Testing

Use the provided test script `test-repository-tree-api.bat` to test the API endpoints with various scenarios.

```bash
# Run the test script
./test-repository-tree-api.bat
```

The test script includes examples for:
- Root directory listing
- Specific path listing
- Recursive structure fetching
- POST requests with JSON body
- Branch-specific requests