package com.archpilot.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for code flow analysis response
 */
public class CodeFlowAnalysisResponse {
    private String flowDescription;
    private String targetClass;
    private String targetMethod;
    private List<FlowStep> flowSteps;
    private List<String> involvedClasses;
    private List<String> designPatternsUsed;
    private String complexity;
    private List<String> potentialImprovements;
    private LocalDateTime analyzedAt;

    public CodeFlowAnalysisResponse() {
        this.analyzedAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getFlowDescription() { return flowDescription; }
    public void setFlowDescription(String flowDescription) { this.flowDescription = flowDescription; }

    public String getTargetClass() { return targetClass; }
    public void setTargetClass(String targetClass) { this.targetClass = targetClass; }

    public String getTargetMethod() { return targetMethod; }
    public void setTargetMethod(String targetMethod) { this.targetMethod = targetMethod; }

    public List<FlowStep> getFlowSteps() { return flowSteps; }
    public void setFlowSteps(List<FlowStep> flowSteps) { this.flowSteps = flowSteps; }

    public List<String> getInvolvedClasses() { return involvedClasses; }
    public void setInvolvedClasses(List<String> involvedClasses) { this.involvedClasses = involvedClasses; }

    public List<String> getDesignPatternsUsed() { return designPatternsUsed; }
    public void setDesignPatternsUsed(List<String> designPatternsUsed) { this.designPatternsUsed = designPatternsUsed; }

    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }

    public List<String> getPotentialImprovements() { return potentialImprovements; }
    public void setPotentialImprovements(List<String> potentialImprovements) { this.potentialImprovements = potentialImprovements; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    /**
     * Inner class representing a step in the code flow
     */
    public static class FlowStep {
        private int stepNumber;
        private String className;
        private String methodName;
        private String description;
        private String inputParameters;
        private String outputResult;

        public FlowStep() {}

        public FlowStep(int stepNumber, String className, String methodName, String description) {
            this.stepNumber = stepNumber;
            this.className = className;
            this.methodName = methodName;
            this.description = description;
        }

        // Getters and setters
        public int getStepNumber() { return stepNumber; }
        public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }

        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getInputParameters() { return inputParameters; }
        public void setInputParameters(String inputParameters) { this.inputParameters = inputParameters; }

        public String getOutputResult() { return outputResult; }
        public void setOutputResult(String outputResult) { this.outputResult = outputResult; }
    }
}