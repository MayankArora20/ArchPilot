# ArchPilot - Quick Start Guide

## 🚀 Get Started in 3 Steps

### 1. Install Dependencies
```bash
cd ArchPilotUI
npm install
```

### 2. Start Development Server
```bash
npm start
```

### 3. Open Browser
Navigate to: `http://localhost:4200`

---

## 📁 Project Overview

**Complete Angular 21 application implementing all specification requirements:**

✅ Interactive landing page with particle network  
✅ PlantUML diagram viewer  
✅ AI chat for requirement engineering  
✅ Repository analysis  
✅ Project management  
✅ Light/Dark theme toggle  

---

## 🎯 Key Features

### Landing Page
- Dynamic particle network background
- Mouse interaction effects
- Typewriter animation
- Theme toggle

### Project Management
- Add projects via Git URL
- Add projects via requirements chat
- View existing projects
- PlantUML visualization

### AI Chat
- Real-time streaming support
- Multiple chat states
- Success/error handling
- Project context awareness

---

## 🔧 Configuration

### Backend API
Default: `http://localhost:8000`

To change, edit `proxy.conf.json`:
```json
{
  "/api": {
    "target": "http://localhost:YOUR_PORT",
    "secure": false,
    "changeOrigin": true
  }
}
```

---

## 📚 Documentation

- **IMPLEMENTATION_SUMMARY.md** - Complete feature list
- **DEVELOPMENT_GUIDE.md** - Detailed development info
- **PROJECT_SETUP.md** - Setup and architecture
- **readme-spec.md** - Original specification

---

## 🛠️ Common Commands

```bash
# Development
npm start                    # Start dev server
npm run build               # Production build
npm run watch               # Watch mode

# Testing
npm test                    # Run tests

# Port Management
npx kill-port 4200         # Kill port 4200
ng serve --port 4300       # Use different port
```

---

## 🎨 Theme Toggle

Click "Invert Color" in the menu to switch between light and dark modes.

---

## 📋 API Endpoints Required

Your backend should implement:

- `POST /api/project/analyze-repo` - Analyze repository
- `GET /api/projects` - List all projects
- `GET /api/project/{name}` - Get project details
- `GET /api/chat/stream` - SSE chat endpoint (optional)

---

## ✅ Build Status

**All systems operational:**
- ✅ TypeScript compilation
- ✅ SCSS compilation
- ✅ Production build
- ✅ All routes configured
- ✅ All components implemented

---

## 🐛 Troubleshooting

**Port in use?**
```bash
npx kill-port 4200
```

**API not connecting?**
- Check backend is running
- Verify proxy.conf.json
- Check browser console

**Theme not working?**
- Clear browser cache
- Check styles.scss loaded
- Inspect body class in DevTools

---

## 📞 Need Help?

Check the detailed guides:
1. DEVELOPMENT_GUIDE.md - Development details
2. IMPLEMENTATION_SUMMARY.md - Feature overview
3. PROJECT_SETUP.md - Architecture info

---

**Happy Coding! 🎉**
