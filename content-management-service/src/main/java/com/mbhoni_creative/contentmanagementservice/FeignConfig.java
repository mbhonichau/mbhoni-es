package com.mbhoni_creative.contentmanagementservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;

@Configuration
public class FeignConfig {

    @Bean
    RequestInterceptor requestInterceptor() {
        return template -> {
            template.header("X-API-Key", "mbhoni_key123");
            String auth = "mbhoni_admin:secure_password";
            String encoded = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
            template.header("Authorization", "Basic " + encoded);
        };
    }
}