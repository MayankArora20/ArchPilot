# Chat Implementation Guide

## Quick Start

This guide explains how to test the newly implemented chat functionality between ArchPilotBackend and ArchPilotUI.

---

## What Was Implemented

### Backend (ArchPilotBackend)

1. **SessionService** - Manages chat sessions with Redis
   - Creates and tracks sessions per project
   - Stores conversation history
   - 24-hour session expiration

2. **ChatAgentService** - Temporary AI agent
   - Processes user messages
   - Returns AI-generated responses using Google Vertex AI (Gemini)

3. **RequirementController** - REST API with 4 endpoints:
   - `POST /api/addRequirement/{projectName}/init` - Initialize session
   - `POST /api/addRequirement/{projectName}` - Send message
   - `GET /api/addRequirement/{projectName}/history` - Get history
   - `DELETE /api/addRequirement/{projectName}/session` - Clear session

### Frontend (ArchPilotUI)

1. **Updated Chat Service** - Session-aware API client
   - Automatic session initialization
   - Message sending with session tracking
   - History retrieval
   - Session clearing

2. **Updated Chat Component** - Enhanced UI
   - Session ID display
   - Welcome message
   - Message timestamps
   - Clear session button
   - Better error handling

---

## Testing Steps

### 1. Start Backend

```bash
cd ArchPilotBackend

# Make sure Redis is running
docker-compose up -d redis

# Set environment variables
export VERTEX_AI_PROJECT_ID=your-project-id
export JWT_SECRET=your-secret-key

# Start the application
./gradlew bootRun
```

Backend will start on `http://localhost:8080`

### 2. Start Frontend

```bash
cd ArchPilotUI

# Install dependencies (if not already done)
npm install

# Start development server
npm start
```

Frontend will start on `http://localhost:4200`

### 3. Test the Chat

1. Open browser: `http://localhost:4200`
2. Navigate to the chat (via Add Project → Requirements tab, or directly to `/chat?projectName=TestProject`)
3. You should see:
   - Project name: "TestProject"
   - Session ID displayed (first 8 characters)
   - Welcome message
4. Type a message and click Send
5. Wait for AI response
6. Verify:
   - Your message appears on the right (blue)
   - AI response appears on the left
   - Timestamps are shown
   - Session ID remains the same

### 4. Test Session Persistence

1. Send multiple messages
2. Note the session ID
3. Click the trash icon (🗑️) to clear session
4. Verify:
   - Messages are cleared
   - New session ID is generated
   - You can start a new conversation

---

## API Testing (Without UI)

You can test the backend API directly using curl:

### Initialize Session
```bash
curl -X POST http://localhost:8080/api/addRequirement/TestProject/init
```

Expected response:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "projectName": "TestProject",
  "message": "Session initialized successfully"
}
```

### Send Message
```bash
curl -X POST http://localhost:8080/api/addRequirement/TestProject \
  -H "Content-Type: application/json" \
  -d '{
    "message": "I need a user authentication system",
    "sessionId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

Expected response:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "projectName": "TestProject",
  "response": "I can help you design a user authentication system...",
  "timestamp": 1706630400000
}
```

### Get History
```bash
curl "http://localhost:8080/api/addRequirement/TestProject/history?sessionId=550e8400-e29b-41d4-a716-446655440000"
```

### Clear Session
```bash
curl -X DELETE "http://localhost:8080/api/addRequirement/TestProject/session?sessionId=550e8400-e29b-41d4-a716-446655440000"
```

---

## Verifying Redis Storage

You can check Redis to see stored sessions:

```bash
# Connect to Redis
docker exec -it archpilot-redis redis-cli

# List all session keys
KEYS chat:*

# Get session data
HGETALL chat:session:550e8400-e29b-41d4-a716-446655440000

# Get session history
LRANGE chat:session:550e8400-e29b-41d4-a716-446655440000:history 0 -1

# Get project-to-session mapping
GET chat:project:TestProject
```

---

## Architecture Flow

```
┌─────────────┐         ┌──────────────────┐         ┌─────────┐
│             │         │                  │         │         │
│  Frontend   │────────▶│  Backend API     │────────▶│  Redis  │
│  (Angular)  │         │  (Spring Boot)   │         │         │
│             │◀────────│                  │◀────────│         │
└─────────────┘         └──────────────────┘         └─────────┘
                                │
                                │
                                ▼
                        ┌──────────────┐
                        │              │
                        │  Vertex AI   │
                        │  (Gemini)    │
                        │              │
                        └──────────────┘
```

### Request Flow

1. User opens chat → Frontend calls `/init` → Backend creates session in Redis
2. User sends message → Frontend calls `/addRequirement/{project}` → Backend:
   - Validates/creates session
   - Stores user message in Redis
   - Calls Vertex AI for response
   - Stores AI response in Redis
   - Returns response to frontend
3. Frontend displays message → User sees response

---

## Key Features

### ✅ Implemented

- Session management with Redis
- Automatic session creation and tracking
- Message history storage (24-hour retention)
- AI-powered responses using Google Vertex AI
- Session clearing
- Error handling
- CORS support for frontend integration
- Comprehensive logging

### 🚧 Not Yet Implemented (Future)

- Database persistence (PostgreSQL)
- Context-aware conversations using history
- PlantUML diagram generation
- Real-time streaming responses
- Multi-user support
- Authentication/authorization

---

## Troubleshooting

### Backend Won't Start

**Check:**
- Redis is running: `docker ps | grep redis`
- Environment variables are set: `echo $VERTEX_AI_PROJECT_ID`
- Port 8080 is available: `netstat -an | grep 8080`

### Frontend Can't Connect

**Check:**
- Backend is running on port 8080
- Proxy configuration in `proxy.conf.json` is correct
- Browser console for CORS errors

### AI Responses Not Working

**Check:**
- Vertex AI credentials are configured
- `VERTEX_AI_PROJECT_ID` environment variable is set
- Vertex AI API is enabled in GCP project
- Check backend logs for errors

### Session Not Persisting

**Check:**
- Redis is running and accessible
- Session ID is being sent in requests
- Session hasn't expired (24-hour limit)
- Check Redis: `redis-cli KEYS "chat:*"`

---

## Documentation

For detailed information, see:

- **Backend**: `ArchPilotBackend/readme-chat.md`
- **Frontend**: `ArchPilotUI/readme-chat.md`
- **Main Backend README**: `ArchPilotBackend/readme.md`
- **Main Frontend README**: `ArchPilotUI/readme.md`

---

## Next Steps

1. Test the basic chat functionality
2. Verify session management works correctly
3. Check Redis storage
4. Review the documentation files
5. Plan enhancements:
   - Add context awareness using conversation history
   - Integrate with PlantUML generation
   - Add database persistence
   - Implement streaming responses

---

**Created:** January 30, 2026
**Status:** Ready for testing
