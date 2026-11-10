package com.coldchain.platform.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public / Monitoring / Auth / Scenario Runner
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/scenarios/**").permitAll()
                        // Public reads for demonstration
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        // Decision requires ANALYST or ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/investigations/*/decision").hasAnyRole("ANALYST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/investigations/*/manual-inspection").hasAnyRole("ANALYST", "ADMIN")
                        // Ingestion and registration require OPERATIONS or ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/shipments/**").hasAnyRole("OPERATIONS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/lots/**").hasAnyRole("OPERATIONS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/sensors/**").hasAnyRole("OPERATIONS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/sensor-events/**").hasAnyRole("OPERATIONS", "ADMIN")
                        // Fallback
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
