# ArchPilot Development Guide

## Quick Start

```bash
# Install dependencies
npm install

# Start development server
npm start

# Application will be available at http://localhost:4200
```

## Available Scripts

- `npm start` - Start development server with hot reload
- `npm run build` - Build for production
- `npm run watch` - Build in watch mode
- `npm test` - Run unit tests

## Backend Integration

The application is configured to proxy API requests to `http://localhost:8000`. 

If your backend runs on a different port, update `proxy.conf.json`:

```json
{
  "/api": {
    "target": "http://localhost:YOUR_PORT",
    "secure": false,
    "changeOrigin": true
  }
}
```

## API Endpoints Expected

### 1. Analyze Repository
```
POST /api/project/analyze-repo
Body: { "gitUrl": "https://github.com/user/repo.git" }
Response: {
  "projectName": "string",
  "pumlContent": "string"
} OR {
  "Error": { "reason": ["error1", "error2"] }
}
```

### 2. Get Projects List
```
GET /api/projects
Response: {
  "Projects": ["project1", "project2"]
}
```

### 3. Get Project Details
```
GET /api/project/{projectName}
Response: {
  "projectName": "string",
  "pumlContent": "string"
} OR {
  "Error": { "reason": ["error1", "error2"] }
}
```

### 4. Chat SSE Endpoint (Future Implementation)
```
GET /api/chat/stream?project={projectName}
Server-Sent Events stream
```

## Component Architecture

### Services

1. **Theme Service** (`services/theme.ts`)
   - Manages light/dark theme toggle
   - Uses BehaviorSubject for reactive theme changes
   - Updates body class for CSS variable switching

2. **API Service** (`services/api.ts`)
   - Handles all HTTP requests to backend
   - Returns typed observables
   - Centralized error handling

3. **Chat Service** (`services/chat.ts`)
   - Manages SSE connections
   - Handles chat state (Active, Streaming, Completed, Error)
   - Message streaming with RxJS

### Components

1. **Menu** - Fixed navigation with dropdown menus
2. **Landing** - Particle network animation with typewriter effect
3. **PlantUML Viewer** - Displays encoded PlantUML diagrams
4. **Chat** - Real-time AI chat interface
5. **Add Project** - Two-tab interface for adding projects
6. **Existing Project** - Dropdown selector for existing projects

## Styling System

### CSS Variables (in `styles.scss`)

```scss
:root {
  --bg-color: #ffffff;
  --text-color: #000000;
  --particle-color: #000000;
  --menu-bg: rgba(255, 255, 255, 0.9);
}

body.dark-mode {
  --bg-color: #000000;
  --text-color: #ffffff;
  --particle-color: #ffffff;
  --menu-bg: rgba(0, 0, 0, 0.9);
}
```

All components use these variables for consistent theming.

## Routing Flow

```
Landing (/)
  ↓
Menu Navigation
  ↓
├─ Add Project (/add-project)
│   ├─ Repository Tab → API Call → PlantUML Viewer
│   └─ Requirement Tab → Chat → PlantUML Viewer
│
├─ Existing Project (/existing-project)
│   └─ Select Project → API Call → PlantUML Viewer
│
└─ PlantUML Viewer (/plantuml-viewer)
    └─ Add New Requirement → Chat
```

## State Management

The application uses:
- RxJS BehaviorSubjects for reactive state
- Angular Router for navigation state
- Query parameters for passing data between routes

## Error Handling

All API calls include error handling with:
- Modal dialogs for user-facing errors
- Console logging for debugging
- Retry mechanisms where appropriate

## Development Tips

1. **Hot Reload**: Changes to TypeScript/SCSS files trigger automatic reload
2. **Debugging**: Use browser DevTools with source maps enabled
3. **API Testing**: Use browser Network tab to inspect API calls
4. **Theme Testing**: Toggle theme to ensure all components adapt correctly

## Building for Production

```bash
npm run build
```

Output will be in `dist/arch-pilot-ui/browser/` directory.

## Troubleshooting

### Port Already in Use
```bash
# Kill process on port 4200
npx kill-port 4200
# Or specify different port
ng serve --port 4300
```

### API Connection Issues
- Check backend is running
- Verify proxy.conf.json settings
- Check browser console for CORS errors

### Theme Not Switching
- Verify CSS variables are defined in styles.scss
- Check body class is being toggled
- Inspect computed styles in DevTools

## Future Enhancements

1. Implement actual SSE streaming in chat
2. Add authentication/authorization
3. Implement project persistence
4. Add diagram editing capabilities
5. Enhanced error recovery
6. Loading states and animations
7. Responsive mobile design
8. Unit and E2E tests
