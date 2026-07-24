package com.mbhoni_creative.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final TenantContextFilter tenantContextFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final CustomLoginSuccessHandler customLoginSuccessHandler;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            TenantContextFilter tenantContextFilter,
            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
            CustomLoginSuccessHandler customLoginSuccessHandler
    ) {
        this.userDetailsService = userDetailsService;
        this.tenantContextFilter = tenantContextFilter;
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.customLoginSuccessHandler = customLoginSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .authenticationProvider(authenticationProvider())

            .addFilterBefore(
                    apiKeyAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            )

            .addFilterAfter(
                    tenantContextFilter,
                    UsernamePasswordAuthenticationFilter.class
            )

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                        "/login",
                        "/assets/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/vendor/**",
                        "/api/public/**"
                ).permitAll()

                .requestMatchers(HttpMethod.GET, "/users/reset-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/users/reset-password/token").permitAll()

                .requestMatchers("/super-admin/**")
                        .hasRole("SUPER_ADMIN")

                .requestMatchers("/tenant-admin/**")
                        .hasAnyRole("TENANT_ADMIN", "SUPER_ADMIN")

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                    .loginPage("/login")
                    .successHandler(customLoginSuccessHandler)
                    .permitAll()
            )

            .logout(logout -> logout
                    .logoutSuccessUrl("/login?logout")
                    .permitAll()
            )

            .exceptionHandling(exception -> exception
                    .defaultAuthenticationEntryPointFor(
                            apiAuthenticationEntryPoint(),
                            new AntPathRequestMatcher("/api/integrations/**")
                    )
                    .accessDeniedPage("/access-denied")
            );

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"status":401,"error":"Unauthorized","message":"A valid API key is required"}
                    """);
        };
    }
}
