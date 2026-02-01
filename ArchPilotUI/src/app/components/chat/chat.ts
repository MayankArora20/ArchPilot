import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Chat as ChatService, ChatState, ChatMessage, SessionInfo } from '../../services/chat';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat',
  imports: [FormsModule, CommonModule],
  templateUrl: './chat.html',
  styleUrl: './chat.scss',
})
export class Chat implements OnInit, OnDestroy {
  projectName = '';
  messages: ChatMessage[] = [];
  currentMessage = '';
  chatState = ChatState.Active;
  ChatState = ChatState;
  showModal = false;
  sessionInfo: SessionInfo | null = null;
  errorMessage = '';
  
  private subscriptions: Subscription[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private chatService: ChatService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.projectName = params['projectName'] || 'New Project';
      this.initializeSession();
    });

    this.subscriptions.push(
      this.chatService.state$.subscribe(state => {
        this.chatState = state;
      })
    );

    this.subscriptions.push(
      this.chatService.message$.subscribe(message => {
        this.messages.push(message);
      })
    );

    this.subscriptions.push(
      this.chatService.session$.subscribe(session => {
        this.sessionInfo = session;
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initializeSession(): void {
    this.chatService.initSession(this.projectName).subscribe({
      next: (response) => {
        console.log('Session initialized:', response);
      },
      error: (error) => {
        console.error('Failed to initialize session:', error);
        this.errorMessage = 'Failed to initialize chat session. Please try again.';
      }
    });
  }

  sendMessage(): void {
    if (!this.currentMessage.trim() || this.chatState === ChatState.Streaming) {
      return;
    }

    const userMessage: ChatMessage = {
      role: 'user',
      content: this.currentMessage,
      timestamp: Date.now()
    };
    
    this.messages.push(userMessage);
    const messageToSend = this.currentMessage;
    this.currentMessage = '';

    this.chatService.sendMessage(this.projectName, messageToSend).subscribe({
      next: (response) => {
        console.log('Message sent successfully:', response);
      },
      error: (error) => {
        console.error('Failed to send message:', error);
        this.errorMessage = 'Failed to send message. Please try again.';
        this.chatState = ChatState.Error;
      }
    });
  }

  clearSession(): void {
    if (this.sessionInfo?.sessionId) {
      this.chatService.clearSession(this.projectName, this.sessionInfo.sessionId).subscribe({
        next: () => {
          this.messages = [];
          this.initializeSession();
        },
        error: (error) => {
          console.error('Failed to clear session:', error);
        }
      });
    }
  }

  closeModal(): void {
    this.showModal = false;
    this.router.navigate(['/plantuml-viewer'], {
      queryParams: { projectName: this.projectName }
    });
  }

  retry(): void {
    this.errorMessage = '';
    this.chatState = ChatState.Active;
  }
}
