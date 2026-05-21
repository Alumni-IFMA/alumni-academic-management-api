package com.alumni.academic_management_api.config;

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

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/campus-courses").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/auth/users/*/role").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/auth/users/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auth/users/*/profile-picture").authenticated()
                        .requestMatchers(HttpMethod.GET, "/auth/users/**", "/campus-courses").permitAll()
                        .requestMatchers(HttpMethod.GET, "/jobs", "/jobs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/jobs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/jobs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/jobs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/news", "/news/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/news").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/news/*/cover-image").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/news/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/news/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}
