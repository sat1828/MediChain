package com.medichain.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/login", "/").permitAll()
                .requestMatchers("/css/**", "/js/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/wards/*/inventory").authenticated()
                .requestMatchers("/api/v1/transactions/dispense").hasAnyRole("WARD_PHARMACIST", "PHARMACY_MANAGER")
                .requestMatchers("/api/v1/transactions/receipt").hasAnyRole("WARD_PHARMACIST", "PHARMACY_MANAGER")
                .requestMatchers("/api/v1/alerts/**").hasAnyRole("PHARMACY_MANAGER", "WARD_PHARMACIST", "ADMIN")
                .requestMatchers("/api/v1/forecast/**").hasAnyRole("PHARMACY_MANAGER", "PROCUREMENT_OFFICER")
                .requestMatchers("/api/v1/procurement/**").hasAnyRole("PHARMACY_MANAGER", "PROCUREMENT_OFFICER")
                .requestMatchers("/api/v1/ngo/**").hasAnyRole("PHARMACY_MANAGER", "NGO_PARTNER")
                .requestMatchers("/api/v1/dashboard/**").hasAnyRole("PHARMACY_MANAGER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
