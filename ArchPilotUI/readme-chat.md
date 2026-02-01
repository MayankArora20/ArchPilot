# Chat Functionality - Frontend Implementation

## Overview

The ArchPilot UI includes a fully functional chat interface that connects to the backend API for AI-powered requirement engineering conversations. The chat component manages sessions, displays conversation history, and provides a smooth user experience.

---

## Architecture

### Components

1. **Chat Component** (`src/app/components/chat/`)
   - Main chat interface
   - Message display and input
   - Session management UI

2. **Chat Service** (`src/app/services/chat.ts`)
   - API communication
   - Session state management
   - Message handling

---

## Chat Service

### Features

- **Session Management**: Automatic session initialization and tracking
- **Message Handling**: Send and receive messages with timestamps
- **State Management**: Track chat states (Active, Streaming, Error)
- **History Retrieval**: Load previous conversations
- **Session Clearing**: Reset conversations

### API Methods

#### `initSession(projectName: string): Observable<any>`

Initializes a new session or retrieves an existing one for a project.

```typescript
this.chatService.initSession('MyProject').subscribe({
  next: (response) => {
    console.log('Session ID:', response.sessionId);
  },
  error: (error) => {
    console.error('Failed to initialize session:', error);
  }
});
```

#### `sendMessage(projectName: string, message: string): Observable<any>`

Sends a message to the AI agent and receives a response.

```typescript
this.chatService.sendMessage('MyProject', 'I need authentication').subscribe({
  next: (response) => {
    console.log('AI Response:', response.response);
  },
  error: (error) => {
    console.error('Failed to send message:', error);
  }
});
```

#### `getHistory(projectName: string, sessionId: string): Observable<any>`

Retrieves the conversation history for a session.

```typescript
this.chatService.getHistory('MyProject', sessionId).subscribe({
  next: (response) => {
    console.log('History:', response.history);
  }
});
```

#### `clearSession(projectName: string, sessionId: string): Observable<any>`

Clears the current session and all its history.

```typescript
this.chatService.clearSession('MyProject', sessionId).subscribe({
  next: () => {
    console.log('Session cleared');
  }
});
```

### Observables

The service exposes three observables for reactive updates:

```typescript
// Listen for new messages
chatService.message$.subscribe(message => {
  console.log('New message:', message);
});

// Listen for state changes
chatService.state$.subscribe(state => {
  console.log('Chat state:', state);
});

// Listen for session updates
chatService.session$.subscribe(session => {
  console.log('Session info:', session);
});
```

---

## Chat Component

### Features

- **Real-time Messaging**: Send and receive messages instantly
- **Session Display**: Shows current session ID
- **Message History**: Displays all messages with timestamps
- **State Indicators**: Visual feedback for loading and errors
- **Session Management**: Clear session button
- **Welcome Message**: Helpful prompt when chat is empty

### Component Properties

```typescript
projectName: string;           // Current project name
messages: ChatMessage[];       // Array of chat messages
currentMessage: string;        // User's input text
chatState: ChatState;          // Current chat state
sessionInfo: SessionInfo;      // Current session information
errorMessage: string;          // Error message to display
```

### Component Methods

#### `initializeSession()`

Initializes a chat session when the component loads.

```typescript
initializeSession(): void {
  this.chatService.initSession(this.projectName).subscribe({
    next: (response) => {
      console.log('Session initialized:', response);
    },
    error: (error) => {
      this.errorMessage = 'Failed to initialize chat session.';
    }
  });
}
```

#### `sendMessage()`

Sends the user's message to the AI agent.

```typescript
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
      console.log('Message sent successfully');
    },
    error: (error) => {
      this.errorMessage = 'Failed to send message.';
    }
  });
}
```

#### `clearSession()`

Clears the current session and reinitializes.

```typescript
clearSession(): void {
  if (this.sessionInfo?.sessionId) {
    this.chatService.clearSession(this.projectName, this.sessionInfo.sessionId)
      .subscribe({
        next: () => {
          this.messages = [];
          this.initializeSession();
        }
      });
  }
}
```

---

## User Interface

### Chat Layout

```
┌─────────────────────────────────────────┐
│ AI Chat - Requirement Engineering       │
│ Project: MyProject                      │
│ Session: 550e8400... [🗑️]              │
├─────────────────────────────────────────┤
│                                         │
│ [Welcome Message]                       │
│                                         │
│ User: I need authentication             │
│ 1/30/26, 10:30 AM                      │
│                                         │
│ Assistant: I can help you design...    │
│ 1/30/26, 10:30 AM                      │
│                                         │
├─────────────────────────────────────────┤
│ [Type your message...        ] [Send]   │
└─────────────────────────────────────────┘
```

### States

1. **Active**: Ready to send messages
2. **Streaming**: Waiting for AI response (button shows "Sending...")
3. **Error**: Shows error message with retry button

### Styling

The chat component uses SCSS with CSS variables for theming:

```scss
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-color);
  color: var(--text-color);
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message {
  margin: 10px 0;
  padding: 10px 15px;
  border-radius: 8px;
  
  &.user {
    background: #007bff;
    color: white;
    margin-left: auto;
    max-width: 70%;
  }
  
  &.assistant {
    background: var(--menu-bg);
    max-width: 70%;
  }
}
```

---

## Navigation Flow

### Accessing the Chat

The chat component can be accessed through multiple routes:

1. **From Add Project (Requirements Tab)**
   ```
   /add-project → Requirements Tab → Chat
   ```

2. **From PlantUML Viewer**
   ```
   /plantuml-viewer → Add Requirement → Chat
   ```

3. **Direct Navigation**
   ```
   /chat?projectName=MyProject
   ```

### Query Parameters

The chat component accepts the following query parameter:

- `projectName`: The name of the project (required)

Example:
```typescript
this.router.navigate(['/chat'], {
  queryParams: { projectName: 'MyProject' }
});
```

---

## Data Models

### ChatMessage

```typescript
interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp?: number;
}
```

### SessionInfo

```typescript
interface SessionInfo {
  sessionId: string;
  projectName: string;
}
```

### ChatState

```typescript
enum ChatState {
  Active = 'active',
  Streaming = 'streaming',
  Completed = 'completed',
  Error = 'error'
}
```

---

## API Integration

### Backend Endpoints

The chat service communicates with these backend endpoints:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/addRequirement/{projectName}/init` | Initialize session |
| POST | `/api/addRequirement/{projectName}` | Send message |
| GET | `/api/addRequirement/{projectName}/history` | Get history |
| DELETE | `/api/addRequirement/{projectName}/session` | Clear session |

### Request/Response Examples

**Initialize Session:**
```typescript
// Request
POST /api/addRequirement/MyProject/init

// Response
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "projectName": "MyProject",
  "message": "Session initialized successfully"
}
```

**Send Message:**
```typescript
// Request
POST /api/addRequirement/MyProject
{
  "message": "I need authentication",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}

// Response
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "projectName": "MyProject",
  "response": "I can help you design an authentication system...",
  "timestamp": 1706630400000
}
```

---

## Configuration

### Proxy Configuration

The frontend uses a proxy to communicate with the backend API. Configuration in `proxy.conf.json`:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

### Running with Proxy

```bash
npm start
# or
ng serve --proxy-config proxy.conf.json
```

---

## Error Handling

### Common Errors

1. **Session Initialization Failed**
   - **Cause**: Backend not running or Redis unavailable
   - **Solution**: Verify backend is running on port 8080

2. **Message Send Failed**
   - **Cause**: Network error or invalid session
   - **Solution**: Check network connection and session validity

3. **Session Expired**
   - **Cause**: Session expired after 24 hours
   - **Solution**: Automatically creates new session

### Error Display

Errors are displayed in the UI with retry options:

```html
<div class="status error">
  {{ errorMessage }} 
  <button (click)="retry()">Retry</button>
</div>
```

---

## Testing

### Manual Testing

1. Start the backend server
2. Start the frontend: `npm start`
3. Navigate to `/chat?projectName=TestProject`
4. Send a message and verify response
5. Check session ID is displayed
6. Clear session and verify it resets

### Testing Checklist

- [ ] Session initializes on component load
- [ ] Messages are sent and received correctly
- [ ] Session ID is displayed in header
- [ ] Clear session button works
- [ ] Error messages display correctly
- [ ] Retry button works after errors
- [ ] Messages show timestamps
- [ ] Welcome message appears when empty
- [ ] Input is disabled while streaming
- [ ] Send button is disabled when input is empty

---

## Future Enhancements

### Planned Features

1. **Message Formatting**
   - Markdown support
   - Code syntax highlighting
   - Link detection

2. **Enhanced UX**
   - Typing indicators
   - Message editing
   - Message deletion
   - Copy message content

3. **Advanced Features**
   - File attachments
   - Voice input
   - Export conversation
   - Search history

4. **Collaboration**
   - Multi-user chat
   - Shared sessions
   - User presence indicators

---

## Troubleshooting

### Chat Not Loading

**Problem:** Chat component shows blank screen

**Solution:**
1. Check browser console for errors
2. Verify backend is running: `http://localhost:8080/api/addRequirement/test/init`
3. Check proxy configuration in `proxy.conf.json`

### Messages Not Sending

**Problem:** Messages don't appear or get stuck

**Solution:**
1. Check network tab in browser DevTools
2. Verify session ID is valid
3. Check backend logs for errors
4. Try clearing session and reinitializing

### Session Not Persisting

**Problem:** Session resets on page refresh

**Solution:**
- This is expected behavior currently
- Future enhancement: Store session ID in localStorage
- Workaround: Keep the chat tab open

---

## Development

### File Structure

```
src/app/
├── components/
│   └── chat/
│       ├── chat.ts          # Component logic
│       ├── chat.html        # Template
│       └── chat.scss        # Styles
└── services/
    └── chat.ts              # Chat service
```

### Adding New Features

1. Update the service in `services/chat.ts`
2. Update the component in `components/chat/chat.ts`
3. Update the template in `components/chat/chat.html`
4. Update styles in `components/chat/chat.scss`
5. Test thoroughly

### Code Style

- Use TypeScript strict mode
- Follow Angular style guide
- Use RxJS for async operations
- Implement proper error handling
- Add comments for complex logic

---

## Performance Considerations

### Optimizations

1. **Message Rendering**: Uses Angular's `@for` with track by index
2. **Subscription Management**: All subscriptions are properly unsubscribed
3. **State Management**: Uses BehaviorSubject for state caching
4. **API Calls**: Debouncing can be added for rapid message sending

### Best Practices

- Unsubscribe from observables in `ngOnDestroy`
- Use `async` pipe where possible
- Avoid unnecessary re-renders
- Implement virtual scrolling for long conversations (future)

---

## Accessibility

### Current Implementation

- Keyboard navigation (Enter to send)
- Disabled states for buttons
- Clear visual feedback for states

### Future Improvements

- ARIA labels for screen readers
- Keyboard shortcuts
- Focus management
- High contrast mode support

---

## Contact & Support

For questions or issues related to the chat UI:
1. Check browser console for errors
2. Verify backend connectivity
3. Review this documentation
4. Check the backend readme-chat.md

---

**Last Updated:** January 30, 2026
**Version:** 1.0.0
**Status:** Development - Basic functionality implemented
