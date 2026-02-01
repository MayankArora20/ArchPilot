package com.archpilot.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Gemini Agent API",
        version = "1.0",
        description = "Simple REST API for asking questions to Google Gemini AI"
    )
)
public class AgentConfig {
    // Configuration will be handled by Spring AI auto-configuration
    // based on application.properties settings
}