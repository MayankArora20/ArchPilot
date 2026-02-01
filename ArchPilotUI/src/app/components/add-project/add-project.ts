import { Component, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Api, RepositoryVerificationResponse, RepositoryBranchesResponse } from '../../services/api';

@Component({
  selector: 'app-add-project',
  imports: [FormsModule, CommonModule],
  templateUrl: './add-project.html',
  styleUrl: './add-project.scss',
})
export class AddProject implements OnDestroy {
  activeTab: 'repository' | 'requirement' = 'repository';
  gitUrl = '';
  projectName = '';
  loading = false;
  verifying = false;
  fetchingBranches = false;
  showErrorModal = false;
  errorReasons: string[] = [];
  verificationResult: RepositoryVerificationResponse | null = null;
  branchesResult: RepositoryBranchesResponse | null = null;
  showVerificationResult = false;
  selectedBranch = '';
  private verificationTimeout: any;

  constructor(
    private apiService: Api,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnDestroy(): void {
    if (this.verificationTimeout) {
      clearTimeout(this.verificationTimeout);
    }
  }

  onGitUrlChange(): void {
    // Reset verification result when URL changes
    this.verificationResult = null;
    this.branchesResult = null;
    this.showVerificationResult = false;
    this.selectedBranch = '';
    
    // Clear existing timeout
    if (this.verificationTimeout) {
      clearTimeout(this.verificationTimeout);
    }
    
    console.log('URL changed to:', this.gitUrl.trim());
  }

  private isValidRepositoryUrl(url: string): boolean {
    const githubPattern = /^https:\/\/github\.com\/[\w.-]+\/[\w.-]+\/?$/;
    const gitlabPattern = /^https:\/\/gitlab\.com\/[\w.-]+\/[\w.-]+\/?$/;
    return githubPattern.test(url) || gitlabPattern.test(url);
  }

  verifyRepository(): void {
    if (!this.gitUrl.trim()) {
      console.log('No URL provided for verification');
      return;
    }

    console.log('Starting repository verification for:', this.gitUrl.trim());
    
    this.verifying = true;
    this.verificationResult = null;
    this.branchesResult = null;
    this.showVerificationResult = false;
    this.selectedBranch = '';
    this.cdr.detectChanges();

    const request = { repositoryUrl: this.gitUrl.trim() };
    console.log('Sending verification request:', request);

    this.apiService.verifyRepository(request).subscribe({
      next: (response) => {
        console.log('Verification response received:', response);
        console.log('Response status:', response?.status);
        console.log('Response type:', typeof response?.status);
        
        this.verifying = false;
        this.verificationResult = response;
        this.showVerificationResult = true;
        this.cdr.detectChanges();
        
        // Always try to fetch branches if we got any response
        if (response) {
          console.log('Checking if should fetch branches...');
          console.log('Status check:', response.status);
          console.log('Status === "success":', response.status === 'success');
          
          if (response.status === 'success') {
            console.log('✅ Repository verified successfully, will fetch branches in 1 second...');
            setTimeout(() => {
              console.log('🔄 Now calling fetchBranches()...');
              this.fetchBranches();
            }, 1000);
          } else {
            console.log('❌ Repository verification failed. Status:', response.status, 'Message:', response.message);
          }
        } else {
          console.log('❌ No response received from verification API');
        }
      },
      error: (error) => {
        console.error('Verification error:', error);
        this.verifying = false;
        this.errorReasons = [`Failed to verify repository: ${error.message || 'Unknown error'}`];
        this.showErrorModal = true;
        this.cdr.detectChanges();
      }
    });
  }

  fetchBranches(): void {
    console.log('🔄 fetchBranches() called');
    console.log('Current verification result:', this.verificationResult);
    console.log('Verification status:', this.verificationResult?.status);
    
    if (!this.gitUrl.trim()) {
      console.log('❌ No URL available for branch fetch');
      return;
    }

    // For debugging, let's try to fetch branches regardless of verification status
    console.log('🚀 Starting branch fetch for:', this.gitUrl.trim());
    
    this.fetchingBranches = true;
    this.branchesResult = null;
    this.cdr.detectChanges();

    const request = { 
      repositoryUrl: this.gitUrl.trim(),
      limit: 50 
    };
    console.log('Sending branches request:', request);

    this.apiService.getRepositoryBranches(request).subscribe({
      next: (response) => {
        console.log('✅ Branches response received:', response);
        this.fetchingBranches = false;
        this.branchesResult = response;
        
        // Auto-select default branch if available
        if (response && response.status === 'success' && response.data?.branches && response.data.branches.length > 0) {
          const defaultBranch = response.data.branches.find(branch => branch.default);
          if (defaultBranch) {
            this.selectedBranch = defaultBranch.name;
            console.log('Auto-selected default branch:', defaultBranch.name);
          } else {
            this.selectedBranch = response.data.branches[0].name;
            console.log('Auto-selected first branch:', response.data.branches[0].name);
          }
        } else {
          console.log('No branches found or fetch failed:', response?.message);
        }
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('❌ Branches fetch error:', error);
        this.fetchingBranches = false;
        this.errorReasons = [`Failed to fetch repository branches: ${error.message || 'Unknown error'}`];
        this.showErrorModal = true;
        this.cdr.detectChanges();
      }
    });
  }

  analyzeRepository(): void {
    if (!this.gitUrl.trim()) {
      return;
    }

    // Check if repository is verified and branch is selected
    if (!this.verificationResult || this.verificationResult.status !== 'success') {
      this.errorReasons = ['Please verify the repository URL first.'];
      this.showErrorModal = true;
      return;
    }

    if (!this.selectedBranch) {
      this.errorReasons = ['Please select a branch to analyze.'];
      this.showErrorModal = true;
      return;
    }

    this.loading = true;
    // For now, we'll use the original analyze API. Later this can be enhanced to include branch info
    this.apiService.analyzeRepo(this.gitUrl).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.Error) {
          this.errorReasons = response.Error.reason;
          this.showErrorModal = true;
        } else {
          this.router.navigate(['/plantuml-viewer'], {
            queryParams: {
              projectName: response.projectName,
              pumlContent: response.pumlContent,
              branch: this.selectedBranch
            }
          });
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorReasons = ['Failed to analyze repository. Please try again.'];
        this.showErrorModal = true;
      }
    });
  }

  startRequirementChat(): void {
    if (!this.projectName.trim()) {
      return;
    }

    this.router.navigate(['/chat'], {
      queryParams: { projectName: this.projectName }
    });
  }

  closeErrorModal(): void {
    this.showErrorModal = false;
    this.errorReasons = [];
  }

  closeVerificationResult(): void {
    this.showVerificationResult = false;
  }

  // Manual branch fetch for debugging
  manualFetchBranches(): void {
    console.log('🔧 Manual branch fetch triggered');
    console.log('Current verification result:', this.verificationResult);
    console.log('Current URL:', this.gitUrl.trim());
    
    if (!this.gitUrl.trim()) {
      console.log('❌ No URL available for manual fetch');
      alert('Please enter a repository URL first');
      return;
    }
    
    // Force fetch branches regardless of verification status for testing
    this.fetchBranches();
  }

  // Direct branches test (bypass verification check)
  testBranchesDirectly(): void {
    if (!this.gitUrl.trim()) {
      alert('Please enter a repository URL first');
      return;
    }

    console.log('🧪 Testing branches API directly for:', this.gitUrl.trim());
    
    this.fetchingBranches = true;
    this.branchesResult = null;
    this.cdr.detectChanges();

    const request = { 
      repositoryUrl: this.gitUrl.trim(),
      limit: 10 
    };
    console.log('Direct branches request:', request);

    this.apiService.getRepositoryBranches(request).subscribe({
      next: (response) => {
        console.log('Direct branches response:', response);
        this.fetchingBranches = false;
        this.branchesResult = response;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Direct branches error:', error);
        this.fetchingBranches = false;
        this.errorReasons = [`Direct branches test failed: ${error.message || 'Unknown error'}`];
        this.showErrorModal = true;
        this.cdr.detectChanges();
      }
    });
  }
}
