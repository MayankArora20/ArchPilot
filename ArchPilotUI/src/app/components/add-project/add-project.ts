import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Api } from '../../services/api';

@Component({
  selector: 'app-add-project',
  imports: [FormsModule, CommonModule],
  templateUrl: './add-project.html',
  styleUrl: './add-project.scss',
})
export class AddProject {
  activeTab: 'repository' | 'requirement' = 'repository';
  gitUrl = '';
  projectName = '';
  loading = false;
  showErrorModal = false;
  errorReasons: string[] = [];

  constructor(
    private apiService: Api,
    private router: Router
  ) {}

  analyzeRepository(): void {
    if (!this.gitUrl.trim()) {
      return;
    }

    this.loading = true;
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
              pumlContent: response.pumlContent
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
}
