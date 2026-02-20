# Java Architect Agent Usage Guide

## Overview
The Java Architect Agent is a sophisticated AI-powered service that acts as a senior Java architect, providing architectural guidance, creating JIRA tickets, and explaining code flows.

## Token Guardrail Protection ✅
- **Automatic Protection**: All chat endpoints (`/api/chat/`) are protected by the TokenGuardrailInterceptor
- **Rate Limiting**: Dual bucket strategy (RPM + TPM) prevents API abuse
- **Token Tracking**: Consumption is monitored and managed automatically
- **User Identification**: Uses API keys, session IDs, or IP addresses for rate limiting

## JIRA Ticket Storage ✅
- **Directory Structure**: `Jira/ProjectName/JiraId.json`
- **Automatic Creation**: Directories are created automatically
- **JSON Format**: Easy integration with external systems
- **File Naming**: Uses the generated JIRA ticket ID (e.g., `ARCH-1234.json`)

## Usage Examples

### 1. Creating JIRA Tickets
```bash
# Initialize chat session first
curl -X POST "http://localhost:8080/api/chat/init" \
  -H "Content-Type: application/json" \
  -d '{"projectName": "smart-sync-warehouse-orchestrator"}'

# Create JIRA ticket
curl -X POST "http://localhost:8080/api/chat/message" \
  -H "Content-Type: application/json" \
  -d '{"message": "Create a JIRA ticket for implementing user authentication with JWT tokens"}'
```

**Response Format (JSON):**
```json
{
  "status": "SUCCESS",
  "message": "Message processed successfully",
  "data": {
    "sessionId": "ABC123",
    "projectName": "smart-sync-warehouse-orchestrator",
    "response": "🎫 **JIRA Ticket Created & Saved**\n\n**Ticket ID:** ARCH-1234\n**Title:** Implement JWT-based User Authentication\n**Type:** Story | **Priority:** High | **Story Points:** 8\n\n**Description:**\nImplement secure user authentication using JWT tokens...\n\n**Classes to Consider:**\n- AuthenticationController\n- JwtTokenService\n- UserService\n\n**Methods to Consider:**\n- authenticateUser()\n- generateJwtToken()\n- validateToken()\n\n**Suggested Design Patterns:**\n- Strategy Pattern for authentication methods\n- Factory Pattern for token generation\n\n**Unit Test Cases:**\n- Test valid login credentials\n- Test invalid credentials\n- Test token expiration\n\n**Success Criteria:**\nUsers can successfully authenticate and receive valid JWT tokens\n\n📁 **File Location:** `Jira/smart-sync-warehouse-orchestrator/ARCH-1234.json`",
    "timestamp": "2026-02-19T15:30:45.123456",
    "sessionActive": true,
    "chatHistory": [...]
  },
  "timestamp": "2026-02-19T15:30:45.123456"
}
```

### 2. Explaining Code Flows
```bash
# Specific class/method
curl -X POST "http://localhost:8080/api/chat/message" \
  -H "Content-Type: application/json" \
  -d '{"message": "Explain the flow of WarehouseOrchestrator.processOrder method"}'

# General description
curl -X POST "http://localhost:8080/api/chat/message" \
  -H "Content-Type: application/json" \
  -d '{"message": "How does the inventory management system work?"}'
```

### 3. Architectural Advice
```bash
curl -X POST "http://localhost:8080/api/chat/message" \
  -H "Content-Type: application/json" \
  -d '{"message": "What design patterns should I use for handling multiple payment methods?"}'
```

## Response Structure

All `/api/chat/message` responses now return structured JSON:

```json
{
  "status": "SUCCESS|ERROR",
  "message": "Status message",
  "data": {
    "sessionId": "Session identifier",
    "projectName": "Project name",
    "response": "The architect agent's response (formatted markdown)",
    "timestamp": "Response timestamp",
    "sessionActive": true,
    "chatHistory": [
      {
        "content": "User message",
        "role": "user",
        "timestamp": "..."
      },
      {
        "content": "Agent response",
        "role": "assistant", 
        "timestamp": "..."
      }
    ]
  },
  "timestamp": "API response timestamp"
}
```

## Agent Capabilities

### Intent Recognition
The agent automatically recognizes user intent:
- **JIRA Creation**: Keywords like "create ticket", "new feature", "implement", "we need", "support"
- **Code Flow**: Keywords like "explain flow", "how does", "trace"
- **Architectural Advice**: Keywords like "design pattern", "architecture", "best practice"

### Context Awareness
- Uses project UML diagrams for architectural understanding
- Leverages project metadata (class count, packages, etc.)
- Maintains chat session context for follow-up questions

### File System Integration
- **JIRA Tickets**: Saved to `Jira/ProjectName/TicketId.json`
- **Utility Methods**: Load existing tickets, check ticket existence
- **Error Handling**: Graceful fallback if file operations fail

## Error Handling
- Token guardrail protection with 429 responses
- Graceful degradation if file operations fail
- Fallback responses for parsing errors
- Comprehensive logging for debugging
- **JSON Response Format**: All responses are now properly structured JSON

## Breaking Changes
- **Response Format**: The `/api/chat/message` endpoint now returns structured JSON instead of plain text
- **Client Updates**: Frontend clients need to access the response content via `data.response` field
- **Backward Compatibility**: Old clients expecting plain text responses will need to be updated