package com.coldchain.platform.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public HttpSessionSecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, @Qualifier("corsConfigurationSource") CorsConfigurationSource cors,
                                                   HttpSessionSecurityContextRepository contexts) throws Exception {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(cookie -> cookie.sameSite("Strict").path("/"));
        http.cors(c -> c.configurationSource(cors))
                .csrf(c -> c.csrfTokenRepository(csrf)
                        // The SPA reads the CSRF cookie and returns that raw token in X-XSRF-TOKEN.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(f -> f.changeSessionId()))
                .securityContext(c -> c.securityContextRepository(contexts).requireExplicitSave(true))
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(401); response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Sign in to continue\"}");
                }).accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(403); response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"You don't have permission\"}");
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/api/auth/login", "/api/auth/csrf").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        .requestMatchers("/api/scenarios/**").hasRole("OPERATIONS")
                        .requestMatchers(HttpMethod.POST, "/api/investigations/*/decision").hasRole("ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/investigations/*/manual-inspection").hasAnyRole("OPERATIONS", "ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/investigations").hasRole("OPERATIONS")
                        .requestMatchers(HttpMethod.PATCH, "/api/investigations/**").hasAnyRole("OPERATIONS", "ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/shipments/**", "/api/lots/**", "/api/sensors/**", "/api/sensor-events/**").hasRole("OPERATIONS")
                        .requestMatchers(HttpMethod.PATCH, "/api/shipments/**", "/api/lots/**").hasRole("OPERATIONS")
                        .requestMatchers(HttpMethod.DELETE, "/api/shipments/**", "/api/lots/**").hasRole("OPERATIONS")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-origin:http://localhost:3000}") String origin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(origin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
