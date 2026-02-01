# ArchPilot - Angular Project

## Project Overview
ArchPilot is an Angular application for architecture visualization and requirement engineering using PlantUML diagrams and AI-powered chat.

## Features Implemented

### 1. Landing Page
- Dynamic particle network background with mouse interaction
- Typewriter effect displaying "ArchPilot"
- Theme toggle (Light/Dark mode)

### 2. Menu Component
- Fixed-top transparent navigation
- Dropdown menus for Project options
- Theme toggle button
- Routing to all major sections

### 3. PlantUML Viewer
- Displays PlantUML diagrams from encoded content
- Shows project name
- "Add New Requirement" button to navigate to chat

### 4. AI Chat Component
- Real-time chat interface for requirement engineering
- Support for Server-Sent Events (SSE)
- Multiple states: Active, Streaming, Completed, Error
- Success modal on completion
- Project name display in header

### 5. Add Project Component
- Two tabs: Repository Link and Requirement
- Repository analysis via API
- Direct chat initiation for requirements
- Error handling with modal display

### 6. Existing Project Component
- Dropdown list of existing projects
- Load project diagrams
- Error handling

## Tech Stack
- Angular 21.1.1
- TypeScript
- SCSS with CSS Variables
- RxJS for reactive programming
- plantuml-encoder for diagram encoding
- HttpClient for API communication

## Installation & Setup

1. Install dependencies:
```bash
npm install
```

2. Run development server:
```bash
ng serve
```

3. Open browser at `http://localhost:4200`

## API Configuration

The application expects the following API endpoints:

- `POST /api/project/analyze-repo` - Analyze git repository
- `GET /api/projects` - Get list of projects
- `GET /api/project/{projectName}` - Get specific project

Update the base URL in `src/app/services/api.ts` if your backend runs on a different port.

## Project Structure

```
src/
├── app/
│   ├── components/
│   │   ├── menu/
│   │   ├── landing/
│   │   ├── plantuml-viewer/
│   │   ├── chat/
│   │   ├── add-project/
│   │   └── existing-project/
│   ├── services/
│   │   ├── api.ts
│   │   ├── chat.ts
│   │   └── theme.ts
│   ├── app.ts
│   ├── app.routes.ts
│   └── app.config.ts
└── styles.scss
```

## Routing

- `/` - Landing page
- `/add-project` - Add new project (Repository or Requirement)
- `/existing-project` - View existing projects
- `/plantuml-viewer` - View PlantUML diagrams
- `/chat` - AI chat for requirements

## Theme System

The application uses CSS variables for theming:
- `--bg-color` - Background color
- `--text-color` - Text color
- `--particle-color` - Particle network color
- `--menu-bg` - Menu background

Toggle between light and dark mode using the "Invert Color" menu option.

## Development Notes

- All components use standalone component architecture
- Services are provided at root level
- Reactive forms with FormsModule
- HttpClient for API communication
- Router for navigation

## Next Steps

1. Connect to actual backend API
2. Implement SSE for real-time chat streaming
3. Add authentication if needed
4. Enhance error handling
5. Add loading states and animations
6. Implement unit tests
