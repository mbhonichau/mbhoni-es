package com.mbhoni_creative.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
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
    private final CustomOAuth2UserService customOAuth2UserService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            TenantContextFilter tenantContextFilter,
            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
            CustomLoginSuccessHandler customLoginSuccessHandler,
            CustomOAuth2UserService customOAuth2UserService,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider
    ) {
        this.userDetailsService = userDetailsService;
        this.tenantContextFilter = tenantContextFilter;
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.customLoginSuccessHandler = customLoginSuccessHandler;
        this.customOAuth2UserService = customOAuth2UserService;
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
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
                        "/oauth2/**",
                        "/assets/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/vendor/**",
                        "/api/public/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                ).permitAll()

                .requestMatchers(HttpMethod.GET, "/users/reset-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/users/reset-password/token").permitAll()
                .requestMatchers(HttpMethod.POST, "/forgot-password", "/users/forgot-password").permitAll()

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
            );

        ClientRegistrationRepository clientRegistrationRepository = clientRegistrationRepositoryProvider.getIfAvailable();
        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .successHandler(customLoginSuccessHandler)
            );
        }

        http
            .logout(logout -> logout
                    .logoutSuccessUrl("/login?logout")
                    .permitAll()
            )

            .exceptionHandling(exception -> exception
                    .defaultAuthenticationEntryPointFor(
                            apiAuthenticationEntryPoint(),
                            new AntPathRequestMatcher("/api/**")
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
                    {"status":401,"error":"Unauthorized","message":"A valid API key or authentication is required"}
                    """);
        };
    }
}
