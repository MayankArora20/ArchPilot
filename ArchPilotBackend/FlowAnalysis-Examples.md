# Code Flow Analysis - Usage Examples

This document provides practical examples of how to use the new Code Flow Analysis functionality.

## Example 1: Chat-based Flow Analysis

### Step 1: Initialize Chat Session
```bash
curl -X POST "http://localhost:8080/api/chat/init" \
  -H "Content-Type: application/json" \
  -d '{"projectName": "smart-sync-warehouse-orchestrator"}'
```

### Step 2: Request Flow Analysis (Method 1 - Specific)
```bash
curl -X POST "http://localhost:8080/api/chat/message" \
  -H "Content-Type: application/json" \
  -d '{"message": "Analyze the flow of processPayment method in PaymentService class"}'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Message processed successfully",
  "data": {
    "sessionId": "ABC123",
    "projectName": "smart-sync-warehouse-orchestrator",
    "response": "## Code Flow Analysis: PaymentService.processPayment\n\n**Flow Description:**\nThe processPayment method orchestrates the complete payment processing workflow...\n\n**Visual Diagrams:**\nHave a look at:\n<a href=\"/api/flow/diagram/smart-sync-warehouse-orchestrator/PaymentServiceprocessPayment_20260219_234700-sequence.png\">Sequence Diagram</a>\n<a href=\"/api/flow/diagram/smart-sync-warehouse-orchestrator/PaymentServiceprocessPayment_20260219_234700-flow.png\">Flow Diagram</a>",
    "sessionActive": true
  }
}
```

### Step 3: Request Flow Analysis (Method 2 - Interactive)
```bash
curl -X POST "http://localhost:8080/api/chat/message" \
  -H "Content-Type: application/json" \
  -d '{"message": "I want to understand how the authentication flow works"}'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Message processed successfully",
  "data": {
    "response": "I can help you understand the code flow! Please provide:\n- Class name: Which class contains the method you want to analyze?\n- Method name: Which specific method should I analyze?\n\nExample: 'Analyze the flow of processPayment method in PaymentService class'",
    "sessionActive": true
  }
}
```

### Step 4: Follow-up with Specific Details
```bash
curl -X POST "http://localhost:8080/api/chat/message" \
  -H "Content-Type: application/json" \
  -d '{"message": "AuthService.authenticate"}'
```

## Example 2: Direct API Flow Analysis

### Analyze Code Flow Directly
```bash
curl -X POST "http://localhost:8080/api/flow/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "smart-sync-warehouse-orchestrator",
    "className": "UserService",
    "methodName": "createUser",
    "userMessage": "Analyze user creation flow"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "flowDescription": "The createUser method handles the complete user registration process including validation, password hashing, database persistence, and notification sending.",
    "targetClass": "UserService",
    "targetMethod": "createUser",
    "flowSteps": [
      {
        "stepNumber": 1,
        "className": "UserService",
        "methodName": "createUser",
        "description": "Validates user input and checks for duplicates",
        "inputParameters": "UserRegistrationRequest",
        "outputResult": "ValidationResult"
      },
      {
        "stepNumber": 2,
        "className": "PasswordService",
        "methodName": "hashPassword",
        "description": "Hashes the user password using bcrypt",
        "inputParameters": "String password",
        "outputResult": "String hashedPassword"
      },
      {
        "stepNumber": 3,
        "className": "UserRepository",
        "methodName": "save",
        "description": "Persists user data to database",
        "inputParameters": "User entity",
        "outputResult": "User with ID"
      }
    ],
    "involvedClasses": ["UserService", "PasswordService", "UserRepository", "NotificationService"],
    "designPatternsUsed": ["Repository", "Service Layer", "Strategy"],
    "complexity": "Medium",
    "potentialImprovements": [
      "Add transaction management",
      "Implement async notification sending",
      "Add user creation audit logging"
    ],
    "analyzedAt": "2026-02-19T23:47:00"
  }
}
```

## Example 3: Accessing Generated Diagrams

### List Available Diagrams
```bash
curl -X GET "http://localhost:8080/api/flow/diagrams/smart-sync-warehouse-orchestrator"
```

**Response:**
```json
{
  "success": true,
  "data": [
    "UserServicecreateUser_20260219_234700-sequence.puml",
    "UserServicecreateUser_20260219_234700-sequence.png",
    "UserServicecreateUser_20260219_234700-flow.puml",
    "UserServicecreateUser_20260219_234700-flow.png",
    "PaymentServiceprocessPayment_20260219_235000-sequence.puml",
    "PaymentServiceprocessPayment_20260219_235000-sequence.png"
  ]
}
```

### Get Diagram Image
```bash
curl -X GET "http://localhost:8080/api/flow/diagram/smart-sync-warehouse-orchestrator/UserServicecreateUser_20260219_234700-sequence.png" \
  --output sequence-diagram.png
```

### Get PlantUML Source
```bash
curl -X GET "http://localhost:8080/api/flow/diagram/smart-sync-warehouse-orchestrator/UserServicecreateUser_20260219_234700-sequence.puml/content"
```

**Response:**
```json
{
  "success": true,
  "data": "@startuml\ntitle Sequence Diagram - UserService.createUser\n\nparticipant \"Client\" as Client\nparticipant \"UserService\" as UserService\nparticipant \"PasswordService\" as PasswordService\nparticipant \"UserRepository\" as UserRepository\n\nClient -> UserService : createUser()\nUserService -> PasswordService : hashPassword()\nPasswordService --> UserService : hashedPassword\nUserService -> UserRepository : save()\nUserRepository --> UserService : User with ID\n\n@enduml"
}
```

## Example 4: Natural Language Variations

The system recognizes various ways to request flow analysis:

### Variation 1: "Understand" keyword
```json
{"message": "I want to understand the flow of how orders are processed"}
```

### Variation 2: "Explain" keyword
```json
{"message": "Explain how the OrderService.processOrder method works"}
```

### Variation 3: "Show" keyword
```json
{"message": "Show me the execution path for user authentication"}
```

### Variation 4: "Trace" keyword
```json
{"message": "Trace the execution of PaymentProcessor.validatePayment"}
```

### Variation 5: "Walk through" keyword
```json
{"message": "Walk me through the step-by-step process of data synchronization"}
```

## Example 5: Integration with Frontend

### HTML Example
```html
<!DOCTYPE html>
<html>
<head>
    <title>Flow Analysis Demo</title>
</head>
<body>
    <div id="chat-container">
        <div id="messages"></div>
        <input type="text" id="message-input" placeholder="Ask about code flows...">
        <button onclick="sendMessage()">Send</button>
    </div>

    <script>
        async function sendMessage() {
            const input = document.getElementById('message-input');
            const message = input.value;
            
            const response = await fetch('/api/chat/message', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({message: message})
            });
            
            const result = await response.json();
            displayMessage(result.data.response);
            input.value = '';
        }
        
        function displayMessage(html) {
            const messages = document.getElementById('messages');
            const messageDiv = document.createElement('div');
            messageDiv.innerHTML = html;
            messages.appendChild(messageDiv);
        }
    </script>
</body>
</html>
```

## Example 6: Error Handling

### Invalid Project Name
```bash
curl -X POST "http://localhost:8080/api/flow/analyze" \
  -H "Content-Type: application/json" \
  -d '{"projectName": "non-existent-project", "className": "Test", "methodName": "test"}'
```

**Response:**
```json
{
  "success": false,
  "message": "Invalid request: Project not found: non-existent-project"
}
```

### Missing Diagram File
```bash
curl -X GET "http://localhost:8080/api/flow/diagram/smart-sync-warehouse-orchestrator/non-existent-file.png"
```

**Response:** `404 Not Found`

## File Structure After Analysis

After running flow analysis, your project structure will include:

```
ArchPilotBackend/
├── ArchpilotResource/
│   └── smart-sync-warehouse-orchestrator/
│       ├── UserServicecreateUser_20260219_234700-sequence.puml
│       ├── UserServicecreateUser_20260219_234700-sequence.png
│       ├── UserServicecreateUser_20260219_234700-flow.puml
│       ├── UserServicecreateUser_20260219_234700-flow.png
│       ├── PaymentServiceprocessPayment_20260219_235000-sequence.puml
│       └── PaymentServiceprocessPayment_20260219_235000-sequence.png
└── ... (rest of project files)
```

## Tips for Best Results

1. **Be Specific**: Include both class name and method name for immediate analysis
2. **Use Natural Language**: The system understands various phrasings
3. **Check Diagrams**: Visual diagrams provide additional insights
4. **Review Improvements**: Pay attention to suggested improvements
5. **Understand Patterns**: Note the design patterns identified in your code