import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ProjectResponse {
  Projects?: string[];
  Error?: { reason: string[] };
}

export interface PlantUMLResponse {
  projectName: string;
  pumlContent: string;
  Error?: { reason: string[] };
}

export interface RepositoryVerificationRequest {
  repositoryUrl: string;
  accessToken?: string;
}

export interface RepositoryInfo {
  name: string;
  fullName: string;
  description: string;
  defaultBranch: string;
  private: boolean;
  language: string;
  platform: string;
}

export interface RepositoryVerificationResponse {
  status: string; // "success" or "error"
  message: string;
  data?: RepositoryInfo;
  timestamp?: string;
}

export interface RepositoryBranchesRequest {
  repositoryUrl: string;
  accessToken?: string;
  limit?: number;
}

export interface BranchInfo {
  name: string;
  sha: string;
  default: boolean;
  protected: boolean;
  lastCommitMessage?: string;
  lastCommitAuthor?: string;
  lastCommitDate?: string;
}

export interface RepositoryBranchesResponse {
  status: string; // "success" or "error"
  message: string;
  data?: {
    repositoryUrl?: string;
    branches?: BranchInfo[];
    totalBranches?: number;
    platform?: string;
  };
  timestamp?: string;
}

@Injectable({
  providedIn: 'root',
})
export class Api {
  private baseUrl = '/api';

  constructor(private http: HttpClient) {}

  analyzeRepo(gitUrl: string): Observable<PlantUMLResponse> {
    return this.http.post<PlantUMLResponse>(`${this.baseUrl}/project/analyze-repo`, { gitUrl });
  }

  verifyRepository(request: RepositoryVerificationRequest): Observable<RepositoryVerificationResponse> {
    return this.http.post<RepositoryVerificationResponse>(`${this.baseUrl}/repository/verify`, request);
  }

  getRepositoryBranches(request: RepositoryBranchesRequest): Observable<RepositoryBranchesResponse> {
    // Use GET endpoint with query parameters instead of POST
    let params = new HttpParams().set('url', request.repositoryUrl);
    
    if (request.accessToken) {
      params = params.set('token', request.accessToken);
    }
    if (request.limit) {
      params = params.set('limit', request.limit.toString());
    }
    
    return this.http.get<RepositoryBranchesResponse>(`${this.baseUrl}/repository/branches`, { params });
  }

  getProjects(): Observable<ProjectResponse> {
    return this.http.get<ProjectResponse>(`${this.baseUrl}/projects`);
  }

  getProject(projectName: string): Observable<PlantUMLResponse> {
    return this.http.get<PlantUMLResponse>(`${this.baseUrl}/project/${projectName}`);
  }
}
