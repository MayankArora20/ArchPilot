# Repository Verification Integration

## Overview
The Add New Project component now includes automatic repository verification and branch selection functionality that validates GitHub and GitLab repository URLs and fetches available branches before allowing analysis.

## Features

### Automatic Verification
- When a user pastes a valid GitHub or GitLab URL, the system automatically verifies the repository
- Verification happens in real-time as the user types or pastes the URL
- Only URLs matching the pattern `https://github.com/user/repo` or `https://gitlab.com/user/repo` trigger automatic verification

### Automatic Branch Fetching
- After successful repository verification, the system automatically fetches all available branches
- Displays branch count and detailed information for each branch
- Auto-selects the default branch or first available branch

### Branch Selection
- Users can select from a dropdown of all available branches
- Shows branch status indicators (default, protected)
- Displays detailed branch information including:
  - Branch name and SHA
  - Last commit message and author
  - Branch status (default/protected/regular)

### Verification Results
When verification is successful, the system displays:
- Repository name and full name
- Description (if available)
- Default branch
- Primary language
- Platform (GitHub/GitLab)
- Visibility (Public/Private)

### Error Handling
- Invalid URLs show appropriate error messages
- Network errors are handled gracefully
- Branch fetching errors are displayed separately
- Users cannot proceed to analysis without successful verification and branch selection

### UI Components

#### Verification Status
- Loading spinner during verification
- Green checkmark for verified repositories
- Red X for failed verification
- Detailed repository information display

#### Branch Selection
- Loading spinner during branch fetching
- Dropdown with all available branches
- Branch status indicators (🔒 for protected, (default) label)
- Detailed branch information panel showing:
  - Branch name and short SHA
  - Last commit message and author
  - Status badges (Default/Protected/Regular)

#### Button States
- "Verify Repository" button for manual verification
- "Analyze Repository" button only enabled after successful verification and branch selection
- Loading states for verification, branch fetching, and analysis

## API Integration

### Endpoints
- `POST /api/repository/verify` - Repository verification
- `POST /api/repository/branches` - Branch fetching

### Repository Verification
- Request: `{ repositoryUrl: string, accessToken?: string }`
- Response: `RepositoryVerificationResponse` with status, message, and repository info

### Branch Fetching
- Request: `{ repositoryUrl: string, accessToken?: string, limit?: number }`
- Response: `RepositoryBranchesResponse` with branches array and metadata

### Response Formats
```typescript
interface RepositoryVerificationResponse {
  status: string; // "Verified" or "Error"
  message: string;
  repositoryUrl?: string;
  repositoryInfo?: {
    name: string;
    fullName: string;
    description: string;
    defaultBranch: string;
    private: boolean;
    language: string;
    platform: string;
  };
  timestamp: string;
}

interface RepositoryBranchesResponse {
  status: string; // "Success" or "Error"
  message: string;
  repositoryUrl?: string;
  branches?: BranchInfo[];
  totalBranches?: number;
  platform?: string;
  timestamp: string;
}

interface BranchInfo {
  name: string;
  sha: string;
  default: boolean;
  protected: boolean;
  lastCommitMessage?: string;
  lastCommitAuthor?: string;
  lastCommitDate?: string;
}
```

## User Experience

1. User navigates to "Add New Project" → "Repository Link"
2. User pastes a GitHub/GitLab URL
3. System automatically verifies the repository
4. Verification result is displayed with repository details
5. System automatically fetches available branches
6. User selects desired branch from dropdown
7. Branch details are displayed for the selected branch
8. User can proceed to analyze the repository only if verification succeeds and branch is selected

## Technical Implementation

### Components Modified
- `AddProjectComponent`: Added verification logic, branch fetching, and selection UI
- `ApiService`: Added `verifyRepository()` and `getRepositoryBranches()` methods
- Added TypeScript interfaces for all request/response types

### Styling
- Added verification status indicators
- Repository information display grid
- Branch selection dropdown and information panel
- Loading animations and state management
- Color-coded success/error states
- Status badges for branch types

## Future Enhancements
- Support for private repositories with access tokens
- Batch verification for multiple repositories
- Repository and branch caching to avoid repeated API calls
- Integration with Git provider APIs for enhanced metadata
- Branch comparison and diff viewing
- Recent branches and favorites