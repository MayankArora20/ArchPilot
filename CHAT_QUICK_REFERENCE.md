# Chat Functionality - Quick Reference

## 🚀 Quick Start

### Backend
```bash
cd ArchPilotBackend
export VERTEX_AI_PROJECT_ID=your-project-id
docker-compose up -d redis
./gradlew bootRun
```

### Frontend
```bash
cd ArchPilotUI
npm install
npm start
```

### Access
- Frontend: http://localhost:4200
- Backend: http://localhost:8080
- Chat: http://localhost:4200/chat?projectName=TestProject

---

## 📡 API Endpoints

### Initialize Session
```bash
POST /api/addRequirement/{projectName}/init
Response: { sessionId, projectName, message }
```

### Send Message
```bash
POST /api/addRequirement/{projectName}
Body: { message, sessionId? }
Response: { sessionId, projectName, response, timestamp }
```

### Get History
```bash
GET /api/addRequirement/{projectName}/history?sessionId={id}
Response: { sessionId, projectName, history[] }
```

### Clear Session
```bash
DELETE /api/addRequirement/{projectName}/session?sessionId={id}
Response: { message }
```

---

## 💻 Code Examples

### Frontend - Initialize Session
```typescript
this.chatService.initSession('MyProject').subscribe({
  next: (response) => console.log('Session:', response.sessionId),
  error: (error) => console.error('Error:', error)
});
```

### Frontend - Send Message
```typescript
this.chatService.sendMessage('MyProject', 'Hello').subscribe({
  next: (response) => console.log('Response:', response.response),
  error: (error) => console.error('Error:', error)
});
```

### Backend - Session Service
```java
// Create session
String sessionId = sessionService.createSession("MyProject");

// Add message
sessionService.addMessage(sessionId, "user", "Hello");

// Get history
List<String> history = sessionService.getHistory(sessionId);

// Clear session
sessionService.clearSession(sessionId);
```

### Backend - Chat Agent
```java
// Get AI response
String response = chatAgentService.chat("I need authentication");
```

---

## 🗄️ Redis Keys

```bash
# Session metadata
chat:session:{sessionId}

# Project-session mapping
chat:project:{projectName}

# Conversation history
chat:session:{sessionId}:history
```

### Redis Commands
```bash
# List all sessions
redis-cli KEYS "chat:*"

# Get session data
redis-cli HGETALL chat:session:{sessionId}

# Get history
redis-cli LRANGE chat:session:{sessionId}:history 0 -1

# Delete session
redis-cli DEL chat:session:{sessionId}
```

---

## 🔍 Testing

### Test with curl
```bash
# Init
curl -X POST http://localhost:8080/api/addRequirement/Test/init

# Send
curl -X POST http://localhost:8080/api/addRequirement/Test \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello","sessionId":"your-id"}'

# History
curl "http://localhost:8080/api/addRequirement/Test/history?sessionId=your-id"

# Clear
curl -X DELETE "http://localhost:8080/api/addRequirement/Test/session?sessionId=your-id"
```

---

## 🐛 Troubleshooting

### Backend won't start
```bash
# Check Redis
docker ps | grep redis

# Check port
netstat -an | grep 8080

# Check env vars
echo $VERTEX_AI_PROJECT_ID
```

### Frontend can't connect
```bash
# Check backend
curl http://localhost:8080/api/addRequirement/Test/init

# Check proxy
cat ArchPilotUI/proxy.conf.json

# Check browser console
# Open DevTools → Console → Network
```

### Redis issues
```bash
# Start Redis
docker-compose up -d redis

# Check Redis
docker logs archpilot-redis

# Connect to Redis
docker exec -it archpilot-redis redis-cli
```

---

## 📁 File Locations

### Backend
```
ArchPilotBackend/src/main/java/com/archpilot/
├── controller/RequirementController.java
└── service/
    ├── SessionService.java
    └── ChatAgentService.java
```

### Frontend
```
ArchPilotUI/src/app/
├── components/chat/
│   ├── chat.ts
│   ├── chat.html
│   └── chat.scss
└── services/chat.ts
```

---

## ⚙️ Configuration

### Backend (application.yml)
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  ai:
    vertex:
      ai:
        gemini:
          project-id: ${VERTEX_AI_PROJECT_ID}
          model: gemini-2.0-flash-exp
```

### Frontend (proxy.conf.json)
```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

---

## 📊 Data Models

### ChatMessage (Frontend)
```typescript
interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp?: number;
}
```

### SessionInfo (Frontend)
```typescript
interface SessionInfo {
  sessionId: string;
  projectName: string;
}
```

### ChatState (Frontend)
```typescript
enum ChatState {
  Active = 'active',
  Streaming = 'streaming',
  Completed = 'completed',
  Error = 'error'
}
```

---

## 🔐 Environment Variables

### Required
```bash
VERTEX_AI_PROJECT_ID=your-gcp-project-id
```

### Optional
```bash
JWT_SECRET=your-secret-key
VERTEX_AI_LOCATION=us-central1
```

---

## 📚 Documentation

- **Backend Details**: `ArchPilotBackend/readme-chat.md`
- **Frontend Details**: `ArchPilotUI/readme-chat.md`
- **Implementation Guide**: `CHAT_IMPLEMENTATION_GUIDE.md`
- **Architecture**: `CHAT_ARCHITECTURE.md`

---

## ✅ Feature Checklist

### Implemented
- [x] Session management with Redis
- [x] REST API endpoints
- [x] AI-powered responses
- [x] Message history storage
- [x] Session clearing
- [x] Error handling
- [x] Frontend UI with session display
- [x] Message timestamps
- [x] State management

### Not Implemented (Future)
- [ ] Database persistence
- [ ] Context-aware conversations
- [ ] PlantUML generation
- [ ] Real-time streaming
- [ ] Authentication
- [ ] Multi-user support

---

## 🎯 Common Tasks

### Add a new endpoint
1. Add method to `RequirementController.java`
2. Add service method if needed
3. Update frontend service `chat.ts`
4. Update component if needed

### Change session expiration
```java
// In SessionService.java
private static final long SESSION_EXPIRATION_HOURS = 24; // Change this
```

### Customize AI behavior
```yaml
# In application.yml
spring:
  ai:
    vertex:
      ai:
        gemini:
          chat:
            options:
              temperature: 0.7      # Creativity (0-1)
              max-output-tokens: 2048  # Response length
```

---

## 🚨 Important Notes

- Sessions expire after 24 hours
- Redis must be running for backend to work
- Vertex AI credentials required for AI responses
- CORS is open in development (restrict in production)
- No authentication currently (add for production)

---

**Last Updated:** January 30, 2026
**Version:** 1.0.0
