package com.mbhoni_creative.contentmanagementservice;

import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

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
                .requestMatchers("/api/content/**").permitAll()     // PUBLIC
                .requestMatchers("/api/loadbalanced-metrics/**").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .addFilterAfter(apiKeyFilter(), BasicAuthenticationFilter.class); // AFTER
        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            User jpaUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            return org.springframework.security.core.userdetails.User
                .withUsername(jpaUser.getUsername())
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

                // SKIP API KEY FOR ALL /api/content/**
                if (path.startsWith("/api/content/")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // REQUIRE API KEY FOR loadbalanced-metrics
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