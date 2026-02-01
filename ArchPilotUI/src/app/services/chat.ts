import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, BehaviorSubject } from 'rxjs';

export enum ChatState {
  Active = 'active',
  Streaming = 'streaming',
  Completed = 'completed',
  Error = 'error'
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp?: number;
}

export interface SessionInfo {
  sessionId: string;
  projectName: string;
}

@Injectable({
  providedIn: 'root',
})
export class Chat {
  private apiUrl = '/api/addRequirement';
  private sessionInfo: SessionInfo | null = null;
  
  private messageSubject = new Subject<ChatMessage>();
  private stateSubject = new BehaviorSubject<ChatState>(ChatState.Active);
  private sessionSubject = new BehaviorSubject<SessionInfo | null>(null);

  message$ = this.messageSubject.asObservable();
  state$ = this.stateSubject.asObservable();
  session$ = this.sessionSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Initialize a session for a project
   */
  initSession(projectName: string): Observable<any> {
    return new Observable(observer => {
      this.http.post<any>(`${this.apiUrl}/${projectName}/init`, {})
        .subscribe({
          next: (response) => {
            this.sessionInfo = {
              sessionId: response.sessionId,
              projectName: response.projectName
            };
            this.sessionSubject.next(this.sessionInfo);
            this.stateSubject.next(ChatState.Active);
            observer.next(response);
            observer.complete();
          },
          error: (error) => {
            console.error('Error initializing session:', error);
            this.stateSubject.next(ChatState.Error);
            observer.error(error);
          }
        });
    });
  }

  /**
   * Send a message to the chat agent
   */
  sendMessage(projectName: string, message: string): Observable<any> {
    return new Observable(observer => {
      this.stateSubject.next(ChatState.Streaming);

      const payload: any = { message };
      if (this.sessionInfo?.sessionId) {
        payload.sessionId = this.sessionInfo.sessionId;
      }

      this.http.post<any>(`${this.apiUrl}/${projectName}`, payload)
        .subscribe({
          next: (response) => {
            // Update session info if new session was created
            if (response.sessionId && response.sessionId !== this.sessionInfo?.sessionId) {
              this.sessionInfo = {
                sessionId: response.sessionId,
                projectName: response.projectName
              };
              this.sessionSubject.next(this.sessionInfo);
            }

            // Emit the assistant's response
            const assistantMessage: ChatMessage = {
              role: 'assistant',
              content: response.response,
              timestamp: response.timestamp
            };
            this.messageSubject.next(assistantMessage);
            this.stateSubject.next(ChatState.Active);
            
            observer.next(response);
            observer.complete();
          },
          error: (error) => {
            console.error('Error sending message:', error);
            this.stateSubject.next(ChatState.Error);
            observer.error(error);
          }
        });
    });
  }

  /**
   * Get session history
   */
  getHistory(projectName: string, sessionId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${projectName}/history`, {
      params: { sessionId }
    });
  }

  /**
   * Clear the current session
   */
  clearSession(projectName: string, sessionId: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${projectName}/session`, {
      params: { sessionId }
    });
  }

  /**
   * Get current session info
   */
  getSessionInfo(): SessionInfo | null {
    return this.sessionInfo;
  }

  /**
   * Reset the chat service
   */
  reset(): void {
    this.sessionInfo = null;
    this.sessionSubject.next(null);
    this.stateSubject.next(ChatState.Active);
  }
}
