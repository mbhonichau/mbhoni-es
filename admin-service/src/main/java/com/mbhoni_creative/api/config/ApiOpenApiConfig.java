package com.mbhoni_creative.api.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@PropertySource("classpath:api-openapi.properties")
public class ApiOpenApiConfig {
    @Bean public OpenAPI adminApiDescription() {
        return new OpenAPI().info(new Info().title("MBHONI Admin API").version("v1")
                .description("Tenant-scoped read API. Send X-API-Key. Scope grants are mapped from existing key permissions."))
                .components(new Components().addSecuritySchemes("ApiKey", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-API-Key")));
    }
    @Bean public SmartInitializingSingleton restrictSpringdocRoutes(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings) {
        return () -> {
            // springdoc also registers a YAML companion. Keep only the explicitly approved JSON route.
            var unwanted = mappings.getHandlerMethods().entrySet().stream()
                    .filter(e -> e.getValue().getBeanType().getName().startsWith("org.springdoc."))
                    .filter(e -> !e.getKey().getPatternValues().equals(java.util.Set.of("/api/openapi.json")))
                    .map(java.util.Map.Entry::getKey).toList();
            unwanted.forEach(mappings::unregisterMapping);
        };
    }
}
