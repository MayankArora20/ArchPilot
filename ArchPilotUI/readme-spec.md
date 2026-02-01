Project Specification: ArchPilot
Project Overview
Project Name: ArchPilot

Framework: Angular (Latest Stable)

Styling: SCSS with CSS Variables for Theming

Libraries: plantuml-encoder, particles.js (or custom Canvas implementation), rxjs (for streaming).

Requirement 1: Layout & Interactive Landing Page
Navigation: A fixed-top, transparent MenuComponent with a high z-index.

Landing Page Background: A dynamic Particle Network (dots connected by lines) rendered on an HTML5 Canvas.

Interaction: Dots/lines should displace or accelerate on mouse hover.

Theme: Default "Light Mode" (White background, black lines/dots). Toggleable via Menu to "Dark Mode" (Black background, white lines/dots).

Centerpiece: A typewriter effect in the center of the screen that types "ArchPilot," pauses, deletes it, and repeats.

Requirement 2: PlantUML Visualization Component
Function: Accepts a string input (PlantUML syntax), encodes it using plantuml-encoder, and displays the resulting diagram from the PlantUML server.

Inputs: projectName: string, pumlContent: string.

Action: Includes an AddNewRequirement button which routes the user to the Chat Component, passing the projectName.

Requirement 3: AI Chat Component (Requirement Engineering)
Header: Displays the current projectName in the top right.

Streaming Logic: Implement an AI chat interface supporting Server-Sent Events (SSE) or chunked transfers.

State Management:

Active: User can type and send messages.

Streaming: Input is disabled; text updates in real-time.

Completed: Input is frozen. A modal appears: "Requirements updated successfully." Clicking "OK" routes the user to the Project View.

Expired/Error: Support "Retry" or "Re-establish Session" UI states.

Requirement 4: Menu Structure
Project

Add New Project

Existing Project

Invert Color (Global Theme Toggle)

About Us

Requirement 5 & 6: Add New Project (Repository Flow)
View: "Add Project" page with two tabs/options: Repository Link or Requirement.

Repository Flow: * Input: Git URL.

On Submit: Call POST /api/project/analyze-repo.

Error Handling: If response is { "Error": { "reason": [] } }, display reasons in a modal/toast.

Success Handling: If response contains PlantUML data, navigate to the PlantUML Component.

Requirement 7: Add New Project (Requirement Flow)
Flow: Selecting "Add Project using Requirement" opens the Chat Component (Req 3).

Outcome: Upon session completion, the backend returns either an error object or a PlantUML graph.

Requirement 8: Existing Project Flow
View: Dropdown populated by GET /api/projects (Format: { "Projects": string[] }).

Action: Selecting a project calls GET /api/project/{projectName}.

Outcome: Display the returned PlantUML graph or show error modal.

Requirement 9: Navigation Logic
PlantUML Component -> AddNewRequirement Button -> Chat Component (passing project context).

Chat Component -> "OK" on Success Modal -> PlantUML Component (refreshing the diagram).