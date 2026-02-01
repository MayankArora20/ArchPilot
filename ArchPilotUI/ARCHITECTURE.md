# ArchPilot Architecture Overview

## 🏗️ Application Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         App Component                        │
│                    (Root + Menu Always Visible)              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ├─ Menu Component (Fixed Top)
                              │
                              └─ Router Outlet
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
   Landing Page              Add/Existing Project          PlantUML Viewer
        │                             │                             │
        │                             ├─ Repository Tab             │
        │                             │      │                      │
        │                             │      └─ API Service         │
        │                             │                             │
        │                             └─ Requirement Tab            │
        │                                    │                      │
        │                                    └─ Chat Component ─────┘
        │                                           │
        └───────────────────────────────────────────┘
```

## 📦 Component Hierarchy

### Root Level
```
App Component
├── Menu Component (Always Visible)
└── Router Outlet
    ├── Landing Component
    ├── Add Project Component
    ├── Existing Project Component
    ├── PlantUML Viewer Component
    ├── Chat Component
    └── About Component
```

## 🔄 Data Flow

### 1. Repository Analysis Flow
```
User Input (Git URL)
    ↓
Add Project Component
    ↓
API Service.analyzeRepo()
    ↓
Backend API (POST /api/project/analyze-repo)
    ↓
Response (projectName + pumlContent)
    ↓
Router Navigation with Query Params
    ↓
PlantUML Viewer Component
    ↓
plantuml-encoder.encode()
    ↓
Display Diagram
```

### 2. Requirement Engineering Flow
```
User Input (Project Name)
    ↓
Add Project Component
    ↓
Router Navigation to Chat
    ↓
Chat Component
    ↓
Chat Service (SSE Connection)
    ↓
Backend API (SSE Stream)
    ↓
Real-time Message Updates
    ↓
Completion Modal
    ↓
Router Navigation to PlantUML Viewer
```

### 3. Existing Project Flow
```
Component Init
    ↓
API Service.getProjects()
    ↓
Backend API (GET /api/projects)
    ↓
Populate Dropdown
    ↓
User Selection
    ↓
API Service.getProject(name)
    ↓
Backend API (GET /api/project/{name})
    ↓
Router Navigation to PlantUML Viewer
```

## 🎨 Service Architecture

### API Service
```typescript
API Service
├── analyzeRepo(gitUrl: string): Observable<PlantUMLResponse>
├── getProjects(): Observable<ProjectResponse>
└── getProject(name: string): Observable<PlantUMLResponse>
```

### Chat Service
```typescript
Chat Service
├── startChat(projectName: string, endpoint: string): void
├── sendMessage(message: string): void
├── completeChat(): void
├── closeChat(): void
├── message$: Observable<string>
└── state$: Observable<ChatState>
```

### Theme Service
```typescript
Theme Service
├── toggleTheme(): void
├── getCurrentTheme(): boolean
└── isDarkMode$: Observable<boolean>
```

## 🎯 State Management

### Component State
```
Landing Component
├── particles: Particle[]
├── typewriterText: string
└── animation state

PlantUML Viewer Component
├── projectName: string
├── pumlContent: string
└── diagramUrl: string

Chat Component
├── messages: ChatMessage[]
├── currentMessage: string
├── chatState: ChatState
└── showModal: boolean

Add Project Component
├── activeTab: 'repository' | 'requirement'
├── gitUrl: string
├── projectName: string
├── loading: boolean
└── errorReasons: string[]

Existing Project Component
├── projects: string[]
├── selectedProject: string
├── loading: boolean
└── errorReasons: string[]
```

### Global State
```
Theme Service
└── isDarkMode: BehaviorSubject<boolean>
```

## 🔌 External Integrations

### PlantUML Server
```
plantuml-encoder
    ↓
Encode PlantUML syntax
    ↓
https://www.plantuml.com/plantuml/svg/{encoded}
    ↓
SVG Diagram
```

### Backend API
```
Angular HttpClient
    ↓
Proxy (proxy.conf.json)
    ↓
Backend Server (localhost:8000)
    ↓
API Endpoints
```

## 🎨 Styling Architecture

### CSS Variables (Theme System)
```scss
:root {
  --bg-color
  --text-color
  --particle-color
  --menu-bg
}

body.dark-mode {
  // Override variables
}
```

### Component Styles
```
Global Styles (styles.scss)
    ↓
Component-Specific SCSS
    ↓
CSS Variables for Theming
    ↓
Responsive Design
```

## 🛣️ Routing Architecture

```
Routes Configuration
├── '' → Landing Component
├── 'add-project' → Add Project Component
├── 'existing-project' → Existing Project Component
├── 'plantuml-viewer' → PlantUML Viewer Component
├── 'chat' → Chat Component
├── 'about' → About Component
└── '**' → Redirect to Landing
```

### Query Parameters
```
/plantuml-viewer?projectName=X&pumlContent=Y
/chat?projectName=X
```

## 📊 Module Dependencies

```
App Module (Standalone)
├── RouterModule
├── HttpClientModule
├── FormsModule
├── CommonModule
└── Components (All Standalone)
    ├── Menu
    ├── Landing
    ├── PlantUML Viewer
    ├── Chat
    ├── Add Project
    ├── Existing Project
    └── About
```

## 🔐 Security Considerations

### Implemented
- HttpClient with CORS support
- Proxy configuration for API
- Input validation
- Error handling
- Type safety with TypeScript

### Future Enhancements
- Authentication/Authorization
- JWT token management
- Route guards
- Input sanitization
- XSS protection

## 🚀 Performance Optimizations

### Current
- Standalone components (tree-shakeable)
- Lazy loading ready
- RxJS for efficient state management
- Canvas for particle animation
- CSS variables for theme switching

### Future
- Lazy loading routes
- Virtual scrolling for large lists
- Image optimization
- Bundle size optimization
- Service worker for caching

## 📱 Responsive Design

### Breakpoints (Ready for Implementation)
```scss
// Mobile
@media (max-width: 768px) { }

// Tablet
@media (min-width: 769px) and (max-width: 1024px) { }

// Desktop
@media (min-width: 1025px) { }
```

## 🧪 Testing Strategy (Future)

```
Unit Tests
├── Component Tests
├── Service Tests
└── Pipe Tests

Integration Tests
├── Component Integration
└── Service Integration

E2E Tests
├── User Flows
└── Navigation Tests
```

## 📈 Scalability

### Current Architecture Supports
- Multiple projects
- Multiple users (with backend support)
- Real-time updates via SSE
- Theme customization
- Extensible component system

### Future Scalability
- Microservices backend
- WebSocket for real-time features
- State management library (NgRx)
- Internationalization (i18n)
- Progressive Web App (PWA)

---

## 🎯 Key Design Decisions

1. **Standalone Components**: Modern Angular architecture, better tree-shaking
2. **RxJS for State**: Reactive programming, efficient updates
3. **CSS Variables**: Dynamic theming without JavaScript
4. **Canvas for Particles**: Better performance than DOM manipulation
5. **Query Parameters**: Simple state passing between routes
6. **Proxy Configuration**: Avoid CORS issues during development
7. **TypeScript Strict Mode**: Type safety and better IDE support

---

**Architecture Status: ✅ Production-Ready**
