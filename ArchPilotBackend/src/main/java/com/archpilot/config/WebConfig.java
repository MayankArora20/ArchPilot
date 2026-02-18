package com.archpilot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.archpilot.interceptor.TokenGuardrailInterceptor;

/**
 * Web configuration to register the TokenGuardrailInterceptor
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TokenGuardrailInterceptor tokenGuardrailInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenGuardrailInterceptor)
                .addPathPatterns("/api/agent/**", "/api/chat/**", "/api/uml/**")
                .excludePathPatterns("/api/agent/health"); // Exclude health check from rate limiting
    }
}