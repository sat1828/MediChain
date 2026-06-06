package com.medichain.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI medichainOpenAPI() {
        var securityScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Enter JWT token from /api/v1/auth/login");

        return new OpenAPI()
            .info(new Info()
                .title("MediChain API")
                .description("Hospital Pharmacy & Drug Inventory Management System\n\n" +
                    "End-to-end batch-aware, ward-level drug inventory with AI-powered forecasting, " +
                    "FEFO dispense logic, automated expiry alerts, procurement PDF generation, " +
                    "and NGO redistribution workflows.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("MediChain Team")
                    .email("support@medichain.in"))
                .license(new License()
                    .name("Proprietary - Hospital Use License")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", securityScheme));
    }
}
