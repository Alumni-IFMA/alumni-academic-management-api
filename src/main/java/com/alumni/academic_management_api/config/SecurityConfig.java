package com.alumni.academic_management_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, denied) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value()))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/refresh", "/auth/logout").permitAll()
                        .requestMatchers("/dev/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/campus-courses").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/auth/users/*/role").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/auth/users/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/auth/users/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auth/users/*/profile-picture").authenticated()
                        .requestMatchers(HttpMethod.POST,
                                "/auth/forgot-password",
                                "/auth/forgot-password/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/auth/reset-password",
                                "/auth/reset-password/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/jobs", "/jobs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/jobs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/jobs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/jobs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/news", "/news/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/news").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/news/*/cover-image").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/news/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/news/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/degrees").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/degrees/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/degrees/*/download").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}