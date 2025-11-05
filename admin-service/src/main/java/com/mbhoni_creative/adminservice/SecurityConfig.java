package com.mbhoni_creative.adminservice;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.config.Customizer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/status").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .addFilterAfter(apiKeyFilter(), BasicAuthenticationFilter.class); // ← AFTER BASIC
        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            User jpaUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            return org.springframework.security.core.userdetails.User.withUsername(jpaUser.getUsername())
                .password(jpaUser.getPassword())
                .roles(jpaUser.getRoles().split(","))
                .build();
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    OncePerRequestFilter apiKeyFilter() {
        return new OncePerRequestFilter() {
            private final List<String> validKeys = List.of("mbhoni_key123");

            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {

                String path = request.getRequestURI();

                // ALLOW PUBLIC ENDPOINT WITHOUT ANY CHECK
                if ("/api/admin/status".equals(path)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // ALL OTHER ENDPOINTS REQUIRE API KEY
                String key = request.getHeader("X-API-Key");
                if (key != null && validKeys.contains(key)) {
                    filterChain.doFilter(request, response);
                    } else {
                    response.setStatus(401);
                    response.getWriter().write("{\"error\":\"Invalid API Key\"}");
                }
            }
        };
    }
}