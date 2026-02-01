import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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

@Injectable({
  providedIn: 'root',
})
export class Api {
  private baseUrl = '/api';

  constructor(private http: HttpClient) {}

  analyzeRepo(gitUrl: string): Observable<PlantUMLResponse> {
    return this.http.post<PlantUMLResponse>(`${this.baseUrl}/project/analyze-repo`, { gitUrl });
  }

  getProjects(): Observable<ProjectResponse> {
    return this.http.get<ProjectResponse>(`${this.baseUrl}/projects`);
  }

  getProject(projectName: string): Observable<PlantUMLResponse> {
    return this.http.get<PlantUMLResponse>(`${this.baseUrl}/project/${projectName}`);
  }
}
