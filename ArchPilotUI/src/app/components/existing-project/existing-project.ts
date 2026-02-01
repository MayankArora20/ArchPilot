import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Api } from '../../services/api';

@Component({
  selector: 'app-existing-project',
  imports: [FormsModule, CommonModule],
  templateUrl: './existing-project.html',
  styleUrl: './existing-project.scss',
})
export class ExistingProject implements OnInit {
  projects: string[] = [];
  selectedProject = '';
  loading = false;
  showErrorModal = false;
  errorReasons: string[] = [];

  constructor(
    private apiService: Api,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.apiService.getProjects().subscribe({
      next: (response) => {
        if (response.Projects) {
          this.projects = response.Projects;
        }
      },
      error: (error) => {
        this.errorReasons = ['Failed to load projects. Please try again.'];
        this.showErrorModal = true;
      }
    });
  }

  loadProject(): void {
    if (!this.selectedProject) {
      return;
    }

    this.loading = true;
    this.apiService.getProject(this.selectedProject).subscribe({
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
        this.errorReasons = ['Failed to load project. Please try again.'];
        this.showErrorModal = true;
      }
    });
  }

  closeErrorModal(): void {
    this.showErrorModal = false;
    this.errorReasons = [];
  }
}
