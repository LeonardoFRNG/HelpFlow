package com.helpflow.ticket.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "HelpFlow Support Ticket API",
                version = "1.0.0",
                description = "Production-Quality IT Support Ticket Management Microservice based on Hexagonal Architecture",
                contact = @Contact(name = "HelpFlow Support Engineering Team", email = "support@helpflow.com")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Provide the JWT token issued by /auth/login or /auth/register"
)
public class OpenApiConfig {
}
