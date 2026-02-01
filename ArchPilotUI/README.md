# ArchPilot - Architecture Visualization & Requirement Engineering Platform

<div align="center">

**An intelligent Angular application for software architecture visualization and AI-powered requirement engineering**

[![Angular](https://img.shields.io/badge/Angular-21.1.1-red)](https://angular.io/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9.2-blue)](https://www.typescriptlang.org/)
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen)](https://github.com)

</div>

---

## 🌟 Features

### 🎨 Interactive Landing Page
- Dynamic particle network with mouse interaction
- Smooth typewriter animation
- Light/Dark theme toggle
- Responsive design

### 📊 PlantUML Visualization
- Automatic diagram generation from code
- Interactive diagram viewer
- Project-based organization
- Export capabilities

### 🤖 AI-Powered Chat
- Real-time requirement engineering
- Server-Sent Events (SSE) support
- Context-aware conversations
- Multiple chat states (Active, Streaming, Completed, Error)

### 📁 Project Management
- Repository analysis via Git URL
- Requirement-based project creation
- Project listing and selection
- Error handling with user feedback

### 🎨 Theme System
- Light and Dark modes
- CSS variable-based theming
- Smooth transitions
- Persistent theme selection

---

## 🚀 Quick Start

```bash
# Clone the repository
cd ArchPilotUI

# Install dependencies
npm install

# Start development server
npm start

# Open browser at http://localhost:4200
```

---

## 📋 Prerequisites

- Node.js 24.13.0 or higher
- npm 11.6.2 or higher
- Angular CLI 21.1.1

---

## 🏗️ Project Structure

```
ArchPilotUI/
├── src/
│   ├── app/
│   │   ├── components/          # All UI components
│   │   │   ├── menu/           # Navigation menu
│   │   │   ├── landing/        # Landing page with particles
│   │   │   ├── plantuml-viewer/# Diagram viewer
│   │   │   ├── chat/           # AI chat interface
│   │   │   ├── add-project/    # Add new project
│   │   │   ├── existing-project/# View existing projects
│   │   │   └── about/          # About page
│   │   ├── services/           # Business logic services
│   │   │   ├── api.ts          # HTTP API service
│   │   │   ├── chat.ts         # Chat service
│   │   │   └── theme.ts        # Theme management
│   │   ├── app.ts              # Root component
│   │   ├── app.routes.ts       # Route configuration
│   │   └── app.config.ts       # App configuration
│   ├── types/                  # TypeScript declarations
│   ├── styles.scss             # Global styles
│   └── index.html              # Entry HTML
├── proxy.conf.json             # API proxy configuration
├── angular.json                # Angular configuration
└── package.json                # Dependencies
```

---

## 🔧 Configuration

### Backend API Configuration

Edit `proxy.conf.json` to configure your backend:

```json
{
  "/api": {
    "target": "http://localhost:8000",
    "secure": false,
    "changeOrigin": true
  }
}
```

### Required API Endpoints

Your backend should implement:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/project/analyze-repo` | Analyze Git repository |
| GET | `/api/projects` | List all projects |
| GET | `/api/project/{name}` | Get project details |
| GET | `/api/chat/stream` | SSE chat endpoint |

---

## 📖 Documentation

- **[QUICK_START.md](QUICK_START.md)** - Get started in 3 steps
- **[IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)** - Complete feature list
- **[DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)** - Detailed development guide
- **[PROJECT_SETUP.md](PROJECT_SETUP.md)** - Architecture and setup
- **[readme-spec.md](readme-spec.md)** - Original specification

---

## 🛠️ Available Scripts

| Command | Description |
|---------|-------------|
| `npm start` | Start development server |
| `npm run build` | Build for production |
| `npm run watch` | Build in watch mode |
| `npm test` | Run unit tests |

---

## 🎯 Key Technologies

- **Framework**: Angular 21.1.1
- **Language**: TypeScript 5.9.2
- **Styling**: SCSS with CSS Variables
- **State Management**: RxJS
- **HTTP Client**: Angular HttpClient
- **Routing**: Angular Router
- **Libraries**:
  - plantuml-encoder (1.4.0)
  - particles.js (2.0.0)
  - rxjs (7.8.0)

---

## 🌐 Application Flow

```
Landing Page (/)
    ↓
Menu Navigation
    ↓
    ├─→ Add Project (/add-project)
    │   ├─→ Repository Tab → Analyze → PlantUML Viewer
    │   └─→ Requirement Tab → Chat → PlantUML Viewer
    │
    ├─→ Existing Project (/existing-project)
    │   └─→ Select Project → PlantUML Viewer
    │
    └─→ PlantUML Viewer (/plantuml-viewer)
        └─→ Add Requirement → Chat → Updated Diagram
```

---

## 🎨 Theme System

The application supports light and dark themes using CSS variables:

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

Toggle theme using the "Invert Color" menu option.

---

## 🧪 Testing

```bash
# Run unit tests
npm test

# Run tests in watch mode
npm test -- --watch

# Generate coverage report
npm test -- --coverage
```

---

## 🏗️ Building for Production

```bash
# Build for production
npm run build

# Output will be in dist/ArchPilotUI/browser/
```

---

## 🐛 Troubleshooting

### Port Already in Use
```bash
npx kill-port 4200
# or use different port
ng serve --port 4300
```

### API Connection Issues
1. Verify backend is running
2. Check `proxy.conf.json` configuration
3. Inspect browser console for errors
4. Check CORS settings on backend

### Theme Not Switching
1. Clear browser cache
2. Verify `styles.scss` is loaded
3. Check body class in DevTools
4. Ensure CSS variables are defined

---

## 📝 Implementation Status

✅ **All Specification Requirements Implemented**

- [x] Interactive landing page with particle network
- [x] PlantUML visualization component
- [x] AI chat component with streaming
- [x] Menu structure with navigation
- [x] Add project via repository
- [x] Add project via requirements
- [x] Existing project management
- [x] Complete navigation flow
- [x] Theme toggle system
- [x] Error handling
- [x] Responsive design

---

## 🔮 Future Enhancements

- [ ] User authentication
- [ ] Project persistence
- [ ] Diagram editing
- [ ] Export functionality
- [ ] Mobile optimization
- [ ] Unit test coverage
- [ ] E2E tests
- [ ] Performance optimization

---

## 📄 License

This project is part of the ArchPilot system.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

---

## 📞 Support

For issues and questions:
- Check the [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)
- Review [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)
- Consult [QUICK_START.md](QUICK_START.md)

---

<div align="center">

**Built with ❤️ using Angular**

[Documentation](QUICK_START.md) • [Features](IMPLEMENTATION_SUMMARY.md) • [Development](DEVELOPMENT_GUIDE.md)

</div>
