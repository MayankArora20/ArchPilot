# Flow Analysis Accuracy Improvements

## Overview
Updated the JavaArchitectAgentService and diagram generation logic to improve accuracy in three key areas:
1. Logic Sequence (Low → High)
2. Method Naming (Low → High)
3. Error Handling (Medium → High)

## Changes Made

### 1. Enhanced Prompt in JavaArchitectAgentService

**File**: `src/main/java/com/archpilot/service/agent/JavaArchitectAgentService.java`

**Key Improvements**:
- Added CRITICAL REQUIREMENTS section emphasizing:
  - Use SPECIFIC method names (findById(), validateOrder(), save()) NOT generic process()
  - List classes in EXACT EXECUTION ORDER
  - Use SPECIFIC exception names (ValidationException, NotFoundException) NOT generic "error"

- Added new **Sequence Interactions** section to the prompt:
  ```
  **Sequence Interactions:** (CRITICAL - This defines the EXACT ORDER and METHOD NAMES for sequence diagrams)
  1. Client -> ClassName.methodName(parameters)
  2. ClassName -> ValidationService.validateInput(parameters)
  3. ClassName -> Repository.findById(id)
  ...
  ```

- Added **Exception Handling** section to the prompt:
  ```
  **Exception Handling:** (CRITICAL - List SPECIFIC exception types)
  - ValidationException: Thrown when input validation fails
  - NotFoundException: Thrown when requested entity is not found
  - BusinessRuleException: Thrown when business rules are violated
  ...
  ```

### 2. Enhanced FlowAnalysisParser

**File**: `src/main/java/com/archpilot/service/diagram/FlowAnalysisParser.java`

**New Features**:
- Added `SequenceInteraction` class to capture explicit sequence interactions with:
  - source class
  - target class
  - method name
  - parameters

- Added `extractSequenceInteractions()` method to parse the new Sequence Interactions section

- Enhanced `FlowLogicElement` class to include exception names

- Added `extractExceptionName()` method to extract specific exception types from decision descriptions

- Added `extractExceptionTypes()` method to parse the Exception Handling section

### 3. Improved SequenceDiagramGenerator

**File**: `src/main/java/com/archpilot/service/diagram/SequenceDiagramGenerator.java`

**Key Improvements**:
- Now prioritizes explicit sequence interactions from the analysis
- Falls back to class-based generation only when explicit interactions aren't available
- Extracts actual method names from execution steps using regex patterns
- Infers appropriate method names based on class types (e.g., Repository → findById(), Validator → validate())
- Maintains proper activation/deactivation of participants
- Preserves the exact order specified in the analysis

**New Methods**:
- `generateFromExplicitInteractions()`: Uses the Sequence Interactions section for accurate ordering
- `extractMethodCallFromStep()`: Extracts method names from execution steps
- `inferMethodFromClassName()`: Provides intelligent method name inference based on class type

### 4. Enhanced FlowDiagramGenerator

**File**: `src/main/java/com/archpilot/service/diagram/FlowDiagramGenerator.java`

**Key Improvements**:
- Now extracts and uses specific exception names in decision nodes
- Replaces generic "Handle error or alternative path" with "Throw ValidationException", "Throw NotFoundException", etc.
- Adds proper stop nodes after exception throws

**New Methods**:
- `extractExceptionFromStep()`: Extracts exception names from step descriptions using regex

## Expected Results

### Before
| Feature | Accuracy | Notes |
|---------|----------|-------|
| Component Discovery | High | Both diagrams correctly identify services |
| Logic Sequence | Low | Incorrect ordering of calls |
| Method Naming | Low | Generic process() calls instead of specific methods |
| Error Handling | Medium | Generic "alternative path" labels |

### After
| Feature | Accuracy | Notes |
|---------|----------|-------|
| Component Discovery | High | Both diagrams correctly identify services |
| Logic Sequence | High | Exact execution order from Sequence Interactions section |
| Method Naming | High | Specific method names (findById, validate, save) |
| Error Handling | High | Specific exception names (ValidationException, NotFoundException) |

## Usage

The improvements are automatic. When users request flow analysis:

```
"Analyze the flow of processOrder method in OrderService class"
```

The system will now:
1. Generate a detailed analysis with explicit sequence interactions and exception handling
2. Parse the structured sections to extract exact ordering and method names
3. Generate sequence diagrams with correct call order and specific method names
4. Generate flow diagrams with specific exception names in decision nodes

## Testing

To verify the improvements:
1. Request a flow analysis for any class/method
2. Check the generated sequence diagram for:
   - Correct ordering of service calls
   - Specific method names (not generic process())
3. Check the generated flow diagram for:
   - Specific exception names in decision nodes
   - Proper error handling paths

## Future Enhancements

- Add support for async/parallel execution flows
- Include return type information in sequence diagrams
- Add timing/performance annotations
- Support for conditional flows based on business rules
