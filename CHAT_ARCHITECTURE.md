# Chat Architecture Overview

## System Components

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ArchPilot Chat System                        │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────────────┐         ┌──────────────────────────────┐
│      Frontend (UI)       │         │      Backend (API)           │
│    Angular 21.1.1        │         │    Spring Boot 3.4.1         │
│    Port: 4200            │         │    Port: 8080                │
└──────────────────────────┘         └──────────────────────────────┘
         │                                      │
         │  1. Initialize Session               │
         │─────────────────────────────────────▶│
         │  POST /api/addRequirement/           │
         │       {projectName}/init             │
         │                                      │
         │  2. Session Created                  │
         │◀─────────────────────────────────────│
         │  { sessionId, projectName }          │
         │                                      │
         │  3. Send Message                     │
         │─────────────────────────────────────▶│
         │  POST /api/addRequirement/           │
         │       {projectName}                  │
         │  { message, sessionId }              │
         │                                      │
         │                                      │  4. Store Message
         │                                      │─────────────────▶
         │                                      │                 │
         │                                      │  5. Get AI      │
         │                                      │     Response    │
         │                                      │─────────────────┼──────┐
         │                                      │                 │      │
         │                                      │◀────────────────┼──────┘
         │                                      │  6. Store       │
         │                                      │     Response    │
         │                                      │─────────────────▶
         │  7. Return Response                  │                 │
         │◀─────────────────────────────────────│                 │
         │  { response, sessionId, timestamp }  │                 │
         │                                      │                 │
                                                              ┌────▼────┐
                                                              │  Redis  │
                                                              │  Cache  │
                                                              └─────────┘
                                                                   │
                                                              ┌────▼────────┐
                                                              │  Vertex AI  │
                                                              │   (Gemini)  │
                                                              └─────────────┘
```

---

## Component Details

### Frontend Components

```
ArchPilotUI/src/app/
│
├── components/chat/
│   ├── chat.ts              # Component logic
│   ├── chat.html            # UI template
│   └── chat.scss            # Styling
│
└── services/
    └── chat.ts              # API communication service
```

**Responsibilities:**
- Display chat interface
- Manage user input
- Handle session state
- Display messages with timestamps
- Error handling and retry logic

---

### Backend Components

```
ArchPilotBackend/src/main/java/com/archpilot/
│
├── controller/
│   └── RequirementController.java    # REST API endpoints
│
└── service/
    ├── SessionService.java           # Session management
    └── ChatAgentService.java         # AI agent
```

**Responsibilities:**
- Handle HTTP requests
- Manage sessions in Redis
- Process messages with AI
- Store conversation history
- Handle errors and validation

---

## Data Flow

### 1. Session Initialization

```
User Opens Chat
      │
      ▼
Frontend: initSession(projectName)
      │
      ▼
Backend: POST /api/addRequirement/{projectName}/init
      │
      ▼
SessionService: getOrCreateSession(projectName)
      │
      ├─▶ Check Redis for existing session
      │   └─▶ chat:project:{projectName}
      │
      ├─▶ If not found, create new UUID
      │
      ├─▶ Store session metadata
      │   └─▶ chat:session:{sessionId}
      │       ├─ projectName
      │       └─ createdAt
      │
      └─▶ Link project to session
          └─▶ chat:project:{projectName} = sessionId
      
Return: { sessionId, projectName, message }
```

### 2. Message Exchange

```
User Types Message
      │
      ▼
Frontend: sendMessage(projectName, message)
      │
      ▼
Backend: POST /api/addRequirement/{projectName}
      │
      ▼
SessionService: Validate/Create Session
      │
      ▼
SessionService: addMessage(sessionId, "user", message)
      │
      └─▶ Redis: RPUSH chat:session:{sessionId}:history
      
      ▼
ChatAgentService: chat(message)
      │
      └─▶ Vertex AI: Generate response
      
      ▼
SessionService: addMessage(sessionId, "assistant", response)
      │
      └─▶ Redis: RPUSH chat:session:{sessionId}:history
      
Return: { sessionId, projectName, response, timestamp }
      │
      ▼
Frontend: Display message in UI
```

### 3. History Retrieval

```
User Requests History
      │
      ▼
Frontend: getHistory(projectName, sessionId)
      │
      ▼
Backend: GET /api/addRequirement/{projectName}/history
      │
      ▼
SessionService: getHistory(sessionId)
      │
      └─▶ Redis: LRANGE chat:session:{sessionId}:history 0 -1
      
Return: { sessionId, projectName, history: [...] }
```

### 4. Session Clearing

```
User Clicks Clear Button
      │
      ▼
Frontend: clearSession(projectName, sessionId)
      │
      ▼
Backend: DELETE /api/addRequirement/{projectName}/session
      │
      ▼
SessionService: clearSession(sessionId)
      │
      ├─▶ Redis: DEL chat:session:{sessionId}
      ├─▶ Redis: DEL chat:session:{sessionId}:history
      └─▶ Redis: DEL chat:project:{projectName}
      
Return: { message: "Session cleared successfully" }
      │
      ▼
Frontend: Clear messages and reinitialize
```

---

## Redis Data Structure

### Session Metadata
```
Key: chat:session:{sessionId}
Type: Hash
Fields:
  - projectName: "MyProject"
  - createdAt: 1706630400000
TTL: 24 hours
```

### Project-Session Mapping
```
Key: chat:project:{projectName}
Type: String
Value: {sessionId}
TTL: 24 hours
```

### Conversation History
```
Key: chat:session:{sessionId}:history
Type: List
Values:
  - "user: I need authentication"
  - "assistant: I can help you design..."
  - "user: What about OAuth?"
  - "assistant: OAuth is a great choice..."
TTL: 24 hours
```

---

## API Endpoints Summary

| Method | Endpoint | Purpose | Request Body | Response |
|--------|----------|---------|--------------|----------|
| POST | `/api/addRequirement/{projectName}/init` | Initialize session | None | `{ sessionId, projectName, message }` |
| POST | `/api/addRequirement/{projectName}` | Send message | `{ message, sessionId? }` | `{ sessionId, projectName, response, timestamp }` |
| GET | `/api/addRequirement/{projectName}/history` | Get history | Query: `sessionId` | `{ sessionId, projectName, history[] }` |
| DELETE | `/api/addRequirement/{projectName}/session` | Clear session | Query: `sessionId` | `{ message }` |

---

## State Management

### Frontend States

```typescript
enum ChatState {
  Active = 'active',        // Ready to send messages
  Streaming = 'streaming',  // Waiting for AI response
  Completed = 'completed',  // Chat completed (future use)
  Error = 'error'          // Error occurred
}
```

### State Transitions

```
Initial State: Active
      │
      ▼
User Sends Message
      │
      ▼
State: Streaming
      │
      ├─▶ Success ──▶ State: Active
      │
      └─▶ Error ────▶ State: Error
                          │
                          ▼
                    User Clicks Retry
                          │
                          ▼
                    State: Active
```

---

## Session Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│                     Session Lifecycle                        │
└─────────────────────────────────────────────────────────────┘

1. Creation
   ├─ User opens chat
   ├─ Frontend calls /init
   ├─ Backend generates UUID
   ├─ Redis stores session data
   └─ TTL set to 24 hours

2. Active Use
   ├─ User sends messages
   ├─ Messages stored in Redis
   ├─ AI generates responses
   ├─ TTL refreshed on each interaction
   └─ Session remains active

3. Expiration (Automatic)
   ├─ 24 hours of inactivity
   ├─ Redis automatically deletes keys
   └─ Next request creates new session

4. Manual Clearing
   ├─ User clicks clear button
   ├─ Frontend calls DELETE endpoint
   ├─ Backend deletes all session data
   └─ New session initialized
```

---

## Security Considerations

### Current Implementation (Development)

```
┌─────────────────────────────────────────────────────────────┐
│                    Security Status                           │
├─────────────────────────────────────────────────────────────┤
│ ⚠️  CORS: Enabled for all origins                           │
│ ⚠️  Authentication: Not implemented                         │
│ ⚠️  Redis: No password                                      │
│ ⚠️  HTTPS: Not enforced                                     │
│ ⚠️  Rate Limiting: Not implemented                          │
└─────────────────────────────────────────────────────────────┘
```

### Production Requirements

```
┌─────────────────────────────────────────────────────────────┐
│              Production Security Checklist                   │
├─────────────────────────────────────────────────────────────┤
│ ✅ Enable JWT authentication                                │
│ ✅ Restrict CORS to specific origins                        │
│ ✅ Use Redis password authentication                        │
│ ✅ Enforce HTTPS only                                       │
│ ✅ Implement rate limiting                                  │
│ ✅ Add request validation                                   │
│ ✅ Enable session timeout policies                          │
│ ✅ Implement audit logging                                  │
└─────────────────────────────────────────────────────────────┘
```

---

## Performance Characteristics

### Response Times (Typical)

```
Session Initialization:  50-100ms
Message Send:           1-3 seconds (AI processing)
History Retrieval:      10-50ms
Session Clear:          10-30ms
```

### Scalability

```
┌─────────────────────────────────────────────────────────────┐
│                    Scalability Factors                       │
├─────────────────────────────────────────────────────────────┤
│ Redis:           Can handle 100k+ sessions                  │
│ Spring Boot:     Stateless, horizontally scalable          │
│ Vertex AI:       Rate limited by GCP quotas                │
│ Bottleneck:      AI response generation (1-3s)             │
└─────────────────────────────────────────────────────────────┘
```

---

## Error Handling

### Frontend Error Handling

```typescript
sendMessage() {
  this.chatService.sendMessage(projectName, message).subscribe({
    next: (response) => {
      // Success: Display message
    },
    error: (error) => {
      // Error: Show error message and retry button
      this.errorMessage = 'Failed to send message';
      this.chatState = ChatState.Error;
    }
  });
}
```

### Backend Error Handling

```java
@PostMapping("/addRequirement/{projectName}")
public ResponseEntity<Map<String, Object>> addRequirement(...) {
    try {
        // Process request
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        log.error("Error processing requirement", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process requirement"));
    }
}
```

---

## Monitoring & Logging

### Backend Logs

```
INFO  - Initializing session for project: MyProject
INFO  - Created session 550e8400... for project MyProject
DEBUG - Processing chat message: I need authentication
DEBUG - Generated response: I can help you design...
INFO  - Processing requirement for project: MyProject, session: 550e8400...
INFO  - Clearing session 550e8400...
```

### Frontend Logs

```javascript
console.log('Session initialized:', response);
console.log('Message sent successfully:', response);
console.error('Failed to send message:', error);
```

### Redis Monitoring

```bash
# Monitor Redis commands
redis-cli MONITOR

# Check memory usage
redis-cli INFO memory

# Count session keys
redis-cli KEYS "chat:*" | wc -l
```

---

## Future Enhancements

### Phase 1: Context Awareness
- Use conversation history in AI prompts
- Implement memory window management
- Add conversation summarization

### Phase 2: Database Integration
- Store sessions in PostgreSQL
- Add user authentication
- Implement project-user relationships

### Phase 3: Advanced Features
- Real-time streaming responses (WebSocket)
- Multi-user collaboration
- PlantUML diagram generation from requirements
- Export conversations to documentation

### Phase 4: Production Ready
- Implement all security measures
- Add comprehensive monitoring
- Set up CI/CD pipelines
- Load testing and optimization

---

**Last Updated:** January 30, 2026
**Version:** 1.0.0
**Status:** Development - Basic functionality complete
