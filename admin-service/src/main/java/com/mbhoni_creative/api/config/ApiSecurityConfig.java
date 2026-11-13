package com.mbhoni_creative.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbhoni_creative.api.error.ApiProblems;
import com.mbhoni_creative.api.security.*;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
public class ApiSecurityConfig {
    @Bean @Order(0)
    public SecurityFilterChain versionedApiChain(HttpSecurity http, ApiCredentialService credentials, ObjectMapper mapper) throws Exception {
        return http.securityMatcher("/api/v1/**", "/api/docs/**", "/api/openapi.json")
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(s -> s.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(s -> s.requestCache(new NullRequestCache()))
                .csrf(s -> s.disable()).formLogin(s -> s.disable()).httpBasic(s -> s.disable())
                .logout(s -> s.disable()).oauth2Login(s -> s.disable())
                .addFilterBefore(new ApiKeyFilter(credentials, mapper), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(s -> s.requestMatchers("/api/docs", "/api/docs/").permitAll()
                        .anyRequest().hasAuthority("API_V1"))
                .exceptionHandling(s -> s.authenticationEntryPoint((req, res, ex) ->
                        ApiProblems.write(mapper, res, 401, "UNAUTHENTICATED", "A valid API key is required"))
                        .accessDeniedHandler((req, res, ex) -> ApiProblems.write(mapper, res, 403, "FORBIDDEN", "Required API scope is missing")))
                .build();
    }
}
