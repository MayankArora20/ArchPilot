# Diagram Generation Services

This package contains services responsible for generating UML diagrams from code flow analysis. These services were extracted from `JavaArchitectAgentService` to follow separation of concerns principle.

## Classes

### FlowAnalysisParser
- **Purpose**: Parses analysis content and extracts structured information
- **Responsibilities**:
  - Extract involved classes from analysis text
  - Extract execution steps from analysis text  
  - Extract flow logic elements (START, DECISION, LOOP, PROCESS, END)

### FlowDiagramGenerator
- **Purpose**: Generates PlantUML flow diagrams (activity diagrams)
- **Responsibilities**:
  - Create flow diagrams from analysis content
  - Handle different flow patterns (decisions, loops, processes)
  - Generate basic flows based on method names when detailed analysis is unavailable

### SequenceDiagramGenerator
- **Purpose**: Generates PlantUML sequence diagrams
- **Responsibilities**:
  - Create sequence diagrams showing interactions between classes
  - Extract participant classes from analysis
  - Generate basic sequence flows

### DiagramFileManager
- **Purpose**: Manages diagram file creation and HTML link generation
- **Responsibilities**:
  - Coordinate between sequence and flow diagram generators
  - Create directory structure for diagram files
  - Convert PlantUML to PNG using PlantUmlToPngService
  - Generate HTML links for accessing diagrams

## Architecture Benefits

1. **Separation of Concerns**: UML generation logic is separated from core agent functionality
2. **Reusability**: Diagram services can be used by other components
3. **Maintainability**: Each service has a single, focused responsibility
4. **Testability**: Individual services can be unit tested independently
5. **Extensibility**: New diagram types can be added without modifying existing services

## Usage

The `JavaArchitectAgentService` now uses `DiagramFileManager` which coordinates all diagram generation:

```java
@Autowired
private DiagramFileManager diagramFileManager;

// Generate diagrams
String diagramLinks = diagramFileManager.generateFlowDiagrams(
    projectName, className, methodName, analysisContent
);
```

## Dependencies

- **FlowAnalysisParser**: No external dependencies (pure parsing logic)
- **FlowDiagramGenerator**: Depends on FlowAnalysisParser
- **SequenceDiagramGenerator**: Depends on FlowAnalysisParser  
- **DiagramFileManager**: Depends on all generators + PlantUmlToPngService