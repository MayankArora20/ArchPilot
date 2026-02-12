package com.archpilot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.archpilot.service.agent.GeminiAgentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
@Tag(name = "Gemini Agent", description = "AI-powered question answering using Google Gemini")
public class AgentController {

    private final GeminiAgentService geminiAgentService;

    public AgentController(GeminiAgentService geminiAgentService) {
        this.geminiAgentService = geminiAgentService;
    }

    @PostMapping("/ask")
    @Operation(summary = "Ask a question to Gemini AI", description = "Submit a question and get an AI-generated answer")
    public ResponseEntity<String> askQuestion(@RequestBody String question) {
        String answer = geminiAgentService.askQuestion(question);
        return ResponseEntity.ok(answer);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the agent service is running")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Agent is running");
    }
}